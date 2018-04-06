package core.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import config.MbtConfig;
import core.bluetooth.lowenergy.MbtBluetoothLE;
import core.eeg.signalprocessing.ContextSP;
import mbtsdk.com.mybraintech.mbtsdk.BuildConfig;
import utils.AsyncUtils;

/**
 * Created by Etienne on 08/02/2018.
 *
 * This class contains all necessary methods to manage the Bluetooth communication with the myBrain peripheral devices.
 *- 3 Bluetooth layers are used :
 *  - Bluetooth Low Energy protocol is used with Melomind Headset for communication.
 *  - Bluetooth SPP protocol which is used for the VPro headset communication.
 *  - Bluetooth A2DP is used for Audio stream.
 *
 * We scan first with the Low Energy Scanner as it is more efficient than the classical Bluetooth discovery scanner.
 */

public final class MbtBluetoothManager {
    private final static String TAG = MbtBluetoothManager.class.getSimpleName();

    private Context mContext;

    private MbtBluetoothLE mbtBluetoothLE;
    private MbtBluetoothA2DP mbtBluetoothA2DP;
    private MbtBluetoothSPP mbtBluetoothSPP;
    private BtProtocol btProtocol;

    private BluetoothDevice bluetoothDevice;


    public MbtBluetoothManager(@NonNull Context context){
        try {
            System.loadLibrary(ContextSP.LIBRARY_NAME + BuildConfig.USE_ALGO_VERSION);
        } catch (final UnsatisfiedLinkError e) {
            e.printStackTrace();
        }

        //save client side objects in variables
        mContext = context;
        mbtBluetoothLE = new MbtBluetoothLE(context);
        mbtBluetoothSPP = new MbtBluetoothSPP(context);
        mbtBluetoothA2DP = new MbtBluetoothA2DP(context);
    }


    public boolean connect(){
        //first step
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                BluetoothDevice scannedDevice = null;
                try {
                    scannedDevice = scanSingle("").get(20, TimeUnit.SECONDS);
                    Log.i(TAG, "scanned device is " + scannedDevice.toString());
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    //TODO
                    e.printStackTrace();
                }finally {
                    mbtBluetoothLE.stopLowEnergyScan();
                }

                if(scannedDevice == null)
                    return;

                mbtBluetoothLE.connect(mContext, scannedDevice); //second step
            }
        });

            //TODO
        return true;
    }


    /**
     * Connect to a specific BluetoothDevice. This allows to skip the scanning part and jump directly to connection step
     * @param device the Bluetooth device to connect to
     * @return immediatly the following : false if device is null, true if connection step has been started
     */
    public boolean connect(@NonNull BluetoothDevice device){
        if(device == null)
            return false;

        mbtBluetoothLE.connect(mContext, device);
        //TODO
        return true;
    }



    /**
     *
     */
    //Lister les input:
    // - Device Type / Bluetooth protocol
    public void scanDevices (/*, MbtScanCallback scanCallback*/){
        //TODO choose method name accordingly between scan() / scanFor() / ...

        //Check device type from config.deviceType

        if (MbtConfig.scannableDevices == MbtConfig.ScannableDevices.ALL && ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mbtBluetoothLE.startLowEnergyScan(true, false); //TODO handle this

        else
            mbtBluetoothLE.startScanDiscovery(mContext);
    }


    /**
     * Start scanning a single device by filtering on its name. This method is asynchronous
     * @param deviceName The broadcasting name of the device to scan
     * @return a {@link Future} object holding the {@link BluetoothDevice} instance of the device to scan.
     */
    public Future<BluetoothDevice> scanSingle (String deviceName){
        //TODO choose method name accordingly between scan() / scanFor() / ...

        return AsyncUtils.executeAsync(new Callable<BluetoothDevice>() {
            @Override
            public BluetoothDevice call() throws Exception {
                Log.i(TAG, "in call method. About to start scan LE");
                if (MbtConfig.scannableDevices == MbtConfig.ScannableDevices.ALL && ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    return mbtBluetoothLE.startLowEnergyScan(true, false); //TODO handle this
//                else
//                    return mbtBluetoothLE.startScanDiscovery(mContext);
                else
                    mbtBluetoothSPP.startScanDiscovery(mContext);
                    return null; //TODO handle scanDiscovery

            }
        });
    }


    /**
     *
     */
    private void stopCurrentScan(){
        if (MbtConfig.scannableDevices == MbtConfig.ScannableDevices.ALL && ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mbtBluetoothLE.stopLowEnergyScan(); //TODO handle this
//                else
//                    return mbtBluetoothLE.startScanDiscovery(mContext);
        else
            mbtBluetoothLE.stopScanDiscovery();
    }

    private synchronized void readBattery() {
        /*if (!this.gatt.readCharacteristic(this.battery)) {
            Log.e(TAG, "Error: failed to initiate read characteristic operation in order " +
                    "to retrieve the current battery value from remote device");
            return;
        }

        Log.i(TAG, "Successfully initiated read characteristic operation in order " +
                "to retrieve the current battery value from remote device");

        final Short level = this.batteryLock.waitAndGetResult(1000);
        if (level == null) {
            Log.e(TAG, "Error: failed to fetch battery level within allotted time of 1 second " +
                    "or fetched value was invalid !");
            return;
        }

        Log.i(TAG, "Successfully retrieved battery value from remote device within allotted" +
                " time of 1 second. Battery level is now -> " + level);

        // We only notify if battery level has indeed changed from last time
        if (level != this.currentBatteryLevel) {
            MbtBluetoothLE.this.uiAccess.post(new Runnable() {
                public final void run() {
                    notifyBatteryLevelChanged(level);
                }
            });
            this.currentBatteryLevel = level;
        }*/
    }

    private synchronized boolean getFwVersion() {
        /*if (!this.gatt.readCharacteristic(this.fwVersion)) {
            Log.e(TAG, "Error: failed to initiate read characteristic operation in order " +
                    "to retrieve the current fwVersion value from remote device");
            return false;
        }

        Log.i(TAG, "Successfully initiated read characteristic operation in order " +
                "to retrieve the current fwVersion value from remote device");

        final String fwVersion = this.readDeviceInfoLock.waitAndGetResult(5000);
        if (fwVersion == null) {
            Log.e(TAG, "Error: failed to fetch fwVersion value within allotted time of 1 second " +
                    "or fetched value was invalid !");
            return false;
        }

        Log.i(TAG, "Successfully retrieved fwVersion value from remote device within allocated");

        // We only notify if battery level has indeed changed from last time
        notifyDeviceInfoReceived(DeviceInfo.FW_VERSION, fwVersion);*/
        return true;
    }

    private synchronized boolean getHwVersion() {
        /*if (!this.gatt.readCharacteristic(this.hwVersion)) {
            Log.e(TAG, "Error: failed to initiate read characteristic operation in order " +
                    "to retrieve the current hwVersion value from remote device");
            return false;
        }

        Log.i(TAG, "Successfully initiated read characteristic operation in order " +
                "to retrieve the current hwVersion value from remote device");

        final String hwVersion = this.readDeviceInfoLock.waitAndGetResult(5000);
        if (hwVersion == null) {
            Log.e(TAG, "Error: failed to fetch hwVersion value within allotted time of 1 second " +
                    "or fetched value was invalid !");
            return false;
        }

        Log.i(TAG, "Successfully retrieved hwVersion value from remote device within allocated");

        // We only notify if battery level has indeed changed from last time
        notifyDeviceInfoReceived(DeviceInfo.HW_VERSION, hwVersion);*/
        return true;
    }

    private synchronized boolean getSerialNumber() {
        /*if (!this.gatt.readCharacteristic(this.serialNumber)) {
            Log.e(TAG, "Error: failed to initiate read characteristic operation in order " +
                    "to retrieve the current serial number from remote device");
            return false;
        }

        Log.i(TAG, "Successfully initiated read characteristic operation in order " +
                "to retrieve the current serial number from remote device");

        final String serialNumber = this.readDeviceInfoLock.waitAndGetResult(5000);
        if (serialNumber == null) {
            Log.e(TAG, "Error: failed to fetch serial number within allotted time of 1 second " +
                    "or fetched value was invalid !");
            return false;
        }

        Log.i(TAG, "Successfully retrieved serial number from remote device within allocated");

        // We only notify if battery level has indeed changed from last time
        notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER, serialNumber);*/
        return true;
    }

}
