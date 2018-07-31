package core.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import config.MbtConfig;
import config.DeviceConfig;
import core.BaseModuleManager;
import core.MbtManager;
import core.bluetooth.lowenergy.MbtBluetoothLE;
import core.bluetooth.requests.BluetoothRequests;
import core.bluetooth.requests.ConnectRequestEvent;
import core.bluetooth.requests.DisconnectRequestEvent;
import core.bluetooth.requests.ReadRequestEvent;
import core.bluetooth.requests.StreamRequestEvent;
import core.bluetooth.requests.UpdateConfigurationRequestEvent;
import core.bluetooth.spp.MbtBluetoothSPP;
import core.device.RawDeviceMeasure;
import core.recordingsession.metadata.DeviceInfo;
import eventbus.EventBusManager;
import eventbus.events.BluetoothEEGEvent;
import eventbus.events.ConnectionStateEvent;
import eventbus.events.DeviceInfoEvent;
import features.MbtFeatures;
import features.ScannableDevices;
import utils.AsyncUtils;
import utils.LogUtils;

import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static core.bluetooth.BtProtocol.BLUETOOTH_SPP;

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

public final class MbtBluetoothManager extends BaseModuleManager{
    private final static String TAG = MbtBluetoothManager.class.getSimpleName();

    private MbtBluetoothLE mbtBluetoothLE;
    private MbtBluetoothA2DP mbtBluetoothA2DP;
    private MbtBluetoothSPP mbtBluetoothSPP;

    @NonNull
    private final Queue<BluetoothRequests> pendingRequests; //TODO see if still necessary
    private boolean requestBeingProcessed = false;

    private RequestThread requestThread;
    private Handler requestHandler;

    private boolean isConnectionInterrupted = false;

    private Future<BluetoothDevice> futureScannedDevice;
    private BluetoothDevice currentDevice;
    //private MbtDeviceAcquisition deviceAcquisition;

    /**
     * Constructor of the manager.
     * @param context the application context
     * @param mbtManagerController the main manager that sends and receives bluetooth events
     */
    public MbtBluetoothManager(@NonNull Context context, MbtManager mbtManagerController){
        super(context, mbtManagerController);
        //save client side objects in variables

        this.mbtBluetoothLE = new MbtBluetoothLE(context, this);
        this.mbtBluetoothSPP = new MbtBluetoothSPP(context,this);
        this.mbtBluetoothA2DP = new MbtBluetoothA2DP(context,this);

        //this.deviceAcquisition = new MbtDeviceAcquisition();
        //EventBusManager.registerOrUnregister(true, this);// register MbtBluetoothManager as a subscriber for receiving event such as ClientReadyEEGEvent event (called after EEG raw data has been converted)

        this.pendingRequests = new LinkedList<>();

        //Init thread that will handle messages synchronously. Using HandlerThread looks like it is the best way for CPU consomption as infinite loop in async thread was too heavy for cpu
        requestThread = new RequestThread("requestThread", Thread.MAX_PRIORITY);
        requestThread.start();
        requestHandler = new Handler(requestThread.getLooper());

    }

    /**
     * This method do the following operations:
     * - First scan for {@link BluetoothDevice } filtering on deviceName. The scan is performed by LE scanner if possible
     * and if the device is LE compatible. Otherwise, the discovery scan is performed instead.
     * - Perform the connect operation if scan is successful.
     * @param deviceName the device bluetooth name.
     */
    private void scanAndConnect(@NonNull String deviceName){
        if(getCurrentState() == BtState.CONNECTED_AND_READY){
            if(currentDevice != null && currentDevice.getName().equals(deviceName)){
                notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
            }else{
                notifyConnectionStateChanged(BtState.ANOTHER_DEVICE_CONNECTED);
            }
            return;
        }

        
        isConnectionInterrupted = false; // resetting the flag when starting a new connection
        if(!BluetoothAdapter.getDefaultAdapter().isEnabled()){
            notifyConnectionStateChanged(BtState.DISABLED);
            return;
        }

        //Checking location permission
        if (MbtConfig.scannableDevices == ScannableDevices.MELOMIND){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED
                    && ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){

                notifyConnectionStateChanged(BtState.LOCATION_PERMISSION_NOT_GRANTED);
                return;
            }

            //Checking location activation
            LocationManager manager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE );
            if(manager != null && !manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)){
                notifyConnectionStateChanged(BtState.LOCATION_IS_REQUIRED);
                return;
            }
        }else{
            //todo
        }


        //first step
        try {
            futureScannedDevice= scanSingle(deviceName);
            currentDevice = futureScannedDevice.get(MbtConfig.getBluetoothScanTimeout(), TimeUnit.MILLISECONDS);

        } catch (@NonNull InterruptedException | ExecutionException | TimeoutException | CancellationException e) {
            //TODO
            e.printStackTrace();
        }finally {
            stopCurrentScan();
        }
        BluetoothDevice bluetoothDevice;
        if(currentDevice == null){
            notifyConnectionStateChanged(isConnectionInterrupted ? BtState.INTERRUPTED : BtState.SCAN_TIMEOUT);
            return;
        }else {
            LogUtils.i(TAG, "scanned device is " + currentDevice.toString());
            notifyConnectionStateChanged(BtState.DEVICE_FOUND);
            bluetoothDevice = currentDevice;
        }

        //second step
        connect(bluetoothDevice);
    }


    /**
     * ConnectRequestEvent to a specific BluetoothDevice. This allows to skip the scanning part and jump directly to connection step.
     * The method returns immediately if a connection interruption has been sent by the user
     * @param device the Bluetooth device to connect to
     * @return immediately the following : false if device is null, true if connection step has been started
     */
    private void connect(@NonNull BluetoothDevice device){
        if(isConnectionInterrupted){
            notifyConnectionStateChanged(BtState.INTERRUPTED);
            return;
        }

        switch (MbtFeatures.getBluetoothProtocol()){
            case BLUETOOTH_LE:
                mbtBluetoothLE.connect(mContext, device);
                break;
            case BLUETOOTH_SPP:
                mbtBluetoothSPP.connect(mContext, device);
                break;
            case BLUETOOTH_A2DP:
                mbtBluetoothA2DP.connect(mContext, device);
                break;
        }

    }

    /**
     * This method starts a bluetooth scan operation.
     * if the device to scan is a melomind, the bluetooth LE scanner is invoked.
     * <p>Requires {@link Manifest.permission#ACCESS_FINE_LOCATION} or {@link Manifest.permission#ACCESS_COARSE_LOCATION} permission.
     *
     * If permissions are not given and/or bluetooth device is not Le compatible, discovery scan is started.
     */
    //Lister les input:
    // - Device Type / Bluetooth protocol
    private void scanDevices(/*, MbtScanCallback scanCallback*/){
        //TODO choose method name accordingly between scan() / scanFor() / ...

        //Check device type from config.deviceType

        if (MbtConfig.scannableDevices == ScannableDevices.MELOMIND && ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mbtBluetoothLE.startLowEnergyScan(true, null); //TODO handle this

        else
            mbtBluetoothLE.startScanDiscovery(MbtFeatures.getDeviceName());
    }


    /**
     * Start scanning a single device by filtering on its name. This method is asynchronous
     * @param deviceName The broadcasting name of the device to scan
     * @return a {@link Future} object holding the {@link BluetoothDevice} instance of the device to scan.
     */
    private Future<BluetoothDevice> scanSingle(@NonNull final String deviceName){ //todo check that
        //TODO choose method name accordingly between scan() / scanFor() / ...


        return AsyncUtils.executeAsync(new Callable<BluetoothDevice>() {
            @Nullable
            @Override
            public BluetoothDevice call() throws Exception {

                if(MbtFeatures.getBluetoothProtocol()== BLUETOOTH_LE){
                    LogUtils.i(TAG, "in call method. About to start scan LE");
                    return mbtBluetoothLE.startLowEnergyScan(true, deviceName);
                }
                else
                    LogUtils.i(TAG, "About to start scan discovery");

                    return mbtBluetoothSPP.startScanDiscovery(deviceName);
            }
        });
    }


    /**
     * This method stops the currently running bluetooth scan, either Le scan or discovery scan
     */
    private void stopCurrentScan(){
        LogUtils.i(TAG, "stopping current scan");
        if (MbtConfig.scannableDevices == ScannableDevices.MELOMIND && ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mbtBluetoothLE.stopLowEnergyScan(); //TODO handle this

        else
            mbtBluetoothLE.stopScanDiscovery();
    }


    /**
     * This method manages a set of calls to perform in order to reconfigure some of the headset's
     * parameters. All parameters are held in a {@link DeviceConfig instance}
     * Each new parameter is updated one after the other. All method inside are blocking.
     * @param config the {@link DeviceConfig} instance to get new parameters from.
     */
    private void configureHeadset(@NonNull DeviceConfig config){
        boolean stepSuccess = true;
        if(config != null){
            //Checking whether or not there are params to send
            if (config.getMtuValue() != -1) {
                stepSuccess = mbtBluetoothLE.changeMTU(config.getMtuValue());
            }

            if(!stepSuccess){
                LogUtils.e(TAG, "step has timeout. Aborting task...");
                return;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (config.getNotchFilter() != null) {
               stepSuccess = mbtBluetoothLE.changeFilterConfiguration(config.getNotchFilter());

                //TODO implement bandpass filter change
//            if(config.getBandpassFilter() != null){
//                boolean b = changeFilterConfiguration(config.getBandpassFilter());
//                if(!b)
//                    LogUtils.e(TAG, "Error changing bandpass filter configuration");
//            }
            }

            if(!stepSuccess){
                LogUtils.e(TAG, "step has timeout. Aborting task...");
                return;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (config.getGainValue() != null) {
                stepSuccess = mbtBluetoothLE.changeAmpGainConfiguration(config.getGainValue());

            }

            if(!stepSuccess){
                LogUtils.e(TAG, "step has timeout. Aborting task...");
                return;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stepSuccess = mbtBluetoothLE.switchP300Mode(config.isUseP300());
            if(!stepSuccess){
                LogUtils.e(TAG, "step has timeout. Aborting task...");
                return;
            }
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        stepSuccess = mbtBluetoothLE.requestDeviceConfig();

        if(!stepSuccess){
            LogUtils.e(TAG, "step has timeout. Aborting task...");
            return;
        }

        //reconfigureBuffers(SAMPRATE, NB_CHANNELS, melomindDevice.getInternalConfig().getNbPackets(), melomindDevice.getInternalConfig().getStatusBytes());
        EventBusManager.postEvent(Void.TYPE/*TODO*/);

    }




    /**
     * Initiates the acquisition of EEG data. This method chooses between the correct BtProtocol.
     * If there is already a streaming session in progress, nothing happens and the method returns silently.
     */
    private void startStream(boolean enableDeviceStatusMonitoring){
        if(!mbtBluetoothLE.isConnected()){
            notifyStreamStateChanged(IStreamable.StreamState.DISCONNECTED);
            return;
        }

        if(mbtBluetoothLE.isStreaming()){
            return;
        }

        //TODO remove configureHeadset method from here later on.
        configureHeadset(new DeviceConfig.Builder().useP300(false).create());

        if(enableDeviceStatusMonitoring)
            mbtBluetoothLE.activateDeviceStatusMonitoring();

        if(!mbtBluetoothLE.startStream()){
            requestBeingProcessed  = false;
            EventBusManager.postEvent(IStreamable.StreamState.FAILED);
        }
    }

    /**
     * Initiates the acquisition of EEG data from the correct BtProtocol
     * If there is no streaming session in progress, nothing happens and the method returns silently.
     */
    private void stopStream(){
        if(!mbtBluetoothLE.isStreaming())
            return;

        if(!mbtBluetoothLE.stopStream()){
            requestBeingProcessed  = false;
            EventBusManager.postEvent(IStreamable.StreamState.FAILED);
        }
    }

    /**
     * Initiates a read battery operation on this correct BtProtocol.
     * In case of failure during read process, an event with error is posted to the main manager.
     */
    private void readBattery() {
        if(!mbtBluetoothLE.readBattery()){
            requestBeingProcessed = false;
            EventBusManager.postEvent(new DeviceInfoEvent<>(DeviceInfo.BATTERY, null));
        }
    }


    /**
     * Initiates a read firmware version operation on this correct BtProtocol
     * In case of failure during read process, an event with error is posted to the main manager.
     */
    private void readFwVersion(){
        if(!mbtBluetoothLE.readFwVersion()){
            requestBeingProcessed = false;
            EventBusManager.postEvent(new DeviceInfoEvent<>(DeviceInfo.FW_VERSION, null));
        }
    }


    /**
     * Initiates a read hardware version operation on this correct BtProtocol
     * In case of failure during read process, an event with error is posted to the main manager.
     */
    private void readHwVersion(){
        if(!mbtBluetoothLE.readHwVersion()){
            requestBeingProcessed = false;
            EventBusManager.postEvent(new DeviceInfoEvent<>(DeviceInfo.HW_VERSION, null));
        }
    }


    /**
     * Initiates a read serial number operation on this correct BtProtocol
     * In case of failure during read process, an event with error is posted to the main manager.
     */
    private void readSerialNumber(){
        if(!mbtBluetoothLE.readSerialNumber()){
            requestBeingProcessed = false;
            EventBusManager.postEvent(new DeviceInfoEvent<>(DeviceInfo.SERIAL_NUMBER, null));
        }
    }


    /**
     * Start the disconnect operation on the currently connected bluetooth device according to the {@link BtProtocol} currently used.
     */
    private void disconnect() {
        switch(MbtFeatures.getBluetoothProtocol()){
            case BLUETOOTH_LE:
                if(mbtBluetoothLE.getCurrentState().equals(BtState.CONNECTED_AND_READY))
                    this.mbtBluetoothLE.disconnect();
                break;
            case BLUETOOTH_SPP:
                this.mbtBluetoothSPP.disconnect();
                break;
            case BLUETOOTH_A2DP:
                this.mbtBluetoothA2DP.disconnect();
                break;
        }
    }


    /**
     * Stops current pending connection according to its current {@link BtState state}.
     * It can be either stop scan or connection process interruption
     */
    private void cancelPendingConnection() {
        LogUtils.i(TAG, "cancelling pending connection");
        isConnectionInterrupted = true;
        if(getCurrentState() == BtState.SCAN_STARTED){
            if(futureScannedDevice != null)
                futureScannedDevice.cancel(false);
        }else if(getCurrentState() == BtState.CONNECTING || getCurrentState() == BtState.CONNECTED){
            disconnect();
        }
    }



    /**
     * Gets current state according to bluetooth protocol value
     * @return
     */
    private BtState getCurrentState(){
        if(MbtFeatures.getBluetoothProtocol() == BLUETOOTH_LE)
            return mbtBluetoothLE.getCurrentState();

        return mbtBluetoothSPP.getCurrentState();
    }



    /**
     * Posts a BluetoothEEGEvent event to the bus so that MbtEEGManager can handle raw EEG data received
     * @param data the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     */
    public void handleDataAcquired(@NonNull final byte[] data){
        EventBusManager.postEvent(new BluetoothEEGEvent(data)); //MbtEEGManager will convert data from raw packets to eeg values
    }


    /**
     * Unregister the MbtBluetoothManager class from the bus to avoid memory leak
     */
    private void deinit(){
        EventBusManager.registerOrUnregister(false,this);
    }



    /**
     * Add the new {@link BluetoothRequests} to the handler thread that will execute tasks one after another
     * This method must return quickly in order not to block the thread.
     * @param request the new {@link BluetoothRequests } to execute
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onNewBluetoothRequest(final BluetoothRequests request){
        //Specific case: disconnection has main priority so we don't add it to queue
        LogUtils.i(TAG, "onNewBTRequest");
        if(request instanceof DisconnectRequestEvent && ((DisconnectRequestEvent) request).isInterrupted())
            cancelPendingConnection();
        else
            requestHandler.post(new Runnable() {
                @Override
                public void run() {
                    requestThread.parseRequest(request);
                }
            });
    }

    /**
     * This method is called from Bluetooth classes and is meant to post an event to the main manager
     * that contains the new {@link BtState}
     * @param newState the new {@link BtState}
     */
    public void notifyConnectionStateChanged(BtState newState) {
        if(newState == BtState.CONNECTED_AND_READY ||newState == BtState.DISCONNECTED || newState == BtState.SCAN_TIMEOUT || newState == BtState.DISABLED || newState == BtState.INTERNAL_FAILURE
                || newState == BtState.LOCATION_IS_REQUIRED ||newState == BtState.LOCATION_PERMISSION_NOT_GRANTED || newState == BtState.INTERRUPTED || newState == BtState.ANOTHER_DEVICE_CONNECTED){
            requestBeingProcessed = false;
        }

        //TODO improve this method

        EventBusManager.postEvent(new ConnectionStateEvent(newState));
    }


    /**
     * This method is called from Bluetooth classes and is meant to post an event to the main manager
     * that contains the {@link DeviceInfo} with the associated value
     * @param deviceInfo the {@link DeviceInfo}
     * @param deviceValue the new value as String
     */
    public void notifyDeviceInfoReceived(DeviceInfo deviceInfo, String deviceValue){
        requestBeingProcessed = false;
        EventBusManager.postEvent(new DeviceInfoEvent<String>(deviceInfo, deviceValue));
    }



    /**
     * This method is called from Bluetooth classes and is meant to post an event to the main manager
     * that contains the {@link IStreamable.StreamState} new state
     * @param newStreamState the {@link IStreamable.StreamState} new state
     */
    public void notifyStreamStateChanged(IStreamable.StreamState newStreamState) {
        requestBeingProcessed = false;
        EventBusManager.postEvent(newStreamState);
    }


    public void notifyNewHeadsetStatus(BtProtocol protocol, @NonNull byte[] payload) {
        EventBusManager.postEvent(new RawDeviceMeasure(payload));
    }

    /**
     * This class is a specific thread that will handle all bluetooth operations. Bluetooth operations
     * are synchronous, meaning two or more operations can't be run simultaneously. This {@link HandlerThread}
     * extended class is able to hold pending operations.
     */
    private class RequestThread extends HandlerThread {
        RequestThread(String name) {
            super(name);
        }

        RequestThread(String name, int priority) {
            super(name, priority);
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
        }

        /**
         * Checks the subclass type of {@link BluetoothRequests} and handles the correct method/action to perform.
         * @param request the {@link BluetoothRequests} request to execute.
         */
        void parseRequest(BluetoothRequests request){
            LogUtils.i(TAG,"parsing new request");
            //BluetoothRequests request = pendingRequests.remove();

            //disconnect request doesn't need to be in "queue" as it is top priority

            while(requestBeingProcessed);
            LogUtils.i(TAG,"bt execution thread is now free");
            requestBeingProcessed = true;
            if(request instanceof ConnectRequestEvent){
                if(((ConnectRequestEvent)request).getName() == null){
                    scanDevices(); //not used yet.
                }else{
                    scanAndConnect(((ConnectRequestEvent)request).getName());
                }
            } else if(request instanceof ReadRequestEvent){
                performReadOperation(((ReadRequestEvent)request).getDeviceInfo());
            } else if(request instanceof DisconnectRequestEvent){
                if(((DisconnectRequestEvent) request).isInterrupted())
                    cancelPendingConnection();
                else
                    disconnect();
            } else if(request instanceof StreamRequestEvent){
                if(((StreamRequestEvent) request).isStart())
                    startStream(((StreamRequestEvent) request).shouldMonitorDeviceStatus());
                else
                    stopStream();
            } else if(request instanceof UpdateConfigurationRequestEvent){
                configureHeadset(((UpdateConfigurationRequestEvent) request).getConfig());
            }
        }



        /**
         * If the {@link BluetoothRequests request} is a {@link ReadRequestEvent} event, this method
         * is called to parse which read operation is to be executed according to the {@link DeviceInfo}.
         * @param deviceInfo the {@link DeviceInfo} info that determine which read to operation to execute.
         */
        private void performReadOperation(DeviceInfo deviceInfo) {
            switch(deviceInfo){
                case BATTERY:
                    readBattery();
                    break;
                case FW_VERSION:
                    readFwVersion();
                    break;
                case HW_VERSION:
                    readHwVersion();
                    break;
                case SERIAL_NUMBER:
                    readSerialNumber();
                    break;
                default:
                    break;
            }
        }
    }


}
