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
import core.bluetooth.requests.StartOrContinueConnectionRequestEvent;
import core.bluetooth.requests.DisconnectRequestEvent;
import core.bluetooth.requests.ReadRequestEvent;
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
import engine.clientevents.ConnectionStateReceiver;
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
    private boolean isDownloadingFW = false;

    private Future futureScannedDevice;
    private Future futureBondResult;

    //private MbtDeviceAcquisition deviceAcquisition;

    private int backgroundReconnectionRetryCounter = 0;

    private ConnectionStateReceiver receiver = new ConnectionStateReceiver() {
        @Override
        public void onError(BaseError error, String additionnalInfo) {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null){
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, " received intent " + action + " for device " + (device != null ? device.getName() : null));
                if(MbtConfig.connectAudioIfDeviceCompatible() && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
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

        BroadcastUtils.registerReceiverIntents(context, new ArrayList<>(Arrays.asList(
                BluetoothAdapter.ACTION_STATE_CHANGED)),
                receiver);
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
            if (request instanceof StartOrContinueConnectionRequestEvent) {
                startOrContinueConnectionOperation();
            } else if (request instanceof ReadRequestEvent) {
                startReadOperation(((ReadRequestEvent) request).getDeviceInfo());
            } else if (request instanceof DisconnectRequestEvent) {
                if (((DisconnectRequestEvent) request).isInterrupted())
                    cancelPendingConnection();
                else
                    disconnect(MbtConfig.isCurrentDeviceAMelomind() ? BtProtocol.BLUETOOTH_LE : BtProtocol.BLUETOOTH_SPP); //todo disconnect audio
            } else if (request instanceof StreamRequestEvent) {
                if (((StreamRequestEvent) request).isStart())
                    startStreamOperation(((StreamRequestEvent) request).shouldMonitorDeviceStatus());
                else
                    stopStreamOperation();
            } else if (request instanceof UpdateConfigurationRequestEvent) {
                configureHeadset(((UpdateConfigurationRequestEvent) request).getConfig());
            }
        }


        /**
         * This method do the following operations:
         * - 1) Check the prerequisites to ensure that the connection can be performed
         * - 2) Scan for {@link BluetoothDevice } filtering on deviceName. The scan is performed by LE scanner if the device is LE compatible. Otherwise, the discovery scan is performed instead.
         * - 3) Perform the BLE/SPP connection operation if scan resulted in a found device
         * - 4) Discovering services once the headset is connected.
         * - 5) Reading the device info (firmware version, hardware version, serial number, model number) once the services has been discovered
         * - 6) Bond the headset if the firmware version supports it (version > 1.6.7)
         * - 7) Send the QR code number to the headset if it doesn't know its own value and if the firmware version supports it (version > 1.7.1)
         */
        void startOrContinueConnectionOperation(){
            switch (getCurrentState()){
                case IDLE:
                    getReadyForBluetoothOperation();
                    break;
                case READY_FOR_BLUETOOTH_OPERATION:
                    startScan();
                    break;
                case DEVICE_FOUND:
                    startConnectionForDataStreaming();
                    break;
                case CONNECTION_SUCCESS:
                    startDiscoveringServices();
                    break;
                case DISCOVERING_SUCCESS:
                    updateConnectionState(); //current state is set to READING_FW_VERSION
                    switchToNextConnectionStep();
                    break;
                case READING_FIRMWARE_VERSION: //should not arrive here as there is no READING SUCCESS STATE, we consider that the READING_HARDWARE_VERSION state is launched only if the Firmware reading operation is a success
                    startReadOperation(DeviceInfo.FW_VERSION); // read all device info (except battery) according to enum order : first device info to read is firmware
                    break;
                case READING_HARDWARE_VERSION: //equivalent to READING_FIRMWARE_VERSION_SUCCESS
                    startReadOperation(DeviceInfo.HW_VERSION); // read all device info (except battery) according to enum order : first firmware , second hardware, third serial number then model number
                    break;
                case READING_SERIAL_NUMBER: //equivalent to READING_HARDWARE_VERSION_SUCCESS
                    startReadOperation(DeviceInfo.SERIAL_NUMBER); // read all device info (except battery) according to enum order : first firmware , second hardware, third serial number then model number
                    break;
                case READING_MODEL_NUMBER: //equivalent to READING_SERIAL_NUMBER_SUCCESS
                    startReadOperation(DeviceInfo.MODEL_NUMBER); // read all device info (except battery) according to enum order : first firmware , second hardware, third serial number then model number
                    break;
                case READING_SUCCESS: //equivalent to READING_MODEL_NUMBER_SUCCESS
                    startBonding();
                    break;
                case BONDED:
                    sendExternalName();
                    break;
                case QR_CODE_SENT:
                    updateConnectionState(); // current state is set to CONNECTED_AND_READY as the last step of the connection step has been reached
                    switchToNextConnectionStep();
                    break;
                case CONNECTED_AND_READY:
                    startConnectionForAudioStreaming();
                    break;
            }
        }
    }

    private void switchToNextConnectionStep(){
        requestBeingProcessed = false;
        EventBusManager.postEvent(new StartOrContinueConnectionRequestEvent());
    }

    /**
     * As headset transmits its sensed data to the SDK using Bluetooth,
     * the mobile device Bluetooth must be currently enabled and ready for use.
     * The Bluetooth Manager must always check this prerequisite before starting any connection operation by calling the isBluetoothDisabled() method.
     * @return false is Bluetooth is ON /enabled,
     * or true is Bluetooth is OFF / disabled.
     **/
    private boolean isBluetoothDisabled(){
        boolean isBluetoothDisabled = false;
        if(!BluetoothAdapter.getDefaultAdapter().isEnabled() && !this.mbtBluetoothLE.enableBluetoothOnDevice()){
            updateConnectionState(BtState.BLUETOOTH_DISABLED);
            isBluetoothDisabled = true;
        }
        return isBluetoothDisabled;
    }

    /**
     * As the Bluetooth scan requires access to the mobile device Location,
     * the Bluetooth Manager must always check this prerequisite before starting any connection operation by calling the isLocationDisabledOrNotGranted() method.
     * @return false is Location is ON /enabled and Location permission is granted,
     * or true is Location is OFF / disabled, and/or Location permission is not granted.
     **/
    private boolean isLocationDisabledOrNotGranted(){
        boolean isLocationDisabledOrNotGranted = false;
        //Checking location permission
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED
                && ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){

            updateConnectionState(BtState.LOCATION_PERMISSION_NOT_GRANTED);
            isLocationDisabledOrNotGranted = true;
        }

        //Checking location activation
        LocationManager manager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if(manager != null && !manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)){
            updateConnectionState(BtState.LOCATION_DISABLED);
            isLocationDisabledOrNotGranted = true;
        }
        return isLocationDisabledOrNotGranted;
    }

    /**
     * As the Bluetooth scan requires access to the mobile device Location,
     * the Bluetooth Manager must always check this prerequisite before starting any connection operation by calling the isLocationDisabledOrNotGranted() method.
     * @return true if a headset is already connected in BLE and Audio (if Audio has been enabled)
     * or if no headset is connected.
     **/
    private boolean isAlreadyConnected(MbtDevice currentConnectedDevice){
        boolean isAlreadyConnected = false;
        if(isConnected()){
            if(!isAlreadyConnectedToRequestedDevice(MbtConfig.getNameOfDeviceRequested(), currentConnectedDevice)) {
                updateConnectionState(BtState.ANOTHER_DEVICE_CONNECTED);
            }else
                updateConnectionState(BtState.CONNECTED_AND_READY);
            isAlreadyConnected = true;
        }
        return isAlreadyConnected;
    }
    /**
     * Check the bluetooth prerequisites before starting any bluetooth operation.
     *
     */
    private void getReadyForBluetoothOperation(){
        //Request sent to the BUS in order to get device from the device manager : the BUS should return a null object if it's the first connection, or return a non null object if the user requests connection whereas a headset is already connected
        requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
            @Override
            public void onRequestComplete(MbtDevice device) { //when the BUS has returned the device object
                boolean isReadyForBluetoothAction = true;

                if(device != null && isAlreadyConnected(device)) // assert that headset is not already connected
                    isReadyForBluetoothAction = false;

                if(isBluetoothDisabled()) //assert that Bluetooth is on
                    isReadyForBluetoothAction = false;

                if(!isLocationDisabledOrNotGranted()) //assert that Location is on and Location permissions are granted
                    isReadyForBluetoothAction = false;

                if(isReadyForBluetoothAction && getCurrentState().equals(BtState.IDLE))
                    updateConnectionState(); //current state is set to READY_FOR_BLUETOOTH_OPERATION
                switchToNextConnectionStep();
            }
        });
    }

    /**
     * This method starts a bluetooth scan operation.
     * if the device to scan is a melomind, the bluetooth LE scanner is invoked. Otherwise, the classic discovery scan is invoked.
     * <p>Requires {@link Manifest.permission#ACCESS_FINE_LOCATION} or {@link Manifest.permission#ACCESS_COARSE_LOCATION} permission
     * if a GPS sensor is available.
     *
     * If permissions are not given and/or bluetooth device is not Le compatible, discovery scan is started.
     */
    private void startScan(){
        try {
            futureScannedDevice = startScanAsync();
            futureScannedDevice.get(MbtConfig.getBluetoothScanTimeout(), TimeUnit.MILLISECONDS);
        } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
            updateConnectionState((e instanceof TimeoutException ) ?
                    BtState.SCAN_TIMEOUT : BtState.SCAN_FAILURE);
            LogUtils.i(TAG, "Exception raised during scanning : \n " + e.toString());
        } finally {
            stopCurrentScan();
        }
        switchToNextConnectionStep();
    }

    /**
     * Start scanning a single device by filtering on its name. This method is asynchronous
     * @return a {@link Future} object holding the {@link BluetoothDevice} instance of the device to scan.
     */
    private Future startScanAsync() {
        Log.i(TAG, " scan device async " + MbtConfig.getNameOfDeviceRequested());
        return AsyncUtils.executeAsync(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return (MbtFeatures.useLowEnergyProtocol()) ?
                        mbtBluetoothLE.startLowEnergyScan(true): mbtBluetoothSPP.startScanDiscovery();
            }
        });
    }

    /**
     * This method stops the currently running bluetooth scan, either Le scan or discovery scan
     */
    private void stopCurrentScan(){
        LogUtils.i(TAG, "stopping current scan");
        if (MbtConfig.isCurrentDeviceAMelomind() && ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mbtBluetoothLE.stopLowEnergyScan(); //TODO handle this
        else
            mbtBluetoothLE.stopScanDiscovery();
    }

    /**
     * This connection step is the BLE (Melomind) /SPP (Vpro) connection to the found device
     * It allows communication between the headset device and the SDK for data streaming (EEG, battery level, etc.)
     **/
    private void startConnectionForDataStreaming(){
        connect(MelomindDevice.isMelomindRequested() ? BLUETOOTH_LE : BLUETOOTH_SPP);
    }

    private void connect(BtProtocol protocol){
        Log.i(TAG,"device to connect Name: "+getCurrentScannedDevice().getName()+ " | address: ");
        boolean isConnectionSucces = false;
        this.isConnectionInterrupted = false; // resetting the flag when starting a new connection
        switch (protocol){
            case BLUETOOTH_LE:
                isConnectionSucces = mbtBluetoothLE.connect(mContext, getCurrentScannedDevice());
                break;
            case BLUETOOTH_SPP:
                isConnectionSucces = mbtBluetoothSPP.connect(mContext, getCurrentScannedDevice());
                break;
            case BLUETOOTH_A2DP:
                isConnectionSucces = mbtBluetoothA2DP.connect(mContext, getCurrentScannedDevice());
                break;
        }
        Log.i(TAG," is connection success "+isConnectionSucces);
    }

    /**
     * Once a device is connected in Bluetooth Low Energy / SPP for data streaming, we consider that the Bluetooth connection process is not fully completed.
     * The services offered by a remote device as well as their characteristics and descriptors are discovered to ensure that Data Streaming can be performed.
     * It means that the Bluetooth Manager retrieve all the services, which can be seen as categories of data that the headset is transmitting
     * This is an asynchronous operation.
     * Once service discovery is completed, the BluetoothGattCallback.onServicesDiscovered callback is triggered.
     * If the discovery was successful, the remote services can be retrieved using the getServices function
     */
    private void startDiscoveringServices(){
        if(getCurrentState().ordinal() >= BtState.CONNECTION_SUCCESS.ordinal()){ //if connection is in progress and BLE is at least connected, we can discover services
            Future futureDiscoveredServices = AsyncUtils.executeAsync(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    Boolean servicesDiscovered = MbtBluetoothManager.this.mbtBluetoothLE.discoverServices();
                    if(!servicesDiscovered)
                        updateConnectionState(BtState.DISCOVERING_FAILURE);
                    else if(getCurrentState().equals(BtState.CONNECTION_SUCCESS))
                        updateConnectionState(); //current state is set to DISCOVERING_SERVICES
                    return servicesDiscovered;
                }
            });
            try {
                if (futureDiscoveredServices != null)
                    futureDiscoveredServices.get(MbtConfig.getBluetoothDiscoverTimeout(), TimeUnit.MILLISECONDS);

            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
            switchToNextConnectionStep();
        }else
            updateConnectionState(BtState.DISCOVERING_FAILURE);
    }

    /**
     * If the {@link BluetoothRequests request} is a {@link ReadRequestEvent} event, this method
     * is called to parse which read operation is to be executed according to the {@link DeviceInfo}.
     * @param deviceInfo the {@link DeviceInfo} info that determine which read to operation to execute.
     */
    public void startReadOperation(DeviceInfo deviceInfo) {
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

    private void startBonding() {
        requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
            @Override
            public void onRequestComplete(MbtDevice device) { //Firmware version has been read during the previous step so we retrieve its value, as it has been stored in the Device Manager
                boolean isBondingSupported = new FirmwareUtils(device.getFirmwareVersion()).isFwValidForFeature(FirmwareUtils.FWFeature.BLE_BONDING);
                if(isBondingSupported) { //if firmware version bonding is higher than 1.6.7, the bonding is launched
                    try {
                        futureBondResult = startBondAsync(device);
                        futureBondResult.get(MbtConfig.getBluetoothBondingTimeout(), TimeUnit.MILLISECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        updateConnectionState(BtState.BONDING_FAILURE);
                    } finally {
                        stopBonding();
                    }
                }else //if firmware version bonding is older than 1.6.7, the connection process is considered completed
                    updateConnectionState(BtState.CONNECTED_AND_READY);
            }
        });
        if(getCurrentState().equals(BtState.BONDING))
            updateConnectionState();
        switchToNextConnectionStep();
    }

    private Future startBondAsync(MbtDevice device) {
        return AsyncUtils.submitAsync(new Runnable() {
            @Override
            public void run() {
                mbtBluetoothLE.bond(device);
            }
        });
    }

    private void stopBonding() {
        mbtBluetoothLE.stopBonding();
    }

    private void sendExternalName() {
        requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
            @Override
            public void onRequestComplete(MbtDevice device) {
                if (device.getExternalName() != null && device.getExternalName().equals(MbtFeatures.MELOMIND_DEVICE_NAME) //send the QR code found in the database if the headset do not know its own QR code
                        && new FirmwareUtils(device.getFirmwareVersion()).isFwValidForFeature(FirmwareUtils.FWFeature.REGISTER_EXTERNAL_NAME)) {
                    Log.i(TAG, "Headset do not know its number : sending external name to device with id " + device.getDeviceId());
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (device.getDeviceId() != null)
                        mbtBluetoothLE.sendExternalName(new MelomindsQRDataBase(mContext, true, false).get(device.getDeviceId().replace(MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX, "")));
                }
            }
        });
        switchToNextConnectionStep();
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

    private void startConnectionForAudioStreaming(){
        if(MbtConfig.connectAudioIfDeviceCompatible()) {
            requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
                @Override
                public void onRequestComplete(MbtDevice device) {
                    boolean connectionFromBleAvailable = new FirmwareUtils(device.getFirmwareVersion()).isFwValidForFeature(FirmwareUtils.FWFeature.BLE_BONDING);
                    if(isBleConnected() && connectionFromBleAvailable) { //A2DP cannot be connected from BLE if BLE connection state is not CONNECTED_AND_READY
                        mbtBluetoothLE.connectA2DPFromBLE();
                    }else // if BLE is not connected or the firmware version doesn't support connection from BLE
                        connect(BLUETOOTH_A2DP);
                    Log.i(TAG, "Audio " + (mbtBluetoothA2DP.isConnected() ? "is connected." : "has failed to connect."));

                }
            });
        }
        requestBeingProcessed = false;
    }

    /**
     * If BLE connection is lost and Audio is still connected, the Bluetooth manager try to reconnect it
     * The method returns immediately if a connection interruption has been sent by the user
     * @param deviceName the name of the Bluetooth device to connect to
     * @return immediately the following : false if device is null, true if connection step has been started
     */


    public void reconnectIfAudioConnected(String deviceName){
        Log.i(TAG," reconnect Bluetooth Low Energy if audio is connected");
        if(deviceName == null)
            return;
        if(mbtBluetoothA2DP.isConnected() && ++backgroundReconnectionRetryCounter < 0 )
            AsyncUtils.executeAsync(new Runnable() {
                @Override
                public void run() {
                    connectBLEFromA2DP(deviceName);
                }
            });
        else{
            backgroundReconnectionRetryCounter = 0; //reset counter
        }
    }

    void resetBackgroundReconnectionRetryCounter(){
        Log.i(TAG," reset background reconnection counter");
        backgroundReconnectionRetryCounter = 0;
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
    void startStreamOperation(boolean enableDeviceStatusMonitoring){
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
    void stopStreamOperation(){
        if(!mbtBluetoothLE.isStreaming())
            return;

        if(!mbtBluetoothLE.stopStream()){
            requestBeingProcessed  = false;
            EventBusManager.postEvent(IStreamable.StreamState.FAILED);
        }
    }

    /**
     * Start the disconnect operation on the currently connected bluetooth device according to the {@link BtProtocol} currently used.
     */
    private void disconnect(BtProtocol protocol) {
        switch(protocol){
            case BLUETOOTH_LE:
                if(isConnected())
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
        if(getCurrentState().equals(BtState.SCAN_STARTED)){
            if(futureScannedDevice != null && !futureScannedDevice.isDone()) //if scanning is still in progress
                futureScannedDevice.cancel(false);
        }else if(getCurrentState().equals(BtState.CONNECTING) || getCurrentState().equals(BtState.CONNECTION_SUCCESS) || getCurrentState().equals(BtState.DISCOVERING_SERVICES) || getCurrentState().isReadingDeviceInfoState() || getCurrentState().equals(BtState.BONDING) || getCurrentState().equals(BtState.SENDIND_QR_CODE) || getCurrentState().equals(BtState.DISCONNECTED)){
            updateConnectionState(BtState.CONNECTION_INTERRUPTED);
            disconnect(MbtConfig.isCurrentDeviceAMelomind() ? BtProtocol.BLUETOOTH_LE : BtProtocol.BLUETOOTH_SPP);
        }
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
     * This method is called from Bluetooth classes and is meant to post an event to the main manager
     * that contains the new {@link BtState}
     * @param newState the new {@link BtState}
     */
    public void notifyConnectionStateChanged(@NonNull BtState newState) {
        Log.i(TAG, "new state = "+newState);
        switch (newState){ //This event is sent to device module if registered
            case AUDIO_CONNECTED:
                mbtBluetoothA2DP.notifyConnectionStateChanged(newState,false);
                break;
                case DEVICE_FOUND:
                EventBusManager.postEvent(new DeviceEvents.NewBluetoothDeviceScannedEvent(getCurrentScannedDevice()));
                break;
            case SCAN_TIMEOUT:
            case SCAN_FAILURE:
            case SCAN_INTERRUPTED:
            case DISCONNECTED:
            case CONNECTION_FAILURE:
            case CONNECTION_INTERRUPTED:
                EventBusManager.postEvent(new DeviceEvents.NewBluetoothDeviceConnectedEvent(null));
                break;
        }
        EventBusManager.postEvent(new NewConnectionStateEvent(newState, newState.getAssociatedError()));//This event is sent to MbtManager for user notifications
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

        if(getCurrentState().isReadingDeviceInfoState() || getCurrentState().equals(BtState.BONDING)){
            updateConnectionState(); //current state is set to READING_HW_VERSION or READING_SERIAL_NUMBER or READING_MODEL_NUMBER or READING_SUCCESS if reading device info, or BONDED if bonding was in progress
            switchToNextConnectionStep(); //todo check is switch is called when received or at the end of startReadOperation method
        }
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
     * Set the current bluetooth connection state to the value given in parameter
     * and notify the bluetooth manager of this change.
     * This method should be called if something went wrong during the connection process, as it stops the connection prccess.
     * The updateConnectionState() method with no parameter should be call if nothing went wrong and user wants to continue the connection process
     */
    private void updateConnectionState(BtState state){
        if(MbtConfig.isCurrentDeviceAMelomind())
            mbtBluetoothLE.notifyConnectionStateChanged(state);
        else
            mbtBluetoothSPP.notifyConnectionStateChanged(state);
    }
    /**
     * Set the current bluetooth connection state to the value of the next step in chronological order (according to enum order)
     * and notify the bluetooth manager of this change.
     * This method should be called if no error occured.
     */
    public void updateConnectionState(){
        updateConnectionState(getCurrentState().getNextConnectionStep());
    }

    /**
     * Return true if the user has requested connection with an already connected device, false otherwise
     */
    private boolean isAlreadyConnectedToRequestedDevice(String nameDeviceToConnect, MbtDevice deviceConnected){
        return (nameDeviceToConnect != null && deviceConnected != null && deviceConnected.getBluetoothDevice().getName().equals(nameDeviceToConnect));
    }

    public boolean isDownloadingFW() {
        return isDownloadingFW;
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
        return (MbtConfig.connectAudioIfDeviceCompatible() ? (isBleConnected() && isAudioConnected()) : isBleConnected());
    }

    /**
     * Gets current state according to bluetooth protocol value
     * @return
     */
    private BtState getCurrentState(){
        return MbtFeatures.useLowEnergyProtocol() ? mbtBluetoothLE.currentState : mbtBluetoothSPP.currentState;
    }

    private BluetoothDevice getCurrentScannedDevice(){
        return (MbtFeatures.useLowEnergyProtocol()) ? mbtBluetoothLE.scannedDevice : mbtBluetoothSPP.scannedDevice;
    }

//    boolean disconnectA2DPFromBLE() {
//        Log.i(TAG, " disconnect A2dp");
//        if(!isBleConnected())
//            return false;
//        return mbtBluetoothLE.disconnectA2DPFromBLE();
//    }

    /**
     * Starts a Low Energy connection process if a Melomind is connected for Audio Streaming in A2DP.
     */
    synchronized void connectBLEFromA2DP(@NonNull String deviceName) {
        Log.i(TAG, "connect BLE from a2dp ");
        if(mbtBluetoothA2DP.isConnected()){
            String bleName = (deviceName.startsWith(MbtFeatures.A2DP_DEVICE_NAME_PREFIX_LEGACY) || deviceName.startsWith(MbtFeatures.A2DP_DEVICE_NAME_PREFIX)) ?
                    deviceName.replace(MbtFeatures.A2DP_DEVICE_NAME_PREFIX, MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX) : //audio_ prefix is replaced by a melo_ prefix
                    MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX + new MelomindsQRDataBase(mContext, true, true).get(deviceName);//BLE name = melo_ with the QRcode digits
            Log.i(TAG, "associated BLE name is " + bleName);
            mbtBluetoothLE.disconnectHeadsetAlreadyConnected(deviceName); //Disconnect if another melomind if already connected in BLE
            //todo manage connect by sending ConnectRequest
        }
    }
}
