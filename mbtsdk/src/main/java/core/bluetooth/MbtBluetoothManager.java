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

    private BluetoothDevice bluetoothDevice;

    public MbtBluetoothManager(@NonNull Context context){
        //save client side objects in variables
        mContext = context;
        mbtBluetoothLE = new MbtBluetoothLE(context);
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

    /**
     * Initiates the acquisition of EEG data. This method chooses between the correct BtProtocol
     * @return false upon immediate failure, true otherwise
     */
    public boolean startStream(){
        return false;
    }

    /**
     * Initiates the acquisition of EEG data from the correct BtProtocol
     * @return false upon immediate failure, true otherwise
     */
    public boolean stopStream(){
        return false;
    }

    /**
     * Initiates a read battery operation on this correct BtProtocol
     */
    public void readBattery(){

    }

    /**
     * Initiates a read firmware version operation on this correct BtProtocol
     */
    public void readFwVersion(){

    }

    /**
     * Initiates a read hardware version operation on this correct BtProtocol
     */
    public void readHwVersion(){

    }

    /**
     * Initiates a read serial number operation on this correct BtProtocol
     */
    public void readSerialNumber(){

    }

}
