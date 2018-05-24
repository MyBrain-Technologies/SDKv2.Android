package core.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import config.MbtConfig;
import core.MbtManager;
import core.bluetooth.lowenergy.MbtBluetoothLE;
import core.bluetooth.acquisition.MbtDeviceAcquisition;
import eventbus.EventBusManager;
import eventbus.events.EEGDataIsReady;
import eventbus.events.EEGDataAcquired;
import features.MbtFeatures;
import features.ScannableDevices;
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
    private final Handler uiAccess;

    private BluetoothDevice bluetoothDevice;
    private BtProtocol btProtocol;

    private MbtBluetoothLE mbtBluetoothLE;
    private MbtBluetoothA2DP mbtBluetoothA2DP;
    private MbtBluetoothSPP mbtBluetoothSPP;

    private MbtManager mbtManager;
    private MbtDeviceAcquisition deviceAcquisition;
    private EventBusManager eventBusManager;

    public MbtBluetoothManager(@NonNull Context context, MbtManager mbtManagerController){

        //save client side objects in variables
        this.mContext = context;
        this.uiAccess = new Handler(context.getMainLooper());
        this.btProtocol = BtProtocol.BLUETOOTH_SPP; //Default value //todo change if tests are successful

        this.mbtBluetoothLE = new MbtBluetoothLE(context, this);
        this.mbtBluetoothSPP = new MbtBluetoothSPP(context,this);
        this.mbtBluetoothA2DP = new MbtBluetoothA2DP(context,this);

        this.mbtManager = mbtManagerController;
        this.deviceAcquisition = new MbtDeviceAcquisition();
        this.eventBusManager = new EventBusManager();// register MbtBluetoothManager as a subscriber for receiving event such as EEGDataIsReady event (called after EEG raw data has been converted)

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

                if(scannedDevice == null){
                    return;
                }else {
                    bluetoothDevice = scannedDevice;
                }

                mbtBluetoothLE.connect(mContext, scannedDevice); //second step
            }
        });
        return connect(bluetoothDevice);
    }


    /**
     * Connect to a specific BluetoothDevice. This allows to skip the scanning part and jump directly to connection step
     * @param device the Bluetooth device to connect to
     * @return immediatly the following : false if device is null, true if connection step has been started
     */
    private boolean connect(@NonNull BluetoothDevice device){
        return mbtBluetoothLE.connect(mContext, device);
    }

    /**
     *
     */
    //Lister les input:
    // - Device Type / Bluetooth protocol
    public void scanDevices (/*, MbtScanCallback scanCallback*/){
        //TODO choose method name accordingly between scan() / scanFor() / ...

        //Check device type from config.deviceType

        if (MbtConfig.scannableDevices == ScannableDevices.ALL && ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mbtBluetoothLE.startLowEnergyScan(true, false); //TODO handle this

        else
            mbtBluetoothLE.startScanDiscovery(mContext, MbtFeatures.getDeviceName());
    }


    /**
     * Start scanning a single device by filtering on its name. This method is asynchronous
     * @param deviceName The broadcasting name of the device to scan
     * @return a {@link Future} object holding the {@link BluetoothDevice} instance of the device to scan.
     */
    public Future<BluetoothDevice> scanSingle (final String deviceName){ //todo check that
        //TODO choose method name accordingly between scan() / scanFor() / ...
        switch (btProtocol){
            case BLUETOOTH_LE:
                if(mbtBluetoothLE.getMelomindDevice()!=null)
                    mbtBluetoothLE.getMelomindDevice().setProductName(deviceName);
                else if (mbtBluetoothLE.getVproDevice()!=null)
                    mbtBluetoothLE.getVproDevice().setProductName(deviceName);
                break;
            case BLUETOOTH_SPP:
                if(mbtBluetoothSPP.getMelomindDevice()!=null)
                    mbtBluetoothSPP.getMelomindDevice().setProductName(deviceName);
                else if (mbtBluetoothSPP.getVproDevice()!=null)
                    mbtBluetoothSPP.getVproDevice().setProductName(deviceName);
                break;
            case BLUETOOTH_A2DP:
                if(mbtBluetoothA2DP.getMelomindDevice()!=null)
                    mbtBluetoothA2DP.getMelomindDevice().setProductName(deviceName);
                else if (mbtBluetoothA2DP.getVproDevice()!=null)
                    mbtBluetoothA2DP.getVproDevice().setProductName(deviceName);
        }

        return AsyncUtils.executeAsync(new Callable<BluetoothDevice>() {
            @Override
            public BluetoothDevice call() throws Exception {
                Log.i(TAG, "in call method. About to start scan LE");
                if (MbtConfig.scannableDevices == ScannableDevices.ALL && ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    btProtocol=BtProtocol.BLUETOOTH_LE;
                    return mbtBluetoothLE.startLowEnergyScan(true, false); //TODO handle this

                }
//                else
//                    return mbtBluetoothLE.startScanDiscovery(mContext);
                else
                    Log.i(TAG, "About to start scan discovery");
                    mbtBluetoothSPP.startScanDiscovery(mContext, deviceName);
                    btProtocol=BtProtocol.BLUETOOTH_SPP;
                    return null; //TODO handle scanDiscovery

            }
        });
    }


    /**
     *
     */
    private void stopCurrentScan(){
        if (MbtConfig.scannableDevices == ScannableDevices.ALL && ContextCompat.checkSelfPermission(mContext,
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
        mbtBluetoothLE.readBattery(deviceAcquisition.getLastKnownBatteryLevel());
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

    void notifyNewEeg(final ArrayList<ArrayList<Float>> matrix, final ArrayList<Float> status, final int nbChannels, final int nbSamples, final int sampleRate) {
    //call mbtbluetooth.notifyneweeg
    }

    /**
     * Allows the user to hot change the eeg signal amplifier gain amongst the proposed ones by sending a bluetooth command.
     * @return true upon success, false otherwise
     */
    public synchronized Byte[] getEEGConfiguration(){
        return mbtBluetoothLE.getEEGConfiguration();
    }

    public BtProtocol getBtProtocol() {
        return btProtocol;
    }

    public void setBtProtocol(BtProtocol btProtocol) {
        this.btProtocol = btProtocol;
    }

    public MbtBluetoothLE getMbtBluetoothLE() {
        return mbtBluetoothLE;
    }

    public MbtBluetoothA2DP getMbtBluetoothA2DP() {
        return mbtBluetoothA2DP;
    }

    public MbtBluetoothSPP getMbtBluetoothSPP() {
        return mbtBluetoothSPP;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public MbtDeviceAcquisition getDeviceAcquisition() {
        return deviceAcquisition;
    }

    public void disconnect() {

        switch(this.btProtocol){
            case BLUETOOTH_LE:
                this.mbtBluetoothLE.disconnect(bluetoothDevice);
                break;
            case BLUETOOTH_SPP:
                this.mbtBluetoothSPP.disconnect(bluetoothDevice);
                break;
            case BLUETOOTH_A2DP:
                this.mbtBluetoothA2DP.disconnect(bluetoothDevice);
                break;
        }
    }

    /**
     * Store the new battery level received from the headset
     * @param pourcent new battery level in percentage
     */
    public void updateBatteryLevel(int pourcent){
        deviceAcquisition.setBatteryLevel(pourcent);
    }

    /**
     * publish an event on the bus so that MbtEEGManager can handle raw EEG data received from Bluetooth
     * @param data raw EEG Data
     */
    public void acquireData(@NonNull final byte[] data){
        Log.e(TAG, "Acquire data BT Manager:" + Arrays.toString(data)); //todo remove
        eventBusManager.postEvent(new EEGDataAcquired(data)); //MbtEEGManager will convert data from raw packets to eeg values
    }

    /**
     * onEvent is called when the event bus receive a EEGDataIsReady event posted by MbtDataAcquisition in handleDataAcquired method
     * @param event EEGDataIsReady is posted when the EEG data is ready (raw EGG data has been converted to Float matrix)
     */
    /*@Subscribe
    public void onEvent(EEGDataIsReady event) {
        switch (this.btProtocol){
            case BLUETOOTH_LE:
                this.mbtBluetoothLE.EEGDataIsReadyReceived(event);
                break;
            case BLUETOOTH_SPP:
                this.mbtBluetoothSPP.EEGDataIsReadyReceived(event);
                break;
            case BLUETOOTH_A2DP:
                this.mbtBluetoothA2DP.EEGDataIsReadyReceived(event);
                break;
        }
    }*/

    /**
     * Unregister the MbtBluetoothManager class from the bus to avoid memory leak
     */
    /*public void deinit(){
        eventBusManager.registerOrUnregister(false,this);
    }*/

}
