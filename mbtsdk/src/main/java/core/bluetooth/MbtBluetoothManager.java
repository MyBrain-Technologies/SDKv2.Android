package core.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import android.util.Log;

import org.apache.commons.lang.ArrayUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import command.BluetoothCommand;
import command.BluetoothCommands;
import command.CommandInterface;
import command.DeviceCommand;

import command.DeviceCommandEvent;
import command.OADCommands;
import config.MbtConfig;
import core.BaseModuleManager;
import command.DeviceCommands;
import command.DeviceStreamingCommands;
import core.bluetooth.lowenergy.MbtBluetoothLE;
import core.bluetooth.requests.BluetoothRequests;
import eventbus.events.BluetoothResponseEvent;
import core.bluetooth.requests.CommandRequestEvent;
import core.bluetooth.requests.DisconnectRequestEvent;
import core.bluetooth.requests.ReadRequestEvent;
import core.bluetooth.requests.StartOrContinueConnectionRequestEvent;
import core.bluetooth.requests.StreamRequestEvent;
import core.bluetooth.spp.MbtBluetoothSPP;
import core.device.DeviceEvents;
import core.device.model.DeviceInfo;
import core.device.model.MbtDevice;
import core.device.model.MelomindsQRDataBase;
import engine.SimpleRequestCallback;
import engine.clientevents.BaseError;
import engine.clientevents.ConnectionStateReceiver;
import eventbus.MbtEventBus;
import eventbus.events.BluetoothEEGEvent;
import eventbus.events.EEGConfigEvent;
import eventbus.events.ConnectionStateEvent;
import eventbus.events.DeviceInfoEvent;
import features.MbtDeviceType;
import features.MbtFeatures;
import utils.AsyncUtils;
import utils.BroadcastUtils;
import utils.VersionHelper;
import utils.LogUtils;
import utils.MbtAsyncWaitOperation;

import static core.bluetooth.BtProtocol.BLUETOOTH_A2DP;
import static core.bluetooth.spp.MbtBluetoothSPP.SERIAL_NUMBER_NB_BYTES;
import static core.bluetooth.spp.MbtBluetoothSPP.VERSION_NB_BYTES;
import static utils.MbtAsyncWaitOperation.CANCEL;

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
    private BluetoothContext bluetoothContext;
    private BluetoothInitializer bluetoothInitializer;

    private MbtDataBluetooth bluetoothForDataStreaming;
    private MbtAudioBluetooth bluetoothForAudioStreaming;

    private boolean requestBeingProcessed = false;
    private boolean isRequestCompleted = false;

    private RequestThread requestThread;
    /**
     * Handler object enqueues an action to be performed on a different thread than the main thread
     */
    private Handler requestHandler;

    private boolean isConnectionInterrupted = false;

    private MbtAsyncWaitOperation asyncOperation = new MbtAsyncWaitOperation<Boolean>();
    private MbtAsyncWaitOperation asyncSwitchOperation = new MbtAsyncWaitOperation<Boolean>();
    private int connectionRetryCounter = 0;
    private final int MAX_CONNECTION_RETRY = 2;

    private ConnectionStateReceiver receiver = new ConnectionStateReceiver() {
        @Override
        public void onError(BaseError error, String additionalInfo) { }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null){
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, " received intent " + action + " for device " + (device != null ? device.getName() : null));
                if(bluetoothForAudioStreaming != null && bluetoothContext.getConnectAudioIfDeviceCompatible() && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
                    ((MbtBluetoothA2DP)bluetoothForAudioStreaming).resetA2dpProxy(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1));
            }
        }
    };

    /**
     * Constructor of the manager.
     * @param context the application context
     */
    public MbtBluetoothManager(@NonNull Context context){
        super(context);

        //Init thread that will handle messages synchronously. Using HandlerThread looks like it is the best way for CPU consomption as infinite loop in async thread was too heavy for cpu
        requestThread = new RequestThread("requestThread", Thread.MAX_PRIORITY);
        requestThread.start();
        requestHandler = new Handler(requestThread.getLooper());

        BroadcastUtils.registerReceiverIntents(context, receiver, BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    /**
     * Notify the DeviceManager and the EEGManager that the headset returned its stored configuration
     */
    private void notifyDeviceConfigReceived(Byte[] returnedConfig) {

        requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
            @Override
            public void onRequestComplete(MbtDevice device) {
                MbtEventBus.postEvent(new EEGConfigEvent(device, returnedConfig));
            }
        });

    }

    /**
     * Notify a command response has been received from the headset
     * @param command is the corresponding type of command
     */
    public void notifyResponseReceived(Object response, CommandInterface.MbtCommand command) {
        LogUtils.d(TAG, "Received response from device : "+ response);

        if(command instanceof DeviceCommand)
            notifyDeviceResponseReceived(response, (DeviceCommand) command);

        else if (command instanceof BluetoothCommand)
            onBluetoothResponseReceived(response, (BluetoothCommand) command);

        setRequestAsProcessed();
    }

    private void notifyDeviceResponseReceived(Object response, DeviceCommand command) {
        if(response != null){
            if(command instanceof DeviceStreamingCommands.EegConfig){
                notifyDeviceConfigReceived(ArrayUtils.toObject((byte[])response));

            }else if(command instanceof DeviceCommands.UpdateSerialNumber) {
                notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER, new String((byte[]) response));

            }else if(command instanceof DeviceCommands.GetDeviceInfo) {
                notifyDeviceInfoReceived(DeviceInfo.FW_VERSION, new String(ArrayUtils.subarray((byte[])response, 0, VERSION_NB_BYTES)));
                notifyDeviceInfoReceived(DeviceInfo.HW_VERSION, new String(ArrayUtils.subarray((byte[])response, VERSION_NB_BYTES, VERSION_NB_BYTES+VERSION_NB_BYTES)));
                notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER, new String(ArrayUtils.subarray((byte[])response, VERSION_NB_BYTES+VERSION_NB_BYTES, VERSION_NB_BYTES+VERSION_NB_BYTES+SERIAL_NUMBER_NB_BYTES)));

            }else if(command instanceof DeviceCommands.UpdateExternalName) {
                notifyDeviceInfoReceived(DeviceInfo.MODEL_NUMBER, new String((byte[]) response));

            }else if(command instanceof DeviceCommands.GetBattery) {
                notifyDeviceInfoReceived(DeviceInfo.BATTERY, Integer.toString((Integer) response));

            }else if(command instanceof OADCommands) {
                updateConnectionState(BluetoothState.UPGRADING);
                if(bluetoothForAudioStreaming != null)
                    bluetoothForAudioStreaming.setCurrentState(BluetoothState.UPGRADING);
                notifyEventReceived(command.getIdentifier(), response);
            }
        }
    }

    /**
     * Notify the Connection process handler if the MTU has been well changed
     * @param command is the corresponding type of bluetooth command
     */
    private void onBluetoothResponseReceived(Object response, BluetoothCommand command) {
        if(command instanceof BluetoothCommands.Mtu)
            onMtuChanged(response == command.getData());
    }

    /**
     * This class is a specific thread that will handle all bluetooth operations. Bluetooth operations
     * are synchronous, meaning two or more operations can't be run simultaneously. This {@link HandlerThread}
     * extended class is able to hold pending operations.
     */
    class RequestThread extends HandlerThread {

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
            Log.d(TAG, "Parse request "+request.toString());

            while (requestBeingProcessed);
            setRequestAsProcessing();
            if (request instanceof StartOrContinueConnectionRequestEvent) {

                initBluetoothParameters((StartOrContinueConnectionRequestEvent)request);
                startOrContinueConnectionOperation(((StartOrContinueConnectionRequestEvent) request).isClientUserRequest());

            } else if (request instanceof ReadRequestEvent) {
                startReadOperation(((ReadRequestEvent) request).getDeviceInfo());

            } else if (request instanceof DisconnectRequestEvent) {
                if (((DisconnectRequestEvent) request).isInterrupted())
                    cancelPendingConnection(((DisconnectRequestEvent) request).isInterrupted());
                else
                    disconnectAllBluetooth(true);

            } else if (request instanceof StreamRequestEvent) {
                if (((StreamRequestEvent) request).isStart()) {
                    startStreamOperation(((StreamRequestEvent) request).monitorDeviceStatus());
                }else if(((StreamRequestEvent) request).stopStream())
                    stopStreamOperation();
                else
                    setRequestAsProcessed();

            } else if (request instanceof CommandRequestEvent) {
                sendCommand(((CommandRequestEvent) request).getCommand());
            }
        }

        private void initBluetoothParameters(StartOrContinueConnectionRequestEvent event) {
            bluetoothContext = new BluetoothContext(
                mContext,
                event.getTypeOfDeviceRequested(),
                event.connectAudioIfDeviceCompatible(),
                event.getNameOfDeviceRequested(),
                event.getQrCodeOfDeviceRequested(),
                event.getMtu());

            if (event.isClientUserRequest() && bluetoothForDataStreaming == null) {
                if (bluetoothContext.getDeviceTypeRequested().equals(MbtDeviceType.MELOMIND)) {
                    bluetoothForDataStreaming = new MbtBluetoothLE(mContext, MbtBluetoothManager.this);
                    if (bluetoothContext.getConnectAudioIfDeviceCompatible())
                        bluetoothForAudioStreaming = new MbtBluetoothA2DP(mContext, MbtBluetoothManager.this);

                } else if (bluetoothContext.getDeviceTypeRequested().equals(MbtDeviceType.VPRO))
                    bluetoothForDataStreaming = new MbtBluetoothSPP(mContext, MbtBluetoothManager.this);
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
            if(isClientUserRequest)
                isConnectionInterrupted = false;

            if(!isConnectionInterrupted){
                LogUtils.d(TAG, "State is "+getCurrentState());
                switch (getCurrentState()){
                    case IDLE:
                    case DATA_BT_DISCONNECTED:
                        getReadyForBluetoothOperation();
                        break;
                    case READY_FOR_BLUETOOTH_OPERATION:
                        startScan();
                        break;
                    case DEVICE_FOUND:
                    case DATA_BT_CONNECTING:
                        startConnectionForDataStreaming();
                        break;
                    case DATA_BT_CONNECTION_SUCCESS:
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
                        changeMTU();
                        break;
                    case BT_PARAMETERS_CHANGED:
                        startSendingExternalName();
                        break;
                    case CONNECTED:
                        startConnectionForAudioStreaming();
                        break;
                    default:
                        setRequestAsProcessed();
                }
            }else
                setRequestAsProcessed();
        }
    }

    @VisibleForTesting
    void setRequestThread(RequestThread requestThread) {
        this.requestThread = requestThread;
    }

    @VisibleForTesting
    void setRequestHandler(Handler requestHandler) {
        this.requestHandler = requestHandler;
    }

    @VisibleForTesting
    void setBluetoothForDataStreaming(MbtBluetoothLE bluetoothForDataStreaming) {
        this.bluetoothForDataStreaming = bluetoothForDataStreaming;
    }

    @VisibleForTesting
    public RequestThread getRequestThread() {
        return requestThread;
    }

    private void switchToNextConnectionStep(){
        setRequestAsProcessed();
        if(!getCurrentState().isAFailureState() && !isConnectionInterrupted && !getCurrentState().equals(BluetoothState.IDLE)) {  //if nothing went wrong during the current step of the connection process, we continue the process
            onNewBluetoothRequest(new StartOrContinueConnectionRequestEvent(false, bluetoothContext));
        }
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
            if(!isAlreadyConnectedToRequestedDevice(bluetoothContext.getDeviceNameRequested(), currentConnectedDevice)) {
                updateConnectionState(BluetoothState.ANOTHER_DEVICE_CONNECTED);
            }else
                updateConnectionState(BluetoothState.CONNECTED_AND_READY);
            isAlreadyConnected = true;
        }
        return isAlreadyConnected;
    }

    /**
     * Check the bluetooth prerequisites before starting any bluetooth operation.
     * The started Bluetooth connection process is stopped if the prerequisites are not valid.
     */
    private void getReadyForBluetoothOperation(){
        connectionRetryCounter = 0;
        //Request sent to the BUS in order to get device from the device manager : the BUS should return a null object if it's the first connection, or return a non null object if the user requests connection whereas a headset is already connected
        LogUtils.i(TAG, "Checking Bluetooth Prerequisites and initialize");

        requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
            @Override
            public void onRequestComplete(MbtDevice device) { //when the BUS has returned the device object
                if (device != null && isAlreadyConnected(device)) // assert that headset is not already connected
                    return;

                bluetoothInitializer = new BluetoothInitializer();
                BluetoothState state = bluetoothInitializer.getBluetoothPrerequisitesState(bluetoothContext);
                if (state != BluetoothState.READY_FOR_BLUETOOTH_OPERATION) { //assert that Bluetooth is on, Location is on, Location permission is granted
                    updateConnectionState(state);
                    return;
                }

                if(bluetoothForAudioStreaming != null && bluetoothForAudioStreaming instanceof MbtBluetoothA2DP)
                    ((MbtBluetoothA2DP)bluetoothForAudioStreaming).initA2dpProxy(); //initialization to check if a Melomind is already connected in A2DP : as the next step is the scanning, the SDK is able to filter on the name of this device

                if (getCurrentState().equals(BluetoothState.IDLE))
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
        BluetoothState newState = BluetoothState.SCAN_FAILURE;
        try {
            AsyncUtils.executeAsync(new Runnable() {
                @Override
                public void run() {
                    bluetoothForDataStreaming.startScan();
                }
            });
            asyncOperation.waitOperationResult(MbtConfig.getBluetoothScanTimeout());
        } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
            if(e instanceof TimeoutException )
                newState = BluetoothState.SCAN_TIMEOUT ; //stop the current Bluetooth connection process
            else if(e instanceof CancellationException )
                asyncOperation.resetWaitingOperation();
            LogUtils.w(TAG, "Exception raised during scanning : \n " + e.toString());
        } finally {
            stopScan();
        }
        if(getCurrentState().equals(BluetoothState.SCAN_STARTED)) ////at this point : current state should be DEVICE_FOUND if scan succeeded
            updateConnectionState(newState); //scan failure or timeout
        switchToNextConnectionStep();
    }

    /**
     * This method stops the currently running bluetooth scan, either Le scan or discovery scan
     */
    private void stopScan(){
        asyncOperation.stopWaitingOperation(CANCEL);
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            bluetoothForDataStreaming.stopScan();
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
                    connect(bluetoothContext.getDeviceTypeRequested().getProtocol());
                }
            });
            asyncOperation.waitOperationResult(MbtConfig.getBluetoothConnectionTimeout()); // blocked until the futureOperation.complete() is called or until timeout
        } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
            LogUtils.w(TAG, "Exception raised during connection : \n " + e.toString());
            if(e instanceof CancellationException )
                asyncOperation.resetWaitingOperation();
        }finally {
            asyncOperation.stopWaitingOperation(CANCEL);
        }
        if(!getCurrentState().equals(BluetoothState.CONNECTED_AND_READY) && !getCurrentState().equals(BluetoothState.DATA_BT_CONNECTION_SUCCESS) && !getCurrentState().equals(BluetoothState.IDLE))
            updateConnectionState(BluetoothState.CONNECTION_FAILURE);
        switchToNextConnectionStep();
    }

    private void connect(BtProtocol protocol){
        boolean isConnectionSuccessful = false;
        this.isConnectionInterrupted = false; // resetting the flag when starting a new connection

        switch (protocol){
            case BLUETOOTH_LE:
            case BLUETOOTH_SPP:
                isConnectionSuccessful = bluetoothForDataStreaming.connect(mContext, getCurrentDevice());
                break;
            case BLUETOOTH_A2DP:
                if(bluetoothForAudioStreaming != null)
                    isConnectionSuccessful = bluetoothForAudioStreaming.connect(mContext, getCurrentDevice());
                break;
        }
        if(isConnectionSuccessful) {
            if(protocol.equals(BLUETOOTH_A2DP)) {
                if (isAudioBluetoothConnected())
                    asyncOperation.stopWaitingOperation(false);
            }else
                updateConnectionState(isDataBluetoothConnected());
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
        if(bluetoothForDataStreaming instanceof MbtBluetoothLE) {
            LogUtils.i(TAG, "start discovering services ");
            if (getCurrentState().ordinal() >= BluetoothState.DATA_BT_CONNECTION_SUCCESS.ordinal()) { //if connection is in progress and BLE is at least connected, we can discover services
                try {
                    AsyncUtils.executeAsync(new Runnable() {
                        @Override
                        public void run() {
                            ((MbtBluetoothLE)bluetoothForDataStreaming).discoverServices();
                        }
                    });
                    asyncOperation.waitOperationResult(MbtConfig.getBluetoothDiscoverTimeout());
                } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
                    if (e instanceof CancellationException)
                        asyncOperation.resetWaitingOperation();
                    LogUtils.i(TAG, "Exception raised discovery connection : \n " + e.toString());
                } finally {
                    asyncOperation.stopWaitingOperation(CANCEL);
                }

                if (!getCurrentState().equals(BluetoothState.DISCOVERING_SUCCESS)) {////at this point : current state should be DISCOVERING_SUCCESS if discovery succeeded
                    updateConnectionState(BluetoothState.DISCOVERING_FAILURE);
                }
                switchToNextConnectionStep();
            }
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
            asyncOperation.waitOperationResult(MbtConfig.getBluetoothReadingTimeout());
        } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
            LogUtils.w(TAG, "Exception raised during reading device info : \n " + e.toString());
            if(e instanceof CancellationException )
                asyncOperation.resetWaitingOperation();
        } finally {
            asyncOperation.stopWaitingOperation(CANCEL);
        }

        switch(deviceInfo){
            case FW_VERSION:
                if(!getCurrentState().equals(BluetoothState.READING_FIRMWARE_VERSION_SUCCESS)) //at this point : current state should be READING...SUCCESS if reading succeeded
                    updateConnectionState(BluetoothState.READING_FAILURE);
                break;
            case HW_VERSION:
                if(!getCurrentState().equals(BluetoothState.READING_HARDWARE_VERSION_SUCCESS))//at this point : current state should be READING...SUCCESS if reading succeeded
                    updateConnectionState(BluetoothState.READING_FAILURE);
                break;
            case SERIAL_NUMBER:
                if(!getCurrentState().equals(BluetoothState.READING_SERIAL_NUMBER_SUCCESS))//at this point : current state should be READING...SUCCESS if reading succeeded
                    updateConnectionState(BluetoothState.READING_FAILURE);
                break;
            case MODEL_NUMBER:
                if(!getCurrentState().equals(BluetoothState.READING_SUCCESS))//at this point : current state should be READING...SUCCESS if reading succeeded
                    updateConnectionState(BluetoothState.READING_FAILURE);
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
        if(!bluetoothForDataStreaming.readBattery()){
            setRequestAsProcessed();
            MbtEventBus.postEvent(new DeviceInfoEvent<>(DeviceInfo.BATTERY, null));
        }
    }

    /**
     * Initiates a read firmware version operation on this correct BtProtocol
     * In case of failure during read process, an event with error is posted to the main manager.
     */
    private void readFwVersion(){
        if(bluetoothForDataStreaming instanceof BluetoothInterfaces.IDeviceInfoMonitor
                && !((BluetoothInterfaces.IDeviceInfoMonitor)bluetoothForDataStreaming).readFwVersion()){
            setRequestAsProcessed();
            MbtEventBus.postEvent(new DeviceInfoEvent<>(DeviceInfo.FW_VERSION, null));
        }
    }

    /**
     * Initiates a read hardware version operation on this correct BtProtocol
     * In case of failure during read process, an event with error is posted to the main manager.
     */
    private void readHwVersion(){
        if(bluetoothForDataStreaming instanceof BluetoothInterfaces.IDeviceInfoMonitor
                && !((BluetoothInterfaces.IDeviceInfoMonitor)bluetoothForDataStreaming).readHwVersion()){
            setRequestAsProcessed();
            MbtEventBus.postEvent(new DeviceInfoEvent<>(DeviceInfo.HW_VERSION, null));
        }
    }

    /**
     * Initiates a read serial number operation on this correct BtProtocol
     * In case of failure during read process, an event with error is posted to the main manager.
     */
    private void readSerialNumber(){
        if(bluetoothForDataStreaming instanceof BluetoothInterfaces.IDeviceInfoMonitor
                && !((BluetoothInterfaces.IDeviceInfoMonitor)bluetoothForDataStreaming).readSerialNumber()){
            setRequestAsProcessed();
            MbtEventBus.postEvent(new DeviceInfoEvent<>(DeviceInfo.SERIAL_NUMBER, null));

        }
    }

    /**
     * Initiates a read model number operation on this correct BtProtocol
     * In case of failure during read process, an event with error is posted to the main manager.
     */
    private void readModelNumber(){
        if(bluetoothForDataStreaming instanceof BluetoothInterfaces.IDeviceInfoMonitor
                && !((BluetoothInterfaces.IDeviceInfoMonitor)bluetoothForDataStreaming).readModelNumber()){
            setRequestAsProcessed();
            MbtEventBus.postEvent(new DeviceInfoEvent<>(DeviceInfo.MODEL_NUMBER, null));

        }
    }

    private void startBonding() {
        if(bluetoothForDataStreaming instanceof MbtBluetoothLE){
            LogUtils.i(TAG, "start bonding if supported");
            requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
                @Override
                public void onRequestComplete(MbtDevice device) { //Firmware version has been read during the previous step so we retrieve its value, as it has been stored in the Device Manager
                    boolean isBondingSupported = new VersionHelper(device.getFirmwareVersion().toString()).isValidForFeature(VersionHelper.Feature.BLE_BONDING);
                    if (isBondingSupported) { //if firmware version bonding is higher than 1.6.7, the bonding is launched
                        try {
                            AsyncUtils.executeAsync(new Runnable() {
                                @Override
                                public void run() {
                                    if(getCurrentState() == BluetoothState.BONDING) //avoid double bond if several requestCurrentConnectedDevice are called at the same moment
                                        return;

                                    ((MbtBluetoothLE)bluetoothForDataStreaming).bond();
                                }
                            });
                            asyncOperation.waitOperationResult(MbtConfig.getBluetoothBondingTimeout());
                        } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
                            LogUtils.w(TAG, "Exception raised during bonding : \n " + e.toString());
                            if (e instanceof CancellationException)
                                asyncOperation.resetWaitingOperation();
                        } finally {
                            asyncOperation.stopWaitingOperation(CANCEL);
                        }

                    } else  //if firmware version bonding is older than 1.6.7, the connection process is considered completed
                        updateConnectionState(BluetoothState.CONNECTED);
                }
            });

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(getCurrentState().equals(BluetoothState.BONDING)) { //at this point : current state should be BONDED if bonding succeeded
               updateConnectionState(BluetoothState.BONDING_FAILURE);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            switchToNextConnectionStep();
        }

    }

    private void startSendingExternalName() {
        LogUtils.i(TAG, "start sending QR code if supported");
        requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
            @Override
            public void onRequestComplete(MbtDevice device) {
                LogUtils.d(TAG, "device "+device);
                updateConnectionState(true);//current state is set to QR_CODE_SENDING
                if (device.getSerialNumber() != null && device.getExternalName() != null && (device.getExternalName().equals(MbtFeatures.MELOMIND_DEVICE_NAME) || device.getExternalName().length() == MbtFeatures.DEVICE_QR_CODE_LENGTH-1) //send the QR code found in the database if the headset do not know its own QR code
                        && new VersionHelper(device.getFirmwareVersion().toString()).isValidForFeature(VersionHelper.Feature.REGISTER_EXTERNAL_NAME)) {
                    AsyncUtils.executeAsync(new Runnable() {
                        @Override
                        public void run() {
                            String externalName = new MelomindsQRDataBase(mContext, false).get(device.getSerialNumber());
                            sendCommand(new DeviceCommands.UpdateExternalName(externalName));
                        }
                    });
                }
                updateConnectionState(true);//current state is set to CONNECTED in any case (success or failure) the connection process is completed and the SDK consider that everything is ready for any operation (for example ready to acquire EEG data)
            }
        });
        switchToNextConnectionStep();
    }

    private void startConnectionForAudioStreaming(){
        if(bluetoothContext.getConnectAudioIfDeviceCompatible() && !isAudioBluetoothConnected()) {
            LogUtils.i(TAG, "start connection audio streaming");
            isRequestCompleted = false;
            requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
                @Override
                public void onRequestComplete(MbtDevice device) {
                    if(device == null)
                        return;
                    if(!isRequestCompleted){
                        isRequestCompleted = true;
                        boolean connectionFromBleAvailable = new VersionHelper(device.getFirmwareVersion().toString()).isValidForFeature(VersionHelper.Feature.A2DP_FROM_HEADSET);
                        try {
                            AsyncUtils.executeAsync(new Runnable() {
                                @Override
                                public void run() {
                                    if (connectionFromBleAvailable)   //A2DP cannot be connected from BLE if BLE connection state is not CONNECTED_AND_READY or CONNECTED
                                        sendCommand(new DeviceCommands.ConnectAudio());
                                    else {// if connectA2DPFromBLE failed or is not supported by the headset firmware version
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || (((MbtBluetoothA2DP)bluetoothForAudioStreaming).isPairedDevice(getCurrentDevice())))
                                            connect(BLUETOOTH_A2DP);
                                        else
                                            notifyConnectionStateChanged(BluetoothState.AUDIO_CONNECTION_UNSUPPORTED);
                                    }
                                }
                            });
                            asyncOperation.waitOperationResult(MbtConfig.getBluetoothA2DpConnectionTimeout());
                        } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
                            LogUtils.w(TAG, "Exception raised during audio connection : \n " + e.toString());
                            if(e instanceof CancellationException )
                                asyncOperation.resetWaitingOperation();
                        } finally {
                            asyncOperation.stopWaitingOperation(CANCEL);
                        }
                    }
                }
            });

            if(!bluetoothForAudioStreaming.isConnected()) {
                if (connectionRetryCounter < MAX_CONNECTION_RETRY) {
                    connectionRetryCounter++;
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    startConnectionForAudioStreaming();
                } else {
                    connectionRetryCounter = 0;
                    bluetoothForAudioStreaming.notifyConnectionStateChanged(BluetoothState.CONNECTION_FAILURE); //at this point : current state should be AUDIO_CONNECTED if audio connection succeeded
                    bluetoothForDataStreaming.notifyConnectionStateChanged(BluetoothState.CONNECTION_FAILURE);
                }
            }

        }
        if(isConnected())
            updateConnectionState(true); //BLE and audio (if SDK user requested it) are connected so the client is notified that the device is fully connected

        setRequestAsProcessed();

        LogUtils.i(TAG, "connection completed");
    }

    /**
     * Command sent from the SDK to the connected headset
     * in order to change its Maximum Transmission Unit
     * (maximum size of the data sent by the headset to the SDK).
     */
    private void changeMTU(){
        updateConnectionState(true); //current state is set to CHANGING_BT_PARAMETERS
        sendCommand(new BluetoothCommands.Mtu(bluetoothContext.getMtu()));
    }

    private void onMtuChanged(boolean isSuccess){
        updateConnectionState(true); //current state is set to BT_PARAMETERS_CHANGED
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
            cancelPendingConnection(((DisconnectRequestEvent) request).isInterrupted());
        else
            requestHandler.post(new Runnable() { // enqueue a Runnable object to be called by the Handler message queue when they are received
                @Override
                public void run() {
                    requestThread.parseRequest(request);//When posting or sending to a Handler, the item is processed as soon as the message queue is ready to do so
                }
            });
    }


    /**
     * This method handle a single command in order to
     * reconfigure some headset's parameters
     * or get values stored by the headset
     * or ask the headset to perform an action.
     * The command's parameters are bundled in a {@link DeviceCommand instance}
     * that can provide a nullable response callback.
     * All method inside are blocking.
     * @param command is the {@link DeviceCommand} object that defines the type of command to send
     * and the asociated command parameters.
     */

    private void sendCommand(@NonNull CommandInterface.MbtCommand command) {
        if (bluetoothForDataStreaming == null) {
            return;
        }
        bluetoothForDataStreaming.sendCommand(command);
    }

    /**
     * Initiates the acquisition of EEG data. This method chooses between the correct BtProtocol.
     * If there is already a streaming session in progress, nothing happens and the method returns silently.
     */
    private void startStreamOperation(boolean enableDeviceStatusMonitoring) {
        Log.d(TAG, "Bluetooth Manager starts streaming");

        if (!bluetoothForDataStreaming.isConnected()) {
            notifyStreamStateChanged(StreamState.DISCONNECTED);
            setRequestAsProcessed();
            return;
        }

        if (bluetoothForDataStreaming.isStreaming()) {
            setRequestAsProcessed();
            return;
        }

        if (enableDeviceStatusMonitoring && bluetoothForDataStreaming instanceof MbtBluetoothLE)
            ((MbtBluetoothLE) bluetoothForDataStreaming).activateDeviceStatusMonitoring();

        try {
            AsyncUtils.executeAsync(new Runnable() {
                @Override
                public void run() {
                    if (!bluetoothForDataStreaming.startStream()) {
                        MbtEventBus.postEvent(StreamState.FAILED);
                    }
                }
            });
            Boolean startSucceeded = (Boolean)asyncOperation.waitOperationResult(6000);
            if(startSucceeded != null && !startSucceeded)
                MbtEventBus.postEvent(StreamState.FAILED);

            setRequestAsProcessed();

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }


    /**
     * Initiates the acquisition of EEG data from the correct BtProtocol
     * If there is no streaming session in progress, nothing happens and the method returns silently.
     */
    private void stopStreamOperation(){
        Log.d(TAG, "Bluetooth Manager stops streaming");
        if (!bluetoothForDataStreaming.isStreaming()) {
            setRequestAsProcessed();
            return;
        }

        try {
            AsyncUtils.executeAsync(new Runnable() {
                @Override
                public void run() {
                    if (!bluetoothForDataStreaming.stopStream()){
                        asyncOperation.stopWaitingOperation(false);
                        bluetoothForDataStreaming.notifyStreamStateChanged(StreamState.FAILED);
                    }
                }
            });
            Boolean stopSucceeded = (Boolean)asyncOperation.waitOperationResult(6000);
            if(stopSucceeded != null && !stopSucceeded)
                bluetoothForDataStreaming.notifyStreamStateChanged(StreamState.FAILED);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            bluetoothForDataStreaming.notifyStreamStateChanged(StreamState.FAILED);
        } finally {
            setRequestAsProcessed();
        }

    }

    /**
     * Start the disconnect operation on the currently connected bluetooth device according to the {@link BtProtocol} currently used.
     */
    private void disconnect(BtProtocol protocol) {
        if(isAudioBluetoothConnected() || isDataBluetoothConnected() || getCurrentState().isConnectionInProgress()){
            switch(protocol){
                case BLUETOOTH_LE:
                case BLUETOOTH_SPP:
                    bluetoothForDataStreaming.disconnect();
                    break;
                case BLUETOOTH_A2DP:
                    if(bluetoothForAudioStreaming != null)
                        bluetoothForAudioStreaming.disconnect();
                    break;
            }
        }
    }

    void disconnectAllBluetooth(boolean disconnectAudioIfConnected){
        LogUtils.i(TAG, "Disconnect all bluetooth");
        if(isAudioBluetoothConnected() && disconnectAudioIfConnected)
            disconnect(BLUETOOTH_A2DP);
        disconnect(bluetoothContext.getDeviceTypeRequested().getProtocol());
    }

    /**
     * Stops current pending connection according to its current {@link BluetoothState state}.
     * It can be either stop scan or connection process interruption
     */
    private void cancelPendingConnection(boolean isClientUserAbortion) {
        LogUtils.i(TAG, "cancelling pending connection");
        setRequestAsProcessed();
        disconnectAllBluetooth(!asyncSwitchOperation.isWaiting());

        if(isClientUserAbortion){
            isConnectionInterrupted = true;
            updateConnectionState(BluetoothState.CONNECTION_INTERRUPTED);
        }
        asyncOperation.stopWaitingOperation(CANCEL);
    }

    /**
     * Posts a BluetoothEEGEvent event to the bus so that MbtEEGManager can handle raw EEG data received
     * @param data the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     */
    public void handleDataAcquired(@NonNull final byte[] data){
        MbtEventBus.postEvent(new BluetoothEEGEvent(data)); //MbtEEGManager will convert data from raw packets to eeg values
        setRequestAsProcessed(false);
    }

    /**
     * Unregister the MbtBluetoothManager class from the bus to avoid memory leak
     */
    void deinit(){
        MbtEventBus.registerOrUnregister(false,this);
    }

    /**
     * This method is called from Bluetooth classes and is meant to post an event to the main manager
     * that contains the new {@link BluetoothState}
     * @param newState the new {@link BluetoothState}
     */
    public void notifyConnectionStateChanged(@NonNull BluetoothState newState) {
        setRequestAsProcessed();

        switch (newState) { //This event is sent to device module if registered
            case DATA_BT_DISCONNECTED:
                if (asyncSwitchOperation.isWaiting())
                    asyncSwitchOperation.stopWaitingOperation(false); //a new a2dp connection was detected while an other headset was connected : here the last device has been well disconnected so we can connect BLE from A2DP
                else
                    cancelPendingConnection(false); //a disconnection occurred
                break;

            case AUDIO_BT_DISCONNECTED:
                if (bluetoothForAudioStreaming != null)
                    bluetoothForAudioStreaming.notifyConnectionStateChanged(newState, false);
                MbtEventBus.postEvent(new DeviceEvents.AudioDisconnectedDeviceEvent());

                break;

            case AUDIO_BT_CONNECTION_SUCCESS:
                if (bluetoothForAudioStreaming != null){
                    bluetoothForAudioStreaming.notifyConnectionStateChanged(newState, false);
                    asyncOperation.stopWaitingOperation(false);
                    if (bluetoothForAudioStreaming.getCurrentDevice() != null && bluetoothForDataStreaming instanceof MbtBluetoothLE) {
                        String bleDeviceName = ((MbtBluetoothLE) bluetoothForDataStreaming).getBleDeviceNameFromA2dp(bluetoothForAudioStreaming.getCurrentDevice().getName(), mContext);
                        if (!isDataBluetoothConnected() || !((MbtBluetoothLE) bluetoothForDataStreaming).isCurrentDeviceNameEqual(bleDeviceName))
                            connectBLEFromA2DP(bleDeviceName);

                        MbtEventBus.postEvent(new DeviceEvents.AudioConnectedDeviceEvent(bluetoothForAudioStreaming.getCurrentDevice()));

                    }
                }
                break;

            case JACK_CABLE_CONNECTED:
                if(asyncOperation.isWaiting())
                    asyncOperation.stopWaitingOperation(false);
                break;

            case DEVICE_FOUND:
                MbtEventBus.postEvent(new ConnectionStateEvent(newState, getCurrentDevice(), bluetoothContext.getDeviceTypeRequested()));
                if(bluetoothContext.getConnectAudioIfDeviceCompatible()){
                    if(bluetoothForAudioStreaming == null){
                        bluetoothForAudioStreaming = new MbtBluetoothA2DP(mContext, MbtBluetoothManager.this);
                    } else if (bluetoothForAudioStreaming.currentDevice != null) {
                        MbtEventBus.postEvent(new DeviceEvents.AudioConnectedDeviceEvent(bluetoothForAudioStreaming.currentDevice));
                    }
                }
        }

        requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
            @Override
            public void onRequestComplete(MbtDevice device) {
                MbtEventBus.postEvent(new ConnectionStateEvent(newState, device)); //This event is sent to MbtManager for user notifications and to MbtDeviceManager
            }
        });

    }

    /**
     * This method is called from Bluetooth classes and is meant to post an event to the main manager
     * that contains the {@link DeviceInfo} with the associated value
     * @param deviceInfo the {@link DeviceInfo}
     * @param deviceValue the new value as String
     */
    void notifyDeviceInfoReceived(DeviceInfo deviceInfo, String deviceValue){
        Log.d(TAG," Device info returned by the headset "+deviceInfo+ " : "+deviceValue);
        setRequestAsProcessed();

        if(deviceInfo.equals(DeviceInfo.BATTERY) && getCurrentState().equals(BluetoothState.BONDING)){
            updateConnectionState(false); //current state is set to BONDED
            switchToNextConnectionStep();
        } else {
            MbtEventBus.postEvent(new DeviceInfoEvent<>(deviceInfo, deviceValue));
        }
    }

    /**
     * This method is called from Bluetooth classes and is meant to post an event to the main manager
     * that contains the {@link StreamState} new state
     * @param newStreamState the {@link StreamState} new state
     */
    public void notifyStreamStateChanged(StreamState newStreamState) {
        if(newStreamState.equals(StreamState.STOPPED) || newStreamState.equals(StreamState.STARTED))
            asyncOperation.stopWaitingOperation(true);

        setRequestAsProcessed();
        MbtEventBus.postEvent(newStreamState);
    }

    public void notifyNewHeadsetStatus(@NonNull byte[] payload) {
        MbtEventBus.postEvent(new DeviceEvents.RawDeviceMeasure(payload));
    }

    public void requestCurrentConnectedDevice(final SimpleRequestCallback<MbtDevice> callback) {
        MbtEventBus.postEvent(new DeviceEvents.GetDeviceEvent(), new MbtEventBus.Callback<DeviceEvents.PostDeviceEvent>(){
            @Override
            @Subscribe
            public Void onEventCallback(DeviceEvents.PostDeviceEvent device) {
                MbtEventBus.registerOrUnregister(false,this);
                callback.onRequestComplete(device.getDevice());
                return null;
            }
        });
    }

    /**
     * Set the current bluetooth connection state to the value given in parameter
     * and notify the bluetooth manager of this change.
     * This method should be called :
     * - if something went wrong during the connection process
     * - or if the new state does not correspond to the state that follow the current state in chronological order ({@link BluetoothState} enum order)
     * The updateConnectionState(boolean) method with no parameter should be call if nothing went wrong and user wants to continue the connection process
     */
    private void updateConnectionState(BluetoothState state){
        if(state != null && !state.isAudioState() && bluetoothContext != null && (!isConnectionInterrupted || state.equals(BluetoothState.CONNECTION_INTERRUPTED))){
            bluetoothForDataStreaming.notifyConnectionStateChanged(state);
        }
    }

    /**
     * Set the current bluetooth connection state to the value of the next step in chronological order (according to enum order)
     * and notify the bluetooth manager of this change.
     * This method should be called if no error occured.
     */
    public void updateConnectionState(boolean isCompleted){
        BluetoothState nextStep = getCurrentState().getNextConnectionStep();
        if(!isConnectionInterrupted)
            updateConnectionState(nextStep != BluetoothState.IDLE ? nextStep : null);

        if(isCompleted)
            asyncOperation.stopWaitingOperation(false);
    }

    /**
     * Return true if the user has requested connection with an already connected device, false otherwise
     */
    private boolean isAlreadyConnectedToRequestedDevice(String nameDeviceToConnect, MbtDevice deviceConnected){
        return (deviceConnected != null && deviceConnected.getExternalName() != null && deviceConnected.getExternalName().equals(nameDeviceToConnect));
    }

    /**
     * Tells whether or not the end-user device is currently connected to the headset in Low Energy.
     *
     * @return <code>true</code> if connected, <code>false</code> otherwise
     */
    private boolean isDataBluetoothConnected() {
        return bluetoothForDataStreaming.isConnected();
    }

    /**
     * Tells whether or not the end-user device is currently connected to the headset in A2DP.
     * @return <code>true</code> if connected, <code>false</code> otherwise
     */
    private boolean isAudioBluetoothConnected() {
        return bluetoothForAudioStreaming != null && bluetoothForAudioStreaming.isConnected();
    }

    /**
     * Tells whether or not the end-user device is currently connected to the headset both in Low Energy and A2DP.
     * @return <code>true</code> if connected, <code>false</code> otherwise
     */
    public final boolean isConnected() {
        return (bluetoothContext.getConnectAudioIfDeviceCompatible() ? (isDataBluetoothConnected() && isAudioBluetoothConnected()) : isDataBluetoothConnected());
    }

    /**
     * Gets current state according to bluetooth protocol value
     */
    private BluetoothState getCurrentState(){
        return bluetoothForDataStreaming.getCurrentState();
    }

    public String getDeviceNameRequested() {
        return bluetoothContext.getDeviceNameRequested();
    }

    private BluetoothDevice getCurrentDevice() {
        return bluetoothForDataStreaming.currentDevice;

    }

    void disconnectA2DPFromBLE() {
        LogUtils.i(TAG, " disconnect A2dp from ble");
        if(isDataBluetoothConnected() && isAudioBluetoothConnected())
            sendCommand(new DeviceCommands.DisconnectAudio());
    }

    /**
     * Starts a Low Energy connection process if a Melomind is connected for Audio Streaming in A2DP.
     */
    private void connectBLEFromA2DP(@NonNull String newDeviceBleName) {
        LogUtils.i(TAG, "connect BLE from a2dp ");
        if(isAudioBluetoothConnected()){
            BluetoothState currentStateBeforeDisconnection = getCurrentState();
            if(bluetoothForDataStreaming.isConnected() && !((MbtBluetoothLE)bluetoothForDataStreaming).isCurrentDeviceNameEqual(newDeviceBleName)) //Disconnecting another melomind if already one connected in BLE
                bluetoothForDataStreaming.disconnect();

            bluetoothContext.setDeviceNameRequested(newDeviceBleName);
            if(!currentStateBeforeDisconnection.equals(BluetoothState.IDLE)) {
                try {
                    asyncSwitchOperation.waitOperationResult(8000);
                }catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
                    LogUtils.w(TAG, "Exception raised during disconnection "+e);
                }
                MbtEventBus.postEvent(new StartOrContinueConnectionRequestEvent(false, bluetoothContext)); //current state should be IDLE
            }
        }
    }

    private void setRequestAsProcessing(){
        Log.d(TAG, "Processing request");
        requestBeingProcessed = true;
    }

    private void setRequestAsProcessed() {
        setRequestAsProcessed(true);
    }

    private void setRequestAsProcessed(boolean displayLog) {
        if(displayLog)
            LogUtils.d(TAG, "Request processed");
        requestBeingProcessed = false;
    }

    /**
     * Notify the event subscribers when a message/response of the headset device
     * is received by the Bluetooth unit
     */
    public void notifyEventReceived(DeviceCommandEvent eventIdentifier, Object eventData) {
        MbtEventBus.postEvent(new BluetoothResponseEvent(eventIdentifier, eventData));
    }
}
