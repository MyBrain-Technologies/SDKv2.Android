package core.bluetooth.lowenergy;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import core.bluetooth.BtState;
import core.bluetooth.IStreamable;
import core.bluetooth.MbtBluetooth;
import core.recordingsession.metadata.DeviceInfo;
import utils.AsyncUtils;
import utils.MbtLock;

/**
 * Created by Etienne on 08/02/2018.
 *
 */

public final class MbtBluetoothLE extends MbtBluetooth implements IStreamable {
    private static final String TAG = MbtBluetoothLE.class.getSimpleName();


    private MbtGattController mbtGattController;

    private BluetoothLeScanner bluetoothLeScanner;

    private BluetoothGatt gatt;

    public MbtBluetoothLE(Context context){
        super(context);
        this.mbtGattController = new MbtGattController(context,this);
    }

    /**
     * This method sends a request to the headset to <strong><code>START</code></strong>
     * the EEG raw data acquisition process and
     * enables Bluetooth Low Energy notification to receive the raw data.
     * <p><strong>Note:</strong> calling this method will start the raw EEG data acquisition process
     * on the headset which will <strong>consume battery life</strong>. Please consider calling
     * {@link #stopStream()} when EEG raw data are no longer needed.</p>
     * @return              <code>true</code> if request has been sent correctly
     *                      <code>false</code> on immediate error
     */
    @Override
    public boolean startStream() {
        if(!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_BRAIN_ACTIVITY))
            return false;

        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
               enableOrDisableNotificationsOnCharacteristic(true, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_BRAIN_ACTIVITY));
            }
        });

        return true;
    }

    /**
     * This method sends a request to the headset to <strong><code>STOP</code></strong>
     * the EEG raw data acquisition process, therefore disabling the Bluetooth Low Energy notification
     * and cleaning reference to previously registered listener.
     * <p>Calling this method will <strong>preserve battery life</strong> by halting the raw EEG
     * data acquisition process on the headset.</p>
     * @return true upon correct EEG disability request, false on immediate error
     */
    @Override
    public boolean stopStream() {
        if(!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_BRAIN_ACTIVITY))
            return false;

        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                enableOrDisableNotificationsOnCharacteristic(false, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_BRAIN_ACTIVITY));
            }
        });
        return true;
    }


    /**
     * Enable or disable notifications on specific characteristic provinding this characteristic is "notification ready".
     * @param enableNotification enabling if set to true, false otherwise
     * @param characteristic the characteristic to enable or disable notification on
     * @return false for any error, true upon success
     */
    private boolean enableOrDisableNotificationsOnCharacteristic(boolean enableNotification, BluetoothGattCharacteristic characteristic) {
        //Check that no other notification operation is in progress
        if(this.mbtGattController.notificationLock.isWaiting())
            return false;

        this.mbtGattController.notificationLock = new MbtLock<>();

        if (characteristic == null)
            throw new IllegalStateException("Error: impossible to enable notification for EEG " +
                    "because the Gatt Controller is not correctly initialized");

        Log.i(TAG, "Request received to enable notification for EEG.");

        Log.i(TAG, "Now enabling local notification for EEG...");
        if (!this.gatt.setCharacteristicNotification(characteristic, enableNotification)) {
            Log.e(TAG, "Failed to enable local notification for EEG!");
            return false;
        }

        final BluetoothGattDescriptor notificationDescriptor =
                characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID);
        if (notificationDescriptor == null) {
            Log.e(TAG, String.format("Error: measurement characteristic with " +
                            "UUID <%s> does not have a descriptor (UUID <%s>) to enable notification remotely!",
                    characteristic.getUuid().toString(), MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID.toString()));
            return false;
        }

        Log.i(TAG, "Now enabling remote notification for EEG...");
        if (!notificationDescriptor.setValue(enableNotification ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
            final StringBuilder sb = new StringBuilder();
            for (final byte value : enableNotification ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) {
                sb.append(value);
                sb.append(';');
            }
            Log.e(TAG, String.format("Error: measurement characteristic's notification descriptor with " +
                            "UUID <%s> could not store the ENABLE notification value <%s>.",
                    MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID.toString(), sb.toString()));
            return false;
        }

        if (!this.gatt.writeDescriptor(notificationDescriptor)) {
            Log.e(TAG, "Error: failed to initiate write descriptor operation in order to remotely " +
                    "enable notification for EEG!");
            return false;
        }

        Log.i(TAG, "Successfully initiated write descriptor operation in order to remotely " +
                "enable notification for EEG: now waiting for confirmation from headset.");

        final Boolean result = this.mbtGattController.notificationLock.waitAndGetResult(1000);
        if (result == null)
            Log.e(TAG, "Error: waiting for confirmation from headset to have notification " +
                    "ENABLED for EEG has expired or the write descriptor operation has failed");
        else if (result) {
            Log.i(TAG, "Notification for EEG are now finally enabled. " +
                    "EEG acquisition should start right away.");
            return true;
        }
        return false;
    }


    /**
     * Start bluetooth low energy scanner in order to find BLE device that matches the specific filters.
     * <p><strong>Note:</strong> This method will consume your mobile/tablet battery. Please consider calling
     * {@link #stopLowEnergyScan()} when scanning is no longer needed.</p>
     * @param filterOnDeviceService
     * @param filterOnDeviceName
     * @return Each found device that matches the specified filters
     */
    public BluetoothDevice startLowEnergyScan(boolean filterOnDeviceService, boolean filterOnDeviceName) {
        this.bluetoothLeScanner = super.bluetoothAdapter.getBluetoothLeScanner();
        List<ScanFilter> mFilters = new ArrayList<>();
        if(filterOnDeviceService){
            Log.i(TAG, "ENABLED SERVICE FILTER");
            final ScanFilter filterService = new ScanFilter.Builder()
                    //.setDeviceName(deviceName)
                    //.setDeviceName("melo_")
                    .setServiceUuid(new ParcelUuid(MelomindCharacteristics.SERVICE_MEASUREMENT))
                    .build();
            mFilters.add(filterService);
        }

        if(filterOnDeviceName){
//            Log.i(TAG, "ENABLED NAME FILTER");
//            final ScanFilter filterName = new ScanFilter.Builder()
//                    .setDeviceName(deviceName)
//                    .build();
//
//            mFilters.add(filterName);
        }

        final ScanSettings settings = new ScanSettings.Builder()
                .setReportDelay(0)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        //Log.i(TAG, String.format("Starting Low Energy Scan with filtering on name '%s' and service UUID '%s'", deviceName, MelomindCharacteristics.SERVICE_MEASUREMENT));
        this.bluetoothLeScanner.startScan(mFilters, settings, this.leScanCallback);
        Log.i(TAG, "in scan method.");
        return super.scanLock.waitAndGetResult(20000);
    }


    /**
     * Stops the currently bluetooth low energy scanner.
     */
    public void stopLowEnergyScan() {
        if(this.bluetoothLeScanner != null)
            this.bluetoothLeScanner.stopScan(this.leScanCallback);
        MbtBluetoothLE.super.scannedDevices = new ArrayList<>();
    }


    /**
     * callback used when scanning using bluetooth Low Energy scanner.
     */
    private ScanCallback leScanCallback = new ScanCallback() {

        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            final BluetoothDevice device = result.getDevice();
            Log.i(TAG, String.format("Stopping Low Energy Scan -> device detected " +
                    "with name '%s' and MAC address '%s' ", device.getName(), device.getAddress()));
            //TODO, check if already in the array list
            MbtBluetoothLE.super.scannedDevices.add(result.getDevice());
            MbtBluetoothLE.super.scanLock.setResultAndNotify(result.getDevice());
        }

        public final void onScanFailed(final int errorCode) {
            super.onScanFailed(errorCode);
            String msg = "Could not start scan. Reason -> ";
            switch(errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    msg += "Scan already started!";
                    notifyStateChanged(BtState.SCAN_FAILED_ALREADY_STARTED);
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    msg += "App could not be registered";
                    notifyStateChanged(BtState.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED);
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    msg += "Failed to start power optimized scan. Feature is not supported by device";
                    notifyStateChanged(BtState.SCAN_FAILED_FEATURE_UNSUPPORTED);
                    break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    msg += "Internal Error. No more details.";
                    notifyStateChanged(BtState.INTERNAL_FAILURE);
                    break;
            }
            Log.e(TAG, msg);
        }
    };

    @Override
    public boolean connect(Context context, BluetoothDevice device) {

        //Using reflexion here because min API is 21 and transport layer is not available publicly until API 23
        try {
            final Method connectGattMethod = device.getClass()
                    .getMethod("connectGatt",
                            Context.class, boolean.class, BluetoothGattCallback.class, int.class);

            final int transport = device.getClass().getDeclaredField("TRANSPORT_LE").getInt(null);
            this.gatt = (BluetoothGatt) connectGattMethod.invoke(device, context, false, mbtGattController, transport);
            Log.i(TAG, "this.gatt = " + this.gatt.toString());
            return true; //TODO test
//            final BtState state = super.connectionLock.waitAndGetResult(20000);
//            return state != null && state == BtState.CONNECTED_AND_READY;

        } catch (final NoSuchMethodException | NoSuchFieldException | IllegalAccessException
                | InvocationTargetException e) {
            final String errorMsg = " -> " + e.getMessage();
            if (e instanceof NoSuchMethodException)
                Log.e(TAG, "Failed to find connectGatt method via reflexion" + errorMsg);
            else if (e instanceof NoSuchFieldException)
                Log.e(TAG, "Failed to find Transport LE field via reflexion" + errorMsg);
            else if (e instanceof IllegalAccessException)
                Log.e(TAG, "Failed to access Transport LE field via reflexion" + errorMsg);
            else if (e instanceof InvocationTargetException)
                Log.e(TAG, "Failed to invoke connectGatt method via reflexion" + errorMsg);
            else
                Log.e(TAG, "Unable to connect LE with reflexion. Reason" + errorMsg);
            Log.getStackTraceString(e);
        }
        return false;
    }

    @Override
    public boolean disconnect(BluetoothDevice device) {
        return false;
    }


    /**
     * Starts a read operation on a specific characteristic
     * @param characteristic the characteristic to read
     * @return immediatly false on error, true true if read operation has started correctly
     */
    public boolean startReadOperation(UUID characteristic){
        if(!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, characteristic))
            return false;

        if (!this.gatt.readCharacteristic(gatt.getService(MelomindCharacteristics.SERVICE_DEVICE_INFOS).getCharacteristic(MelomindCharacteristics.CHARAC_INFO_FIRMWARE_VERSION))) {
            Log.e(TAG, "Error: failed to initiate read characteristic operation in order " +
                    "to retrieve the current fwVersion value from remote device");
            return false;
        }

        Log.i(TAG, "Successfully initiated read characteristic operation in order " +
                "to retrieve the current fwVersion value from remote device");
        //TODO check how to implement timeout on read
//        final String fwVersion = this.readDeviceInfoLock.waitAndGetResult(2000);
//        if (fwVersion == null) {
//            Log.e(TAG, "Error: failed to fetch fwVersion value within allotted time of 1 second " +
//                    "or fetched value was invalid !");
//            return false;
//        }
//
//        Log.i(TAG, "Successfully retrieved fwVersion value from remote device within allocated");
//        Log.i(TAG, "fwVersion = " + fwVersion);

        return true;
    }

    /**
     * Starts a write operation on a specific characteristic
     * @param characteristic the characteristic to perform write operation on
     * @param payload the payload to write to the characteristic
     * @return immediatly false on error, true otherwise
     */
    public boolean startWriteOperation(UUID characteristic, byte[] payload){
        if(!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, characteristic))
            return false;

        //Send buffer
        gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(characteristic).setValue(payload);
        if (!this.gatt.writeCharacteristic(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(characteristic))) {
            Log.e(TAG, "Error: failed to send filter values update");
            return false;
        }
        return true;
    }


    /**
     * Checks whether the service and characteristic about to be used to communicate with the remote device.
     * @param service the service to check
     * @param characteristic the characteristic to check
     * @return false if something not valid, true otherwise
     */
    private boolean checkServiceAndCharacteristicValidity(@NonNull UUID service, @NonNull UUID characteristic){
        if(gatt == null)
            return false;

        if(gatt.getService(service) == null)
            return false;

        if(gatt.getService(service).getCharacteristic(characteristic) == null)
            return false;

        return true;
    }

    public synchronized Byte[] getEEGConfiguration() {
        if(!this.mbtGattController.enableNotificationOnMailbox())
            return null;
        //first read the eeg config to update acquisition buffers
        return this.mbtGattController.getEegConfig();
    }
    /**
     * Initiates a read battery operation on this correct BtProtocol
     */
    public void readBattery(){
       this.mbtGattController.readBattery();
    }

    /**
     * Initiates a read firmware version operation on this correct BtProtocol
     */
    public void readFwVersion(){
        this.mbtGattController.readFwVersion();
    }

    /**
     * Initiates a read hardware version operation on this correct BtProtocol
     */
    public void readHwVersion(){
        this.mbtGattController.readHwVersion();
    }

    /**
     * Initiates a read serial number operation on this correct BtProtocol
     */
    public void readSerialNumber(){
    this.mbtGattController.readSerialNumber();
    }
}
