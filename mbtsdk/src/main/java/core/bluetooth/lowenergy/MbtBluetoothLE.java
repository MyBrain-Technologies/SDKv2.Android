package core.bluetooth.lowenergy;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import core.bluetooth.BtState;
import core.bluetooth.IConnectable;
import core.bluetooth.IScannable;
import core.bluetooth.IStreamable;
import core.bluetooth.MbtBluetooth;
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
        this.mbtGattController = new MbtGattController(this);
    }

    @Override
    public boolean startStream() {
        return false;
    }

    @Override
    public boolean stopStream() {
        return false;
    }

    /**
     *
     * @param filterOnDeviceService
     * @param filterOnDeviceName
     * @return Each found device that matches the specified filters
     */
    public BluetoothDevice startLowEnergyScan(boolean filterOnDeviceService, boolean filterOnDeviceName) {
        this.
        this.bluetoothLeScanner = super.bluetoothAdapter.getBluetoothLeScanner();
        List<ScanFilter> mFilters = new ArrayList<>();
        if(filterOnDeviceService){
            Log.i(TAG, "ENABLED SERVICE FILTER");
            final ScanFilter filterService = new ScanFilter.Builder()
                    //.setDeviceName(deviceName)
                    //.setDeviceName("melo_")
                    .setServiceUuid(new ParcelUuid(MelomindServices.SERVICE_MEASUREMENT))
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

        //Log.i(TAG, String.format("Starting Low Energy Scan with filtering on name '%s' and service UUID '%s'", deviceName, MelomindServices.SERVICE_MEASUREMENT));
        this.bluetoothLeScanner.startScan(mFilters, settings, this.leScanCallback);
        Log.i(TAG, "in scan method.");
        return super.scanLock.waitAndGetResult(20000);
    }

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
            switch (errorCode) {
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
}
