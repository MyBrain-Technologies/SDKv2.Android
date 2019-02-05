package core.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
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
import core.bluetooth.requests.DisconnectRequestEvent;
import core.bluetooth.requests.ReadRequestEvent;
import core.bluetooth.requests.ScanAndConnectRequestEvent;
import core.bluetooth.requests.ScanRequestEvent;
import core.bluetooth.requests.StreamRequestEvent;
import core.bluetooth.requests.UpdateConfigurationRequestEvent;
import core.bluetooth.spp.MbtBluetoothSPP;
import core.device.DeviceEvents;
import core.device.model.DeviceInfo;
import core.device.model.MbtDevice;
import core.device.model.MelomindDevice;
import core.device.model.MelomindsQRDataBase;
import engine.SimpleRequestCallback;
import engine.clientevents.BaseError;
import engine.clientevents.BluetoothError;
import engine.clientevents.ConnectionStateReceiver;
import engine.clientevents.FirmwareError;
import engine.clientevents.MobileDeviceError;
import eventbus.EventBusManager;
import eventbus.events.BluetoothEEGEvent;
import eventbus.events.NewConnectionStateEvent;
import eventbus.events.DeviceInfoEvent;
import features.MbtFeatures;
import utils.AsyncUtils;
import utils.BroadcastUtils;
import utils.FirmwareUtils;
import utils.LogUtils;

import static core.bluetooth.BtProtocol.BLUETOOTH_A2DP;
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
    /**+**/private boolean isDownloadingFW = false;

    private Future<BluetoothDevice> futureScannedDevice;
    private BluetoothDevice currentDevice;
    //private MbtDeviceAcquisition deviceAcquisition;

    /**+**/private boolean useAudio = false;

    /**+**/private int backgroundReconnectionRetryCounter = 0;

    /**+**/private ConnectionStateReceiver receiver = new ConnectionStateReceiver() {
        @Override
        public void onError(BaseError error, String additionnalInfo) {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null){
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, " received intent " + action + " for device " + (device != null ? device.getName() : null));
                if(useAudio && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
                    mbtBluetoothA2DP.resetA2dpProxy(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1));
            }
        }
    };

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

        /**+**/BroadcastUtils.registerReceiverIntents(context, new ArrayList<>(Arrays.asList(
                BluetoothAdapter.ACTION_STATE_CHANGED)),
                receiver);
    }

    /**
     * This method do the following operations:
     * - First scan for {@link BluetoothDevice } filtering on deviceName. The scan is performed by LE scanner if possible
     * and if the device is LE compatible. Otherwise, the discovery scan is performed instead.
     * - Perform the connect operation if scan is successful.
     * @param deviceName the device bluetooth name.
     */
    private void scanAndConnect(@Nullable String deviceName, boolean useAudio){
        this.isConnectionInterrupted = false; // resetting the flag when starting a new connection
        this.useAudio = useAudio;

        if(isAlreadyConnected(deviceName))
            return;

        if(isBluetoothDisabled()) //check if Bluetooth is on
            return;

        if(isLocationDisabledOrNotGranted()) //check if location is enabled and permissions are granted
            return;

        if(!scan(deviceName))//first step is scanning
            return;

        connect(currentDevice, (MelomindDevice.isMelomindRequested()) ? //second step is BLE (Melomind) /SPP (Vpro) connection to the found device
                BLUETOOTH_LE : BLUETOOTH_SPP);

    }

    private boolean isReadyForBluetoothAction(String deviceName){
        if(isAlreadyConnected(deviceName))
            return false;

        if(isBluetoothDisabled()) //check if Bluetooth is on
            return false;

        return !isLocationDisabledOrNotGranted();
    }

    private boolean startScan(String deviceName){
        boolean deviceIsFound = true;
        try {
            futureScannedDevice = scanDeviceAsync(deviceName);
            Log.i(TAG," future scanned device "+futureScannedDevice.toString());
            currentDevice = futureScannedDevice.get(MbtConfig.getBluetoothScanTimeout(), TimeUnit.MILLISECONDS);

            //todo following part in other method
            Log.i(TAG," scanned device from future Name "+currentDevice.getName()+" | Address "+currentDevice.getAddress()+" | Bond state "+currentDevice.getBondState()+" | Type "+currentDevice.getType());
        } catch (@NonNull InterruptedException | ExecutionException | TimeoutException | CancellationException e) {
            LogUtils.i(TAG, "Exception raised during scanning : \n " + e.toString());
        }finally {
            stopCurrentScan();
        }
        if(currentDevice == null){
            notifyConnectionStateChanged((isConnectionInterrupted ? BtState.SCAN_INTERRUPTED : BtState.SCAN_TIMEOUT), true);
            deviceIsFound = false;
        }else {
            LogUtils.i(TAG, "scanned device is " + currentDevice.toString());
            notifyConnectionStateChanged(BtState.DEVICE_FOUND, true);
        }
        return deviceIsFound;
    }

    private void startConnection(boolean useAudio){
        this.useAudio = useAudio;
        this.isConnectionInterrupted = false; // resetting the flag when starting a new connection

        connect(currentDevice, (MelomindDevice.isMelomindRequested()) ? //second step is BLE (Melomind) /SPP (Vpro) connection to the found device
                BLUETOOTH_LE : BLUETOOTH_SPP);
    }

    /**+**/
    private boolean isBluetoothDisabled(){
        boolean isBluetoothDisabled = false;
        if(!BluetoothAdapter.getDefaultAdapter().isEnabled() && !this.mbtBluetoothLE.enableBluetoothOnDevice()){ //check if Bluetooth is on
            notifyConnectionStateChanged(BtState.BLUETOOTH_DISABLED, true);
            isBluetoothDisabled = true;
        }
        return isBluetoothDisabled;
    }

    /**+**/
    private boolean isLocationDisabledOrNotGranted(){
        boolean isLocationDisabledOrNotGranted = false;
        if (MelomindDevice.isMelomindRequested()){
            //Checking location permission
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED
                    && ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){

                notifyConnectionStateChanged(BtState.LOCATION_PERMISSION_NOT_GRANTED, true);
                isLocationDisabledOrNotGranted = true;
            }

            //Checking location activation
            LocationManager manager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            if(manager != null && !manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)){
                notifyConnectionStateChanged(BtState.LOCATION_DISABLED, true);
                isLocationDisabledOrNotGranted = true;
            }
        }else //VPro todo
            notifyConnectionStateChanged(BtState.SCAN_FAILED,true);
        return isLocationDisabledOrNotGranted;
    }

    /**+**/
    private boolean isAlreadyConnected(String deviceName){
        boolean isAlreadyConnected = false;
        if(isConnected()){
            notifyConnectionStateChanged( isAlreadyConnectedToRequestedDevice(deviceName) ?
                    BtState.CONNECTED_AND_READY : BtState.ANOTHER_DEVICE_CONNECTED, true);
            isAlreadyConnected = true;
        }
        return isAlreadyConnected;
    }

    private boolean scan(String deviceName){
        Log.i(TAG," start scan "+deviceName);
        boolean deviceIsFound = true;
        try {
            futureScannedDevice = scanDeviceAsync(deviceName);
            Log.i(TAG," future scanned device "+futureScannedDevice.toString());
            currentDevice = futureScannedDevice.get(MbtConfig.getBluetoothScanTimeout(), TimeUnit.MILLISECONDS);
            Log.i(TAG," scanned device from future Name "+currentDevice.getName()+" | Address "+currentDevice.getAddress()+" | Bond state "+currentDevice.getBondState()+" | Type "+currentDevice.getType());
        } catch (@NonNull InterruptedException | ExecutionException | TimeoutException | CancellationException e) {
            LogUtils.i(TAG, "Exception raised during scanning : \n " + e.toString());
        }finally {
            stopCurrentScan();
        }
        if(currentDevice == null){
            notifyConnectionStateChanged((isConnectionInterrupted ? BtState.SCAN_INTERRUPTED : BtState.SCAN_TIMEOUT), true);
            deviceIsFound = false;
        }else {
            LogUtils.i(TAG, "scanned device is " + currentDevice.toString());
            notifyConnectionStateChanged(BtState.DEVICE_FOUND, true);
        }
        return deviceIsFound;
    }

    /**+**/private void bond(MbtDevice device){
        if(new FirmwareUtils(device.getFirmwareVersion()).isFwValidForFeature(FirmwareUtils.FWFeature.BLE_BONDING)){ //bonding is not supported for firmware versions older than 1.7.1 so we consider than the connection process is completed
            if(device.getBluetoothDevice().getType() == BluetoothDevice.DEVICE_TYPE_LE && device.getBluetoothDevice().getBondState() != BluetoothDevice.BOND_BONDED)
                notifyConnectionStateChanged(BtState.BONDING, true);
            //mbtBluetoothLE.triggerBonding();
            Log.i(TAG, "external " + device.getExternalName());
            device.setExternalName(MbtFeatures.MELOMIND_DEVICE_NAME); //todo remove
            if (device.getExternalName() != null && device.getExternalName().equals(MbtFeatures.MELOMIND_DEVICE_NAME) //send the QR code found in the database if the headset do not know its own QR code
                    && new FirmwareUtils(device.getFirmwareVersion()).isFwValidForFeature(FirmwareUtils.FWFeature.REGISTER_EXTERNAL_NAME)) {
                Log.i(TAG, "Headset do not know its number : sending external name to device with id " + device.getSerialNumber());
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (device.getSerialNumber() != null)
                    mbtBluetoothLE.sendExternalName(new MelomindsQRDataBase(mContext, true, false).get(device.getSerialNumber().replace(MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX,"")));
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mbtBluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY, true);

        if(isDownloadingFW)
            isDownloadingFW = false;
    }

    private void connectAudio(MbtDevice device){
        if(useAudio){
            connectA2DP(currentDevice, new FirmwareUtils(device.getFirmwareVersion()).isFwValidForFeature(FirmwareUtils.FWFeature.BLE_BONDING));
            Log.i(TAG, "Audio "+ (mbtBluetoothA2DP.isConnected() ? "is connected." : "has failed to connect."));
        }
    }

    /**
     * ConnectRequestEvent to a specific BluetoothDevice. This allows to skip the scanning part and jump directly to connection step.
     * The method returns immediately if a connection interruption has been sent by the user
     * @param device the Bluetooth device to connect to
     * @return immediately the following : false if device is null, true if connection step has been started
     */
    private void connect(@NonNull BluetoothDevice device, BtProtocol bluetoothProtocol){
        Log.i(TAG," connect device "+device+" protocol "+bluetoothProtocol);
        boolean isConnectionSucces = false;
        if(isConnectionInterrupted){
            notifyConnectionStateChanged(BtState.CONNECTION_INTERRUPTED, true);
            return;
        }

        switch (bluetoothProtocol){
            case BLUETOOTH_LE:
                isConnectionSucces = mbtBluetoothLE.connect(mContext, device);
                break;
            case BLUETOOTH_SPP:
                isConnectionSucces = mbtBluetoothSPP.connect(mContext, device);
                break;
            case BLUETOOTH_A2DP:
                isConnectionSucces = mbtBluetoothA2DP.connect(mContext, device); //never call if check on mbtfeatures.getbluetoothProtocol
                break;
        }
        Log.i(TAG," is connection success "+isConnectionSucces);
    }

    public void reconnectIfAudioConnected(String device){
        Log.i(TAG," reconnect if audio connected");
        if(mbtBluetoothA2DP.isConnected() && ++backgroundReconnectionRetryCounter < 0 )
            AsyncUtils.executeAsync(new Runnable() {
                @Override
                public void run() {
                    if (device != null)
                        connectBLEFromA2DP(device);
                }
            });
        else{
            backgroundReconnectionRetryCounter = 0; //reset counter
            mbtBluetoothLE.notifyConnectionStateChanged(BtState.DISCONNECTED, true);
        }
    }

    public void resetBackgroundReconnectionRetryCounter(){
        Log.i(TAG," reset background reconnection counter");
        backgroundReconnectionRetryCounter = 0;
    }

    /**
     * This method starts a bluetooth scan operation.
     * if the device to scan is a melomind, the bluetooth LE scanner is invoked.
     * <p>Requires {@link Manifest.permission#ACCESS_FINE_LOCATION} or {@link Manifest.permission#ACCESS_COARSE_LOCATION} permission
     * if a GPS sensor is available.
     *
     * If permissions are not given and/or bluetooth device is not Le compatible, discovery scan is started.
     */
    //Lister les input:
    // - Device Type / Bluetooth protocol
    void scanDevices(/*, MbtScanCallback scanCallback*/){
        //TODO choose method name accordingly between scan() / scanFor() / ...

        //Check device type from config.deviceType

        if (MelomindDevice.isMelomindRequested() && ContextCompat.checkSelfPermission(mContext,
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
    private Future<BluetoothDevice> scanDeviceAsync(@Nullable final String deviceName){ //todo check that
        Log.i(TAG," scan device async "+deviceName);
        return AsyncUtils.executeAsync(new Callable<BluetoothDevice>() {
            @Nullable
            @Override
            public BluetoothDevice call() {
                return (MbtFeatures.getBluetoothProtocol()== BLUETOOTH_LE) ?
                            mbtBluetoothLE.startLowEnergyScan(true, deviceName) : mbtBluetoothSPP.startScanDiscovery(deviceName);
            }
        });
    }


    /**
     * This method stops the currently running bluetooth scan, either Le scan or discovery scan
     */
    private void stopCurrentScan(){
        LogUtils.i(TAG, "stopping current scan");
        if (MelomindDevice.isMelomindRequested() && ContextCompat.checkSelfPermission(mContext,
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
    void configureHeadset(@NonNull DeviceConfig config){
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
    void startStream(boolean enableDeviceStatusMonitoring){
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
    void stopStream(){
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
    void readBattery() {
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
     * Initiates a read serial number operation on this correct BtProtocol
     * In case of failure during read process, an event with error is posted to the main manager.
     */
    private void readModelNumber(){
        if(!mbtBluetoothLE.readModelNumber()){
            requestBeingProcessed = false;
            EventBusManager.postEvent(new DeviceInfoEvent<>(DeviceInfo.MODEL_NUMBER, null));
        }
    }


    /**
     * Start the disconnect operation on the currently connected bluetooth device according to the {@link BtProtocol} currently used.
     */
    private void disconnect(BtProtocol protocol) {
        switch(protocol){
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
            //this.notifyConnectionStateChanged(BtState.SCAN_INTERRUPTED, true);
            if(futureScannedDevice != null)
                futureScannedDevice.cancel(false);
        }else if(getCurrentState() == BtState.CONNECTING || getCurrentState() == BtState.CONNECTED || getCurrentState() == BtState.DISCOVERING_SERVICES || getCurrentState() == BtState.READING_DEVICE_INFO || getCurrentState() == BtState.BONDING || getCurrentState() == BtState.SENDIND_QR_CODE || getCurrentState() == BtState.DISCONNECTED){
            this.notifyConnectionStateChanged(BtState.CONNECTION_INTERRUPTED, true);
            disconnect(MelomindDevice.isMelomindRequested() ? BtProtocol.BLUETOOTH_LE : BtProtocol.BLUETOOTH_SPP);
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
    void deinit(){
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
    public void notifyConnectionStateChanged(@NonNull BtState newState, boolean notifyUserClient) {
        Log.i(TAG, "new state = "+newState);
        if(newState == BtState.CONNECTED_AND_READY || newState == BtState.DISCONNECTED || newState == BtState.SCAN_TIMEOUT || newState == BtState.BLUETOOTH_DISABLED || newState == BtState.INTERNAL_FAILURE
                || newState == BtState.LOCATION_DISABLED || newState == BtState.LOCATION_PERMISSION_NOT_GRANTED || newState == BtState.SCAN_INTERRUPTED || newState == BtState.ANOTHER_DEVICE_CONNECTED){
            requestBeingProcessed = false;
        }
        //This event is sent to device module if registered
        if(newState == BtState.CONNECTED && currentDevice != null)
            EventBusManager.postEvent(new DeviceEvents.NewBluetoothDeviceEvent(currentDevice));
        else if (newState == BtState.DISCONNECTED || newState == BtState.CONNECTION_FAILURE || newState == BtState.CONNECTION_INTERRUPTED)
            EventBusManager.postEvent(new DeviceEvents.NewBluetoothDeviceEvent(currentDevice = null));

        //This event is sent to MbtManager for user notifications
        if(notifyUserClient)
            EventBusManager.postEvent(new NewConnectionStateEvent(newState, getConnectionError(newState)));
    }

    private BaseError getConnectionError(BtState newState) {
        BaseError error = null;
        switch (newState) {
            case NO_BLUETOOTH:
                error = BluetoothError.ERROR_NOT_SUPPORTED;
                break;
            case BLUETOOTH_DISABLED:
                error = MobileDeviceError.ERROR_BLUETOOTH_DISABLED;
                break;
            case LOCATION_DISABLED:
                error = MobileDeviceError.ERROR_GPS_DISABLED;
                break;
            case LOCATION_PERMISSION_NOT_GRANTED:
                error = MobileDeviceError.ERROR_LOCATION_PERMISSION;
                break;
            case SCAN_FAILED_ALREADY_STARTED:
                error = BluetoothError.ERROR_ALREADY_SCANNING;
                break;
            case SCAN_FAILED:
                error = BluetoothError.ERROR_SCANNING_FAILED;
                break;
            case ANOTHER_DEVICE_CONNECTED:
                error = BluetoothError.ERROR_ALREADY_CONNECTED_ANOTHER;
                break;
            case SCAN_TIMEOUT:
                error = BluetoothError.ERROR_SCANNING_TIMEOUT;
                break;
            case SCAN_INTERRUPTED:
                error = BluetoothError.ERROR_SCANNING_INTERRUPTED;
                break;
            case CONNECTION_INTERRUPTED:
                error = BluetoothError.ERROR_SCANNING_INTERRUPTED;
                break;
            case CONNECTION_FAILURE:
            case DISCOVERING_FAILURE:
            case READING_FAILURE:
            case BONDING_FAILURE:
                error = BluetoothError.ERROR_CONNECT_FAILED;
                break;
            case UPGRADE_FAILED:
                error = FirmwareError.ERROR_FIRMWARE_UPGRADE_FAILED;
                break;
        }
        return error;
    }


    /**
     * This method is called from Bluetooth classes and is meant to post an event to the main manager
     * that contains the {@link DeviceInfo} with the associated value
     * @param deviceInfo the {@link DeviceInfo}
     * @param deviceValue the new value as String
     */
    void notifyDeviceInfoReceived(DeviceInfo deviceInfo, String deviceValue){
        LogUtils.i(TAG, "post device info");
        requestBeingProcessed = false;
        EventBusManager.postEvent(new DeviceInfoEvent<String>(deviceInfo, deviceValue));
        if(getCurrentState().equals(BtState.READING_DEVICE_INFO)) {
            if(deviceInfo.equals(DeviceInfo.FW_VERSION)){
                mbtBluetoothLE.readHwVersion();
            }else if(deviceInfo.equals(DeviceInfo.HW_VERSION)){
                mbtBluetoothLE.readSerialNumber();
            }else if(deviceInfo.equals(DeviceInfo.SERIAL_NUMBER)){
                mbtBluetoothLE.readModelNumber();
            }else if(deviceInfo.equals(DeviceInfo.MODEL_NUMBER))
                finalizeConnectionProcess();
        }
    }

    /**+**/
    private void finalizeConnectionProcess(){ //Bonding and A2DP connection if user requested it on the connection config
        requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() { //get the currently connected headset
            @Override
            public void onRequestComplete(MbtDevice headsetDevice) {
                if(headsetDevice != null) {
                    bond(headsetDevice);
                    connectAudio(headsetDevice);
                }
            }
        });
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
        EventBusManager.postEvent(new DeviceEvents.RawDeviceMeasure(payload));
    }

    /**
     * This class is a specific thread that will handle all bluetooth operations. Bluetooth operations
     * are synchronous, meaning two or more operations can't be run simultaneously. This {@link HandlerThread}
     * extended class is able to hold pending operations.
     */
    class RequestThread extends HandlerThread {
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
         *
         * @param request the {@link BluetoothRequests} request to execute.
         */
        void parseRequest(BluetoothRequests request) {
            //BluetoothRequests request = pendingRequests.remove();

            //disconnect request doesn't need to be in "queue" as it is top priority

            while (requestBeingProcessed);
            LogUtils.i(TAG, "bluetooth execution thread is now free");
            requestBeingProcessed = true;
            if (request instanceof ScanAndConnectRequestEvent) {
                scanAndConnect(((ScanAndConnectRequestEvent) request).getName(), ((ScanAndConnectRequestEvent) request).connectAudioInA2DP());
            } else if (request instanceof ReadRequestEvent) {
                performReadOperation(((ReadRequestEvent) request).getDeviceInfo());
            } else if (request instanceof DisconnectRequestEvent) {
                if (((DisconnectRequestEvent) request).isInterrupted())
                    cancelPendingConnection();
                else
                    disconnect(MelomindDevice.isMelomindRequested() ? BtProtocol.BLUETOOTH_LE : BtProtocol.BLUETOOTH_SPP);
            } else if (request instanceof StreamRequestEvent) {
                if (((StreamRequestEvent) request).isStart())
                    startStream(((StreamRequestEvent) request).shouldMonitorDeviceStatus());
                else
                    stopStream();
            } else if (request instanceof UpdateConfigurationRequestEvent) {
                configureHeadset(((UpdateConfigurationRequestEvent) request).getConfig());
            }
        }

        /**
         * If the {@link BluetoothRequests request} is a {@link ReadRequestEvent} event, this method
         * is called to parse which read operation is to be executed according to the {@link DeviceInfo}.
         * @param deviceInfo the {@link DeviceInfo} info that determine which read to operation to execute.
         */
        void performReadOperation(DeviceInfo deviceInfo) {
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
                case MODEL_NUMBER:
                    readModelNumber();
                    break;
                default:
                    break;
            }
        }

        void followBluetoothProcess(BluetoothRequests request){
            switch (mbtBluetoothLE.getCurrentState()){
                case IDLE:
                    if(request instanceof ScanRequestEvent)
                        startScan(((ScanRequestEvent) request).getName());
                    break;
                case SCAN_STARTED:
                    break;
            }

        }
    }

    /**
     * Tells whether or not the end-user device is currently connected to the headset in Low Energy.
     *
     * @return <code>true</code> if connected, <code>false</code> otherwise
     */
    private boolean isBleConnected() {
        return mbtBluetoothLE.isConnected();
    }

    /**
     * Tells whether or not the end-user device is currently connected to the headset in A2DP.
     * @return <code>true</code> if connected, <code>false</code> otherwise
     */
    private boolean isAudioConnected() {
        return mbtBluetoothA2DP.isConnected();
    }

    /**
     * Tells whether or not the end-user device is currently connected to the headset both in Low Energy and A2DP.
     * @return <code>true</code> if connected, <code>false</code> otherwise
     */
    public final boolean isConnected() {
        return (useAudio ? (isBleConnected() && isAudioConnected()) : isBleConnected());
    }

    // Raise events
    void notifyStateChanged(final BtState newState, BtProtocol protocol) {
        if(protocol.equals(BtProtocol.BLUETOOTH_LE)) {
            this.mbtBluetoothLE.setCurrentState(newState);
            notifyConnectionStateChanged(newState, true);
        }else
            this.mbtBluetoothA2DP.setCurrentState(newState);
    }

    /**
     * Detects that a melomind is connected in A2DP, then starts a connection process to the BLE.
     */
    synchronized void connectBLEFromA2DP(@NonNull String deviceName) {
        Log.i(TAG, "connect BLE from a2dp ");

        if(deviceName == null)
            return;

        this.mbtBluetoothA2DP.notifyConnectionStateChanged(BtState.AUDIO_CONNECTED, false); //meaning one melomind is  already connected in A2DP.

        String bleName = (deviceName.startsWith(MbtFeatures.A2DP_DEVICE_NAME_PREFIX_LEGACY) || deviceName.startsWith(MbtFeatures.A2DP_DEVICE_NAME_PREFIX)) ?
                //audio_ prefix is replaced by a melo_ prefix
                deviceName.replace(MbtFeatures.A2DP_DEVICE_NAME_PREFIX, MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX) :
                //BLE name = melo_ with the QRcode digits
            MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX + new MelomindsQRDataBase(mContext, true, true).get(deviceName);


        Log.i(TAG, "associated BLE name is " + bleName);
        //Disconnecting another melomind if already one connected in BLE
        mbtBluetoothLE.disconnectHeadsetAlreadyConnected(deviceName, isBleConnected());

        scanAndConnect(deviceName, MbtConfig.connectAudioIfDeviceCompatible);
    }

    void requestCurrentConnectedDevice(final SimpleRequestCallback<MbtDevice> callback) {
        EventBusManager.postEventWithCallback(new DeviceEvents.GetDeviceEvent(), new EventBusManager.Callback<DeviceEvents.PostDeviceEvent>(){
            @Override
            @Subscribe
            public void onEventCallback(DeviceEvents.PostDeviceEvent device) {
                Log.i(TAG," on request complete");
                callback.onRequestComplete(device.getDevice());
            }
        });
    }

    /**
     * This methods aims at configuring the remote headset by adjusting a set of parameters.
     * Small sleeps have been added in the method in order to make sure gatt is free.
     */
    @WorkerThread
    private synchronized void connectA2DP(BluetoothDevice device, boolean connectFromBle){
        Log.i(TAG," connect A2dp");
        if(isBleConnected() && connectFromBle) { //A2DP cannot be connected from BLE if BLE connection state is not CONNECTED_AND_READY
             this.mbtBluetoothLE.connectA2DPFromBLE();
        }else if(device != null)
            this.connect(device, BLUETOOTH_A2DP);
    }

    boolean disconnectA2DPFromBLE() {
        Log.i(TAG, " disconnect A2dp");
        if(!isBleConnected())
            return false;
        return mbtBluetoothLE.disconnectA2DPFromBLE();
    }

    /**
     * Return true if the user has requested connection with an already connected device, false otherwise
     */
    private boolean isAlreadyConnectedToRequestedDevice(String deviceName){
        return (deviceName != null && currentDevice != null && currentDevice.getName().equals(deviceName));
    }

    public boolean isDownloadingFW() {
        return isDownloadingFW;
    }
}
