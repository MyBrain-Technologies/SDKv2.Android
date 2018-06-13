package core.bluetooth.lowenergy;


import android.Manifest;
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
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import config.AmpGainConfig;
import core.bluetooth.BtProtocol;
import core.bluetooth.BtState;
import core.bluetooth.IStreamable;
import core.bluetooth.MbtBluetooth;
import core.bluetooth.MbtBluetoothManager;
import config.FilterConfig;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 *
 * This class contains all required methods to interact with a LE bluetooth peripheral, such as Melomind.
 *
 * <p>In order to work {@link android.Manifest.permission#BLUETOOTH} and {@link android.Manifest.permission#BLUETOOTH_ADMIN} permissions
 * are required </p>
 *
 * Created by Etienne on 08/02/2018.
 *
 */

public final class MbtBluetoothLE extends MbtBluetooth implements IStreamable {
    private static final String TAG = MbtBluetoothLE.class.getSimpleName();

    private StreamState streamingState = StreamState.IDLE;

    private MbtGattController mbtGattController;

    private BluetoothLeScanner bluetoothLeScanner;

    private BluetoothGatt gatt;

    /**
     * public constructor that will instanciate this class. It also instanciate a new
     * {@link MbtGattController MbtGattController} instance
     * @param context the application context
     * @param mbtBluetoothManager the Bluetooth manager that performs requests and receives results.
     */
    public MbtBluetoothLE(Context context, MbtBluetoothManager mbtBluetoothManager) {
        super(context, mbtBluetoothManager);
        this.mbtGattController = new MbtGattController(context, this);
    }

    /**
     * This method sends a request to the headset to <strong><code>START</code></strong>
     * the EEG raw data acquisition process and
     * enables Bluetooth Low Energy notification to receive the raw data.
     * <p><strong>Note:</strong> calling this method will start the raw EEG data acquisition process
     * on the headset which will <strong>consume battery life</strong>. Please consider calling
     * {@link #stopStream()} when EEG raw data are no longer needed.</p>
     *
     * If there is already a streaming session in progress, nothing happens and true is returned.
     *
     * @return              <code>true</code> if request has been sent correctly
     *                      <code>false</code> on immediate error
     */
    @Override
    public synchronized boolean startStream() {
        if(isStreaming())
            return true;

        if (!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_EEG))
            return false;

        enableOrDisableNotificationsOnCharacteristic(true, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_HEADSET_STATUS));

        //Adding small sleep to "free" bluetooth
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return enableOrDisableNotificationsOnCharacteristic(true, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG));
    }

    /**
     * This method sends a request to the headset to <strong><code>STOP</code></strong>
     * the EEG raw data acquisition process, therefore disabling the Bluetooth Low Energy notification
     * and cleaning reference to previously registered listener.
     * <p>Calling this method will <strong>preserve battery life</strong> by halting the raw EEG
     * data acquisition process on the headset.</p>
     *
     * If there is no streaming session in progress, nothing happens and true is returned.
     *
     * @return true upon correct EEG disability request, false on immediate error
     */
    @Override
    public boolean stopStream() {
        if(!isStreaming())
            return true;

        if (!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_EEG))
            return false;

        return enableOrDisableNotificationsOnCharacteristic(false, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG));
    }

    /**
     * Whenever there is a new stream state, this method is called to notify the bluetooth manager about it.
     * @param newStreamState the new stream state based on {@link core.bluetooth.IStreamable.StreamState the StreamState enum}
     */
    @Override
    public void notifyStreamStateChanged(StreamState newStreamState) {
        Log.i(TAG, "new streamstate with state " + newStreamState.toString());

        streamingState = newStreamState;
        super.mbtBluetoothManager.notifyStreamStateChanged(newStreamState);
    }


    /**
     * Whenever there is a new headset status received, this method is called to notify the bluetooth manager about it.
     * @param payload the new headset status as a raw byte array. This byte array has to be parsed afterward.
     */
    public void notifyNewHeadsetStatus(byte[] payload){
        this.mbtBluetoothManager.notifyNewHeadsetStatus(BtProtocol.BLUETOOTH_LE, payload);
    }

    /**
     *
     * @return true if a streaming session is in progress, false otherwise
     */
    @Override
    public boolean isStreaming() {
        return streamingState == StreamState.STARTED;
    }


    /**
     * Enable or disable notifications on specific characteristic provinding this characteristic is "notification ready".
     * @param enableNotification enabling if set to true, false otherwise
     * @param characteristic the characteristic to enable or disable notification on.
     *
     * This operation is synchronous, meaning the thread running this method is blocked until the operation completes.
     *
     * @return false for any error, true upon success
     */
    private synchronized boolean enableOrDisableNotificationsOnCharacteristic(boolean enableNotification, @NonNull BluetoothGattCharacteristic characteristic) {
        if(!isConnected())
            return false;

        Log.i(TAG, "Now enabling local notification for characteristic: " + characteristic.getUuid());
        if (!this.gatt.setCharacteristicNotification(characteristic, enableNotification)) {
            Log.e(TAG, "Failed to enable local notification for characteristic: " + characteristic.getUuid());
            return false;
        }

        final BluetoothGattDescriptor notificationDescriptor =
                characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID);
        if (notificationDescriptor == null) {
            Log.e(TAG, String.format("Error: characteristic with " +
                            "UUID <%s> does not have a descriptor (UUID <%s>) to enable notification remotely!",
                    characteristic.getUuid().toString(), MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID.toString()));
            return false;
        }

        Log.i(TAG, "Now enabling remote notification for characteristic: " + characteristic.getUuid());
        if (!notificationDescriptor.setValue(enableNotification ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
            final StringBuilder sb = new StringBuilder();
            for (final byte value : enableNotification ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) {
                sb.append(value);
                sb.append(';');
            }
            Log.e(TAG, String.format("Error: characteristic's notification descriptor with " +
                            "UUID <%s> could not store the ENABLE notification value <%s>.",
                    MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID.toString(), sb.toString()));
            return false;
        }

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!this.gatt.writeDescriptor(notificationDescriptor)) {
            Log.e(TAG, "Error: failed to initiate write descriptor operation in order to remotely " +
                    "enable notification for characteristic: " + characteristic.getUuid());
            return false;
        }

        Log.i(TAG, "Successfully initiated write descriptor operation in order to remotely " +
                "enable notification... now waiting for confirmation from headset.");

        try {
            this.wait(5000);
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Start bluetooth low energy scanner in order to find BLE device that matches the specific filters.
     * <p><strong>Note:</strong> This method will consume your mobile/tablet battery. Please consider calling
     * {@link #stopLowEnergyScan()} when scanning is no longer needed.</p>
     * @param filterOnDeviceService
     * @return Each found device that matches the specified filters
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public BluetoothDevice startLowEnergyScan(boolean filterOnDeviceService, String deviceName) {
        this.bluetoothLeScanner = super.bluetoothAdapter.getBluetoothLeScanner();
        List<ScanFilter> mFilters = new ArrayList<>();
        if (filterOnDeviceService) {
            Log.i(TAG, "ENABLED SERVICE FILTER");
            final ScanFilter.Builder filterService = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(MelomindCharacteristics.SERVICE_MEASUREMENT));

            if(deviceName != null)
                filterService.setDeviceName(deviceName);

            mFilters.add(filterService.build());
        }

//        if (deviceName != null) {
//            Log.i(TAG, "ENABLED NAME FILTER");
//            final ScanFilter filterName = new ScanFilter.Builder()
//                    .setDeviceName(deviceName)
//                    .build();
//
//            mFilters.add(filterName);
//        }

        final ScanSettings settings = new ScanSettings.Builder()
                .setReportDelay(0)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        //Log.i(TAG, String.format("Starting Low Energy Scan with filtering on name '%s' and service UUID '%s'", deviceName, MelomindCharacteristics.SERVICE_MEASUREMENT));
        this.bluetoothLeScanner.startScan(mFilters, settings, this.leScanCallback);
        Log.i(TAG, "in scan method.");
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        notifyConnectionStateChanged(BtState.SCAN_STARTED);
        return super.scanLock.waitAndGetResult();
    }


    /**
     * Stops the currently bluetooth low energy scanner.
     * If a lock is currently waiting, the lock is disabled.
     */
    public void stopLowEnergyScan() {

        if(super.scanLock != null && super.scanLock.isWaiting()){
            super.scanLock.setResultAndNotify(null);
        }

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
            MbtBluetoothLE.super.scannedDevices.add(device);
            MbtBluetoothLE.super.scanLock.setResultAndNotify(device);
        }

        public final void onScanFailed(final int errorCode) {
            super.onScanFailed(errorCode);
            String msg = "Could not start scan. Reason -> ";
            switch(errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    msg += "Scan already started!";
                    notifyConnectionStateChanged(BtState.SCAN_FAILED_ALREADY_STARTED);
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    msg += "App could not be registered";
                    notifyConnectionStateChanged(BtState.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED);
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    msg += "Failed to start power optimized scan. Feature is not supported by device";
                    notifyConnectionStateChanged(BtState.SCAN_FAILED_FEATURE_UNSUPPORTED);
                    break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    msg += "Internal Error. No more details.";
                    notifyConnectionStateChanged(BtState.INTERNAL_FAILURE);
                    break;
            }
            Log.e(TAG, msg);
        }
    };

    /**
     * Starts the connect operation in order to connect the {@link BluetoothDevice bluetooth device} (peripheral)
     * to the terminal (central).
     * If the operation starts successfully, a new {@link BluetoothGatt gatt} instance will be stored.
     * @param context the context which the connection event takes place in.
     * @param device the bluetooth device to connect to.
     * @return true if operation has correctly started, false otherwise.
     */
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

    /**
     * Disconnects from the currently connected {@link BluetoothGatt gatt instance} and sets it to null
     * @return
     */
    @Override
    public boolean disconnect() {
        this.gatt.disconnect();
        this.gatt = null;
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected() {
        return getCurrentState() == BtState.CONNECTED_AND_READY;
    }


    /**
     * Starts a read operation on a specific characteristic
     * @param characteristic the characteristic to read
     * @return immediatly false on error, true true if read operation has started correctly
     */
    private boolean startReadOperation(UUID characteristic){
        if(!isConnected())
            return false;

        UUID service;

        if (characteristic.equals(MelomindCharacteristics.CHARAC_INFO_FIRMWARE_VERSION)
                || characteristic.equals(MelomindCharacteristics.CHARAC_INFO_HARDWARE_VERSION)
                || characteristic.equals(MelomindCharacteristics.CHARAC_INFO_SERIAL_NUMBER)) {
            service = MelomindCharacteristics.SERVICE_DEVICE_INFOS;
        }else{
            service = MelomindCharacteristics.SERVICE_MEASUREMENT;
        }

        if(!checkServiceAndCharacteristicValidity(service, characteristic))
            return false;

        if (!this.gatt.readCharacteristic(gatt.getService(service).getCharacteristic(characteristic))) {
            Log.e(TAG, "Error: failed to initiate read characteristic operation");
            return false;
        }

        Log.i(TAG, "Successfully initiated read characteristic operation");

        return true;
    }

    /**
     * Starts a write operation on a specific characteristic
     * @param characteristic the characteristic to perform write operation on
     * @param payload the payload to write to the characteristic
     * @return immediatly false on error, true otherwise
     */
    private synchronized boolean startWriteOperation(UUID characteristic, byte[] payload){
        if(!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, characteristic))
            return false;

        //Send buffer
        this.gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(characteristic).setValue(payload);
        if (!this.gatt.writeCharacteristic(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(characteristic))) {
            Log.e(TAG, "Error: failed to write characteristic " + characteristic.toString());
            return false;
        }

        try {
            this.wait(5000);
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
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

    /**
     * Checks if the charateristic has notifications already enabled or not.
     * @param service the Service UUID that holds the characteristic
     * @param characteristic the characteristic UUID.
     * @return true is already enabled notifications, false otherwise.
     */
    private boolean isNotificationEnabledOnCharacteristic(@NonNull UUID service, @NonNull UUID characteristic){
        if(!checkServiceAndCharacteristicValidity(service, characteristic))
            return false;

        if(this.gatt.getService(service).getCharacteristic(characteristic).getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID) == null)
            return false;

        return Arrays.equals(this.gatt.getService(service).getCharacteristic(characteristic).getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID).getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

    }


    /**
     * Initiates a read battery operation on this correct BtProtocol
     */
    public boolean readBattery() {
        Log.i(TAG, "read battery requested");
        return startReadOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_BATTERY_LEVEL);
    }

    /**
     * Initiates a read firmware version operation on this correct BtProtocol
     */
    public boolean readFwVersion(){
        Log.i(TAG, "read firmware version requested");
        return startReadOperation(MelomindCharacteristics.CHARAC_INFO_FIRMWARE_VERSION);
    }
    /**
     * Initiates a read hardware version operation on this correct BtProtocol
     */
    public boolean readHwVersion(){
        Log.i(TAG, "read hardware version requested");
        return startReadOperation(MelomindCharacteristics.CHARAC_INFO_HARDWARE_VERSION);
    }

    /**
     * Initiates a read serial number operation on this correct BtProtocol
     */
    public boolean readSerialNumber(){
        Log.i(TAG, "read serial number requested");
        return startReadOperation(MelomindCharacteristics.CHARAC_INFO_SERIAL_NUMBER);
    }


    public void testAcquireDataRandomByte(){ //eeg matrix size
        byte[] data = new byte[250];
        for (int i=0; i<63; i++){// buffer size = 1000=16*62,5 => matrix size always = 1000/2 = 500
            new Random().nextBytes(data); //Generates random bytes and places them into a user-supplied byte array
            this.notifyNewDataAcquired(data);
            Arrays.fill(data,0,0,(byte)0);
        }
    }


    /**
     * Callback called by the {@link MbtGattController gatt controller} when the notification state has changed.
     * @param isSuccess if the modification state is correctly changed
     * @param characteristic the characteristic which had its notification state changed
     * @param wasEnableRequest if the request was to enable (true) or disable (false) request.
     */
    public void onNotificationStateChanged(boolean isSuccess, BluetoothGattCharacteristic characteristic, boolean wasEnableRequest) {
        if(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG.equals(characteristic.getUuid())){
            if(wasEnableRequest && isSuccess){
                notifyStreamStateChanged(StreamState.STARTED);
            }else if(!wasEnableRequest && isSuccess){
                notifyStreamStateChanged(StreamState.STOPPED);
            }else{notifyStreamStateChanged(StreamState.FAILED);}
        }else if (MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX.equals(characteristic.getUuid())){
            //TODO see what's important here
        }else if (MelomindCharacteristics.CHARAC_HEADSET_STATUS.equals(characteristic.getUuid())){
            //TODO see what's important here
        }
    }

    /**
     * Callback called by the {@link MbtGattController gatt controller} when the connection state has changed.
     * @param newState the new {@link BtState state}
     */
    @Override
    public void notifyConnectionStateChanged(@NonNull BtState newState) {
        super.notifyConnectionStateChanged(newState);
        if(newState == BtState.DISCONNECTED){
            if(isStreaming()){
                streamingState = StreamState.IDLE;
            }
        }
    }

    /**
     * Initiates a change MTU request in order to have bigger (or smaller) bluetooth notifications.
     * The default size is also the minimum size : 23
     * The maximum size is set to 121.
     * This method is synchronous and blocks the calling thread until operation is complete.
     *
     *
     * See {@link BluetoothGatt#requestMtu(int)} for more info.
     *
     * @param newMTU the new MTU value.
     *
     * @return false if request dod not start as planned, true otherwise.
     */
    public synchronized boolean changeMTU(@IntRange(from = 23, to = 121)final int newMTU) {
        Log.i(TAG, "changing mtu to " + newMTU);
        if(!isConnected()){
            return false;
        }

        if(this.gatt == null)
            return false;

        if(!this.gatt.requestMtu(newMTU))
            return false;

        try {
            this.wait(5000);
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Initiates a write operation in order to change the embedded filter values in the melomind firmware.
     * It first requires that notifications are enabled to the {@link MelomindCharacteristics#CHARAC_MEASUREMENT_MAILBOX}
     * charateristic are enabled.
     * @param newConfig the new config.
     */
    public boolean changeFilterConfiguration(FilterConfig newConfig){
        if(!isNotificationEnabledOnCharacteristic(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX)){
            enableOrDisableNotificationsOnCharacteristic(true, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX));
        }

        ByteBuffer nameToBytes = ByteBuffer.allocate(2); // +1 for mailbox code
        nameToBytes.put(MailboxEvents.MBX_SET_NOTCH_FILT);
        nameToBytes.put((byte)newConfig.getNumVal());
        return startWriteOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX, nameToBytes.array());

    }

    /**
     * Initiates a write operation in order to change the embedded ampli gain value in the melomind firmware.
     * It first requires that notifications are enabled to the {@link MelomindCharacteristics#CHARAC_MEASUREMENT_MAILBOX}
     * charateristic are enabled.
     * @param newConfig the new config.
     */
    public boolean changeAmpGainConfiguration(AmpGainConfig newConfig){
        if(!isNotificationEnabledOnCharacteristic(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX)){
            enableOrDisableNotificationsOnCharacteristic(true, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX));
        }
        ByteBuffer nameToBytes = ByteBuffer.allocate(2); // +1 for mailbox code
        nameToBytes.put(MailboxEvents.MBX_SET_AMP_GAIN);
        nameToBytes.put((byte)newConfig.getNumVal());
        return startWriteOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX, nameToBytes.array());

    }

    /**
     * Initiates a write operation in order to enable or disable the p300 mode in the melomind firmware.
     * It first requires that notifications are enabled to the {@link MelomindCharacteristics#CHARAC_MEASUREMENT_MAILBOX}
     * charateristic are enabled.
     * @param useP300 is true to enable, false to disable.
     */
    public boolean switchP300Mode(boolean useP300){
        Log.i(TAG, "switch p300: new mode is " + (useP300 ? "enabled" : "disabled"));

        if(!isNotificationEnabledOnCharacteristic(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX)){
            enableOrDisableNotificationsOnCharacteristic(true, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX));
        }

        ByteBuffer nameToBytes = ByteBuffer.allocate(2); // +1 for mailbox code
        nameToBytes.put(MailboxEvents.MBX_P300_ENABLE);
        nameToBytes.put((byte)(useP300 ? 0x01 : 0x00));
        return startWriteOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX, nameToBytes.array());
    }

    /**
     * Initiates a write operation in order to enable or disable the p300 mode in the melomind firmware.
     * It first requires that notifications are enabled to the {@link MelomindCharacteristics#CHARAC_MEASUREMENT_MAILBOX}
     * charateristic are enabled.
     * @return
     */
    public boolean requestDeviceConfig(){
        byte[] code = {MailboxEvents.MBX_GET_EEG_CONFIG};
        return startWriteOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX, code);
    }


}
