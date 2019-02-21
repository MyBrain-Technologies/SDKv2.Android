package core.bluetooth;

import android.Manifest;
import android.annotation.TargetApi;
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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
import core.device.model.MelomindsQRDataBase;
import engine.SimpleRequestCallback;
import engine.clientevents.BaseError;
import engine.clientevents.ConnectionStateReceiver;
import eventbus.EventBusManager;
import eventbus.events.BluetoothEEGEvent;
import eventbus.events.ConnectionStateEvent;
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

@TargetApi(Build.VERSION_CODES.N)
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
    private boolean isRequestCompleted = false;

    private CompletableFuture<Boolean> futureOperation;
    private CompletableFuture<Boolean> futureSwitchOperation;

    private String deviceNameRequested;

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

        BroadcastUtils.registerReceiverIntents(context, receiver, BluetoothAdapter.ACTION_STATE_CHANGED);
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
            requestBeingProcessed = true;
            if (request instanceof StartOrContinueConnectionRequestEvent) {
                deviceNameRequested = ((StartOrContinueConnectionRequestEvent) request).getNameOfDeviceRequested();
                startOrContinueConnectionOperation(((StartOrContinueConnectionRequestEvent) request).isClientUserRequest());
            } else if (request instanceof ReadRequestEvent) {
                startReadOperation(((ReadRequestEvent) request).getDeviceInfo());
            } else if (request instanceof DisconnectRequestEvent) {
                if (((DisconnectRequestEvent) request).isInterrupted())
                    cancelPendingConnection(((DisconnectRequestEvent) request).isInterrupted());
                else {
                    if(isAudioBluetoothConnected())
                        disconnect(BtProtocol.BLUETOOTH_A2DP);
                    disconnect(MbtConfig.isCurrentDeviceAMelomind() ? BtProtocol.BLUETOOTH_LE : BtProtocol.BLUETOOTH_SPP);
                }
            } else if (request instanceof StreamRequestEvent) {
                if (((StreamRequestEvent) request).isStart())
                    startStreamOperation(((StreamRequestEvent) request).shouldMonitorDeviceStatus());
                else
                    stopStreamOperation();
            } else if (request instanceof UpdateConfigurationRequestEvent) {
                configureHeadset(((UpdateConfigurationRequestEvent) request).getConfig());
            }
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
     * - 8) Connect audio in A2dp is the user requested it
     */
    private void startOrContinueConnectionOperation(boolean isClientUserRequest){
        LogUtils.i(TAG, " Connection Operation continue with state: "+getCurrentState());
        if(isClientUserRequest)
            isConnectionInterrupted = false;
        if(!isConnectionInterrupted){
            switch (getCurrentState()){
                case IDLE:
                    getReadyForBluetoothOperation();
                    break;
                case READY_FOR_BLUETOOTH_OPERATION:
                    startScan();
                    break;
                case DEVICE_FOUND:
                case CONNECTING:
                    startConnectionForDataStreaming();
                    break;
                case CONNECTION_SUCCESS:
                    startDiscoveringServices();
                    break;
                case DISCOVERING_SUCCESS:
                    startReadingDeviceInfo(DeviceInfo.FW_VERSION); // read all device info (except battery) : first device info to read is firmware version
                    break;
                case READING_FIRMWARE_VERSION_SUCCESS:
                    startReadingDeviceInfo(DeviceInfo.HW_VERSION); // read next device info : second device info to read is hardware version
                    break;
                case READING_HARDWARE_VERSION_SUCCESS:
                    startReadingDeviceInfo(DeviceInfo.SERIAL_NUMBER); // read next device info : third device info to read is serial number (device ID)
                    break;
                case READING_SERIAL_NUMBER_SUCCESS:
                    startReadingDeviceInfo(DeviceInfo.MODEL_NUMBER); // read next device info : fourth device info to read is model number
                    break;
                case READING_SUCCESS: //equivalent to READING_MODEL_NUMBER_SUCCESS
                    startBonding();
                    break;
                case BONDED:
                    startSendingExternalName();
                    break;
                case CONNECTED_AND_READY:
                    startConnectionForAudioStreaming();
                    break;
                default:
                    requestBeingProcessed = false;
            }
        }else
            requestBeingProcessed = false;
    }

    private void switchToNextConnectionStep(){
        requestBeingProcessed = false;
        if(!getCurrentState().isAFailureState() && !isConnectionInterrupted && !getCurrentState().equals(BtState.IDLE))  //if nothing went wrong during the current step of the connection process, we continue the process
            EventBusManager.postEvent(new StartOrContinueConnectionRequestEvent(false, deviceNameRequested));
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
        if(!BluetoothAdapter.getDefaultAdapter().isEnabled()){
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
            if(!isAlreadyConnectedToRequestedDevice(deviceNameRequested, currentConnectedDevice)) {
                updateConnectionState(BtState.ANOTHER_DEVICE_CONNECTED);
            }else
                updateConnectionState(BtState.CONNECTED_AND_READY);
            isAlreadyConnected = true;
        }
        return isAlreadyConnected;
    }
    /**
     * Check the bluetooth prerequisites before starting any bluetooth operation.
     * The started Bluetooth connection process is stopped if the prerequisites are not valid.
     */
    private void getReadyForBluetoothOperation(){
        //Request sent to the BUS in order to get device from the device manager : the BUS should return a null object if it's the first connection, or return a non null object if the user requests connection whereas a headset is already connected
        LogUtils.i(TAG, "Checking Bluetooth Prerequisites and initialize");
        requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
            @Override
            public void onRequestComplete(MbtDevice device) { //when the BUS has returned the device object
                if(device != null && isAlreadyConnected(device)) // assert that headset is not already connected
                    return;

                if(isBluetoothDisabled()) //assert that Bluetooth is on
                    return;

                if(isLocationDisabledOrNotGranted()) //assert that Location is on and Location permissions are granted
                    return;

                if(MbtConfig.connectAudioIfDeviceCompatible()) //if user requested audio connection
                    mbtBluetoothA2DP.initA2dpProxy(); //initialization to check if a Melomind is already connected in A2DP : as the next step is the scanning, the SDK is able to filter on the name of this device

                if(getCurrentState().equals(BtState.IDLE))
                    updateConnectionState(false); //current state is set to READY_FOR_BLUETOOTH_OPERATION
                switchToNextConnectionStep();
            }
        });
    }

    /**
     * This method starts a bluetooth scan operation, loooking for a single device by filtering on its name.
     * This method is asynchronous.
     * If the device to scan is a Melomind, the bluetooth LE scanner is invoked.
     * Otherwise, the classic discovery scan is invoked.
     * <p>Requires {@link Manifest.permission#ACCESS_FINE_LOCATION} or {@link Manifest.permission#ACCESS_COARSE_LOCATION} permission if a GPS sensor is available.
     * If permissions are not given and/or bluetooth device is not Le compatible, discovery scan is started.
     * The started Bluetooth connection process is stopped if the prerequisites are not valid.
     */
    private void startScan(){
        LogUtils.i(TAG, "start scan");
        BtState newState = BtState.SCAN_FAILURE;
        try {
            AsyncUtils.executeAsync(new Runnable() {
                @Override
                public void run() {
                    if ((MbtFeatures.useLowEnergyProtocol()))
                        mbtBluetoothLE.startLowEnergyScan(true);
                    else
                        mbtBluetoothSPP.startScanDiscovery();
                }
            });
            futureOperation = new CompletableFuture<>();
            futureOperation.get(MbtConfig.getBluetoothScanTimeout(), TimeUnit.MILLISECONDS);
        } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
            if(e instanceof TimeoutException )
                newState = BtState.SCAN_TIMEOUT ; //stop the current Bluetooth connection process
            else if(e instanceof CancellationException )
                futureOperation = null;
            LogUtils.i(TAG, "Exception raised during scanning : \n " + e.toString());
        } finally {
            stopScan();
        }
        if(getCurrentState().equals(BtState.SCAN_STARTED)) ////at this point : current state should be DEVICE_FOUND if scan succeeded
            updateConnectionState(newState); //scan failure or timeout
        switchToNextConnectionStep();
    }

    /**
     * This method stops the currently running bluetooth scan, either Le scan or discovery scan
     */
    private void stopScan(){
        LogUtils.i(TAG, "stopping current scan");
        stopFutureOperation(true);
        if (MbtFeatures.useLowEnergyProtocol() && ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mbtBluetoothLE.stopLowEnergyScan();
        else
            mbtBluetoothLE.stopScanDiscovery();
    }

    /**
     * This connection step is the BLE (Melomind) /SPP (Vpro) connection to the found device
     * It allows communication between the headset device and the SDK for data streaming (EEG, battery level, etc.)
     **/
    private void startConnectionForDataStreaming(){
        LogUtils.i(TAG, "start connection data streaming");
        try {
            AsyncUtils.executeAsync(new Runnable() {
                @Override
                public void run() {
                    connect(MbtFeatures.useLowEnergyProtocol() ? BLUETOOTH_LE : BLUETOOTH_SPP);
                }
            });
            futureOperation = new CompletableFuture<>();
            futureOperation.get(MbtConfig.getBluetoothConnectionTimeout(), TimeUnit.MILLISECONDS); // blocked until the futureOperation.complete() is called or until timeout
        } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
            LogUtils.i(TAG, "Exception raised during connection : \n " + e.toString());
            if(e instanceof CancellationException )
                futureOperation = null;
        }finally {
            stopFutureOperation(true);
        }
        if(!getCurrentState().equals(BtState.CONNECTION_SUCCESS) && !getCurrentState().equals(BtState.IDLE))
            updateConnectionState(BtState.CONNECTION_FAILURE);
        switchToNextConnectionStep();
    }

    private void connect(BtProtocol protocol){
        boolean isConnectionSuccessful = false;
        this.isConnectionInterrupted = false; // resetting the flag when starting a new connection
        switch (protocol){
            case BLUETOOTH_LE:
                isConnectionSuccessful = mbtBluetoothLE.connect(mContext, getCurrentScannedDevice());
                break;
            case BLUETOOTH_SPP:
                isConnectionSuccessful = mbtBluetoothSPP.connect(mContext, getCurrentScannedDevice());
                break;
            case BLUETOOTH_A2DP:
                isConnectionSuccessful = mbtBluetoothA2DP.connect(mContext, getCurrentScannedDevice());
                break;
        }
        LogUtils.i(TAG,"updateConnectionState "+(protocol.equals(BLUETOOTH_A2DP) ? mbtBluetoothA2DP.getCurrentState() : getCurrentState()));
        if(isConnectionSuccessful) {
            updateConnectionState((protocol.equals(BLUETOOTH_A2DP) && isAudioBluetoothConnected()) || (!protocol.equals(BLUETOOTH_A2DP) && isDataBluetoothConnected()));
        }
    }

    /**
     * Once a device is connected in Bluetooth Low Energy / SPP for data streaming, we consider that the Bluetooth connection process is not fully completed.
     * The services offered by a remote device as well as their characteristics and descriptors are discovered to ensure that Data Streaming can be performed.
     * It means that the Bluetooth Manager retrieve all the services, which can be seen as categories of data that the headset is transmitting
     * This is an asynchro
     * nous operation.
     * Once service discovery is completed, the BluetoothGattCallback.onServicesDiscovered callback is triggered.
     * If the discovery was successful, the remote services can be retrieved using the getServices function
     */
    private void startDiscoveringServices(){
        LogUtils.i(TAG,"start discovering services ");
        if(getCurrentState().ordinal() >= BtState.CONNECTION_SUCCESS.ordinal()){ //if connection is in progress and BLE is at least connected, we can discover services
            try {
                AsyncUtils.executeAsync(new Runnable() {
                    @Override
                    public void run() {
                        MbtBluetoothManager.this.mbtBluetoothLE.discoverServices();
                    }
                });
                futureOperation = new CompletableFuture<>();
                futureOperation.get(MbtConfig.getBluetoothDiscoverTimeout(), TimeUnit.MILLISECONDS);
            } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
                if(e instanceof CancellationException )
                    futureOperation = null;
                LogUtils.i(TAG, "Exception raised discovery connection : \n " + e.toString());
            } finally {
                stopFutureOperation(true);
            }

            if(!getCurrentState().equals(BtState.DISCOVERING_SUCCESS))////at this point : current state should be DISCOVERING_SUCCESS if discovery succeeded
                updateConnectionState(BtState.DISCOVERING_FAILURE);
            switchToNextConnectionStep();
        }
    }

    private void startReadingDeviceInfo(DeviceInfo deviceInfo){
        updateConnectionState(false); //current state is set to READING_FIRMWARE_VERSION or READING_HARDWARE_VERSION or READING_SERIAL_NUMBER or READING_MODEL_NUMBER
        try {
            AsyncUtils.executeAsync(new Runnable() {
                @Override
                public void run() {
                    startReadOperation(deviceInfo);
                }
            });
            futureOperation = new CompletableFuture<>();
            futureOperation.get(MbtConfig.getBluetoothReadingTimeout(), TimeUnit.MILLISECONDS);
        } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
            LogUtils.i(TAG, "Exception raised during reading device info : \n " + e.toString());
            if(e instanceof CancellationException )
                futureOperation = null;
        } finally {
            stopFutureOperation(true);
        }

        switch(deviceInfo){
            case FW_VERSION:
                if(!getCurrentState().equals(BtState.READING_FIRMWARE_VERSION_SUCCESS)) //at this point : current state should be READING...SUCCESS if reading succeeded
                    updateConnectionState(BtState.READING_FAILURE);
                break;
            case HW_VERSION:
                if(!getCurrentState().equals(BtState.READING_HARDWARE_VERSION_SUCCESS))//at this point : current state should be READING...SUCCESS if reading succeeded
                    updateConnectionState(BtState.READING_FAILURE);
                break;
            case SERIAL_NUMBER:
                if(!getCurrentState().equals(BtState.READING_SERIAL_NUMBER_SUCCESS))//at this point : current state should be READING...SUCCESS if reading succeeded
                    updateConnectionState(BtState.READING_FAILURE);
                break;
            case MODEL_NUMBER:
                if(!getCurrentState().equals(BtState.READING_SUCCESS))//at this point : current state should be READING...SUCCESS if reading succeeded
                    updateConnectionState(BtState.READING_FAILURE);
                break;
        }
        switchToNextConnectionStep();
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
        LogUtils.i(TAG, "start bonding if supported");
        requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
            @Override
            public void onRequestComplete(MbtDevice device) { //Firmware version has been read during the previous step so we retrieve its value, as it has been stored in the Device Manager
                boolean isBondingSupported = new FirmwareUtils(device.getFirmwareVersion()).isFwValidForFeature(FirmwareUtils.FWFeature.BLE_BONDING);
                if(isBondingSupported) { //if firmware version bonding is higher than 1.6.7, the bonding is launched
                    LogUtils.i(TAG, "start bonding");
                    try {
                        AsyncUtils.executeAsync(new Runnable() {
                            @Override
                            public void run() {
                                mbtBluetoothLE.bond(device);
                            }
                        });
                        futureOperation = new CompletableFuture<>();
                        futureOperation.get(MbtConfig.getBluetoothBondingTimeout(), TimeUnit.MILLISECONDS);
                    } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
                        LogUtils.i(TAG, "Exception raised during bonding : \n " + e.toString());
                        if(e instanceof CancellationException )
                            futureOperation = null;
                    } finally {
                        stopFutureOperation(true);
                    }
                }else  //if firmware version bonding is older than 1.6.7, the connection process is considered completed
                    updateConnectionState(BtState.CONNECTED_AND_READY);
            }
        });
        if(getCurrentState().equals(BtState.BONDING)) { //at this point : current state should be BONDED if bonding succeeded
            if (getCurrentScannedDevice().getBondState() == BluetoothDevice.BOND_BONDED)
                updateConnectionState(false); //current state is set to BONDED
            else
                updateConnectionState(BtState.BONDING_FAILURE);
        }

        switchToNextConnectionStep();
    }

    private void startSendingExternalName() {
        LogUtils.i(TAG, "start sending QR code if supported");
        requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
            @Override
            public void onRequestComplete(MbtDevice device) {
                updateConnectionState(true);//current state is set to QR_CODE_SENDING
                if (device.getDeviceId() != null && device.getExternalName() != null && device.getExternalName().equals(MbtFeatures.MELOMIND_DEVICE_NAME) //send the QR code found in the database if the headset do not know its own QR code
                        && new FirmwareUtils(device.getFirmwareVersion()).isFwValidForFeature(FirmwareUtils.FWFeature.REGISTER_EXTERNAL_NAME)) {
                    LogUtils.i(TAG, "Headset do not know its number : sending external name to device with id " + device.getDeviceId());
                   AsyncUtils.executeAsync(new Runnable() {
                       @Override
                       public void run() {
                           mbtBluetoothLE.sendExternalName(new MelomindsQRDataBase(mContext, false).get(device.getDeviceId().replace(MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX, "")));
                       }
                   });
                }
                updateConnectionState(true);//current state is set to CONNECTED_AND_READY in any case (success or failure) the connection process is completed and the SDK consider that everything is ready for any operation (for example ready to acquire EEG data)
            }
        });
        switchToNextConnectionStep();
    }

    private void startConnectionForAudioStreaming(){
        LogUtils.i(TAG, "start connection audio streaming");
        if(MbtConfig.connectAudioIfDeviceCompatible() && !isAudioBluetoothConnected()) {
            isRequestCompleted = false;
            requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
                @Override
                public void onRequestComplete(MbtDevice device) {
                    if(!isRequestCompleted){
                        isRequestCompleted = true;
                        boolean connectionFromBleAvailable = new FirmwareUtils(device.getFirmwareVersion()).isFwValidForFeature(FirmwareUtils.FWFeature.A2DP_FROM_HEADSET);
                        try {
                            AsyncUtils.executeAsync(new Runnable() {
                                @Override
                                public void run() {
                                    if(isDataBluetoothConnected() && connectionFromBleAvailable)  //A2DP cannot be connected from BLE if BLE connection state is not CONNECTED_AND_READY
                                        mbtBluetoothLE.connectA2DPFromBLE();
                                    else// if connectA2DPFromBLE failed or is not supported by the headset firmware version
                                        connect(BLUETOOTH_A2DP);
                                }
                            });
                            futureOperation = new CompletableFuture<>();
                            futureOperation.get(MbtConfig.getBluetoothA2DpConnectionTimeout(), TimeUnit.MILLISECONDS);
                        } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
                            LogUtils.i(TAG, "Exception raised during audio connection : \n " + e.toString());
                            if(e instanceof CancellationException )
                                futureOperation = null;
                        } finally {
                            stopFutureOperation(true);
                        }
                    }
                }
            });
            if(!mbtBluetoothA2DP.isConnected())
                mbtBluetoothA2DP.notifyConnectionStateChanged(BtState.CONNECTION_FAILURE); //at this point : current state should be AUDIO_CONNECTED if audio connection succeeded
        }
        requestBeingProcessed = false;
        LogUtils.i(TAG, "connection for audio streaming done");
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
            cancelPendingConnection(((DisconnectRequestEvent) request).isInterrupted());
        else
            requestHandler.post(new Runnable() {
                @Override
                public void run() {
                    requestThread.parseRequest(request);
                }
            });
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
    private void startStreamOperation(boolean enableDeviceStatusMonitoring){
        if(!mbtBluetoothLE.isConnected()){
            notifyStreamStateChanged(IStreamable.StreamState.DISCONNECTED);
            requestBeingProcessed = false;
            return;
        }

        if(mbtBluetoothLE.isStreaming()){
            requestBeingProcessed = false;
            return;
        }

        //TODO remove configureHeadset method from here later on.
        configureHeadset(new DeviceConfig.Builder().useP300(false).create());

        if(enableDeviceStatusMonitoring)
            mbtBluetoothLE.activateDeviceStatusMonitoring();

        if(!mbtBluetoothLE.startStream()){
            requestBeingProcessed = false;
            EventBusManager.postEvent(IStreamable.StreamState.FAILED);
        }
    }

    /**
     * Initiates the acquisition of EEG data from the correct BtProtocol
     * If there is no streaming session in progress, nothing happens and the method returns silently.
     */
    private void stopStreamOperation(){
        if(!mbtBluetoothLE.isStreaming()) {
            requestBeingProcessed = false;
            return;
        }
        if(!mbtBluetoothLE.stopStream()){
            requestBeingProcessed  = false;
            EventBusManager.postEvent(IStreamable.StreamState.FAILED);
        }
    }

    /**
     * Start the disconnect operation on the currently connected bluetooth device according to the {@link BtProtocol} currently used.
     */
    private void disconnect(BtProtocol protocol) {
        LogUtils.i(TAG, "Disconnect");
        if(isAudioBluetoothConnected() || isDataBluetoothConnected() || getCurrentState().isConnectionInProgress()){
            switch(protocol){
                case BLUETOOTH_LE:
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
    }

    /**
     * Stops current pending connection according to its current {@link BtState state}.
     * It can be either stop scan or connection process interruption
     */
    private void cancelPendingConnection(boolean isClientUserAbortion) {
        LogUtils.i(TAG, "cancelling pending connection");
        requestBeingProcessed = false;
        if(isAudioBluetoothConnected())
            disconnect(BLUETOOTH_A2DP);
        disconnect(MbtConfig.isCurrentDeviceAMelomind() ? BtProtocol.BLUETOOTH_LE : BtProtocol.BLUETOOTH_SPP);

        if(isClientUserAbortion){
            isConnectionInterrupted = true;
            updateConnectionState(BtState.CONNECTION_INTERRUPTED);
        }
        LogUtils.i(TAG, "cancelling pending connection stop future");
        stopFutureOperation(true);
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
        requestBeingProcessed = false;
        if (newState.equals(BtState.DISCONNECTED)) {
            if(futureSwitchOperation != null && !futureSwitchOperation.isDone() && !futureSwitchOperation.isCancelled())
                futureSwitchOperation.complete(true); //a new a2dp connection was detected while an other headset was connected : here the last device has been well disconnected so we can connect BLE from A2DP
            else
                cancelPendingConnection(false); //a disconnection occurred
        }
        switch (newState){ //This event is sent to device module if registered
            case AUDIO_DISCONNECTED:
                mbtBluetoothA2DP.notifyConnectionStateChanged(newState, false);
                break;
            case AUDIO_CONNECTED:
                mbtBluetoothA2DP.notifyConnectionStateChanged(newState,false);
                stopFutureOperation(false);
                if(mbtBluetoothA2DP.getConnectedDevice() != null){
                    String bleDeviceName = mbtBluetoothLE.getBleDeviceNameFromA2dp(mbtBluetoothA2DP.getConnectedDevice().getName(), mContext);
                    if((!isDataBluetoothConnected() || !mbtBluetoothLE.isCurrentDeviceNameEqual(bleDeviceName)) && MbtConfig.connectAudioIfDeviceCompatible())
                        connectBLEFromA2DP(mbtBluetoothA2DP.getConnectedDevice().getName());
                }
                break;
            case DEVICE_FOUND:
                EventBusManager.postEvent(new DeviceEvents.NewBluetoothDeviceEvent(getCurrentScannedDevice()));
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

        ConnectionStateEvent connectionStateEvent = (mbtBluetoothA2DP.getCurrentState().equals(BtState.CONNECTION_FAILURE) ?
                new ConnectionStateEvent(newState, " Impossible to connect Bluetooth for audio streaming.") : new ConnectionStateEvent(newState));
        EventBusManager.postEvent(connectionStateEvent); //This event is sent to MbtManager for user notifications
    }

    /**
     * This method is called from Bluetooth classes and is meant to post an event to the main manager
     * that contains the {@link DeviceInfo} with the associated value
     * @param deviceInfo the {@link DeviceInfo}
     * @param deviceValue the new value as String
     */
    void notifyDeviceInfoReceived(DeviceInfo deviceInfo, String deviceValue){
        requestBeingProcessed = false;
        EventBusManager.postEvent(new DeviceInfoEvent<>(deviceInfo, deviceValue));
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
        EventBusManager.postEventWithCallback(new DeviceEvents.GetDeviceEvent(), new EventBusManager.CallbackVoid<DeviceEvents.PostDeviceEvent>(){
            @Override
            @Subscribe
            public void onEventCallback(DeviceEvents.PostDeviceEvent device) {
                callback.onRequestComplete(device.getDevice());
            }
        });
    }

    /**
     * Set the current bluetooth connection state to the value given in parameter
     * and notify the bluetooth manager of this change.
     * This method should be called :
     * - if something went wrong during the connection process
     * - or if the new state does not correspond to the state that follow the current state in chronological order ({@link BtState} enum order)
     * The updateConnectionState(boolean) method with no parameter should be call if nothing went wrong and user wants to continue the connection process
     */
    private void updateConnectionState(BtState state){
        if(state != null && (!isConnectionInterrupted || state.equals(BtState.CONNECTION_INTERRUPTED))){
            if(MbtConfig.isCurrentDeviceAMelomind())
                mbtBluetoothLE.notifyConnectionStateChanged(state);
            else
                mbtBluetoothSPP.notifyConnectionStateChanged(state);
        }
    }
    /**
     * Set the current bluetooth connection state to the value of the next step in chronological order (according to enum order)
     * and notify the bluetooth manager of this change.
     * This method should be called if no error occured.
     */
    public void updateConnectionState(boolean isFutureCompleted){
        if(!isConnectionInterrupted)
            updateConnectionState(getCurrentState().getNextConnectionStep());
        if(isFutureCompleted)
            stopFutureOperation(false);
    }

    private void stopFutureOperation(boolean isCancel){
        if(futureOperation != null && !futureOperation.isDone() && !futureOperation.isCancelled()){
            if(isCancel)
                futureOperation.cancel(true);
            else
                futureOperation.complete(false);
        }
    }

    /**
     * Return true if the user has requested connection with an already connected device, false otherwise
     */
    private boolean isAlreadyConnectedToRequestedDevice(String nameDeviceToConnect, MbtDevice deviceConnected){
        return (nameDeviceToConnect != null && deviceConnected != null && deviceConnected.getBluetoothDevice().getName().equals(nameDeviceToConnect));
    }

    /**
     * Tells whether or not the end-user device is currently connected to the headset in Low Energy.
     *
     * @return <code>true</code> if connected, <code>false</code> otherwise
     */
    private boolean isDataBluetoothConnected() {
        return  MbtFeatures.useLowEnergyProtocol() ? mbtBluetoothLE.isConnected() : mbtBluetoothSPP.isConnected();
    }

    /**
     * Tells whether or not the end-user device is currently connected to the headset in A2DP.
     * @return <code>true</code> if connected, <code>false</code> otherwise
     */
    private boolean isAudioBluetoothConnected() {
        return mbtBluetoothA2DP.isConnected();
    }

    /**
     * Tells whether or not the end-user device is currently connected to the headset both in Low Energy and A2DP.
     * @return <code>true</code> if connected, <code>false</code> otherwise
     */
    public final boolean isConnected() {
        return (MbtConfig.connectAudioIfDeviceCompatible() ? (isDataBluetoothConnected() && isAudioBluetoothConnected()) : isDataBluetoothConnected());
    }

    /**
     * Gets current state according to bluetooth protocol value
     */
    private BtState getCurrentState(){
        return MbtFeatures.useLowEnergyProtocol() ? mbtBluetoothLE.getCurrentState() : mbtBluetoothSPP.getCurrentState();
    }

    public String getDeviceNameRequested() {
        return deviceNameRequested;
    }

    private BluetoothDevice getCurrentScannedDevice() {
        return (MbtFeatures.useLowEnergyProtocol()) ? mbtBluetoothLE.scannedDevice : mbtBluetoothSPP.scannedDevice;
    }

    void disconnectA2DPFromBLE() {
        LogUtils.i(TAG, " disconnect A2dp from ble");
        if(isDataBluetoothConnected() && isAudioBluetoothConnected())
            mbtBluetoothLE.disconnectA2DPFromBLE();
    }

    /**
     * Starts a Low Energy connection process if a Melomind is connected for Audio Streaming in A2DP.
     */
    private void connectBLEFromA2DP(@NonNull String deviceName) {
        LogUtils.i(TAG, "connect BLE from a2dp ");
        if(mbtBluetoothA2DP.isConnected()){
            String newDeviceBleName = mbtBluetoothLE.getBleDeviceNameFromA2dp(deviceName, mContext); //BLE name = melo_ with the QRcode digits
            LogUtils.i(TAG, "associated BLE name is " + newDeviceBleName);
            if(mbtBluetoothLE.isConnected() && !mbtBluetoothLE.isCurrentDeviceNameEqual(newDeviceBleName)) //Disconnecting another melomind if already one connected in BLE
                mbtBluetoothLE.disconnect();

            deviceNameRequested = newDeviceBleName;

            if(!getCurrentState().equals(BtState.IDLE)) {
                try {
                    futureSwitchOperation= new CompletableFuture<>();
                    futureSwitchOperation.get(5000, TimeUnit.MILLISECONDS);
                }catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
                    LogUtils.i(TAG, "Exception raised during disconnection "+e);
                }
                EventBusManager.postEvent(new StartOrContinueConnectionRequestEvent(false,deviceNameRequested)); //current state should be IDLE
            }
        }
    }
}
