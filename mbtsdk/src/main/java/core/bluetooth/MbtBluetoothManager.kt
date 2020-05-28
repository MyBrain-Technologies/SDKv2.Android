package core.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.IntegerRes
import androidx.annotation.VisibleForTesting
import command.BluetoothCommand
import command.BluetoothCommands.Mtu
import command.CommandInterface.MbtCommand
import command.DeviceCommand
import command.DeviceCommandEvent
import command.DeviceCommands.*
import command.DeviceStreamingCommands.EegConfig
import command.OADCommands
import config.MbtConfig
import core.BaseModuleManager
import core.bluetooth.BluetoothInterfaces.IDeviceInfoMonitor
import core.bluetooth.lowenergy.MbtBluetoothLE
import core.bluetooth.requests.*
import core.bluetooth.BluetoothProtocol.*
import core.bluetooth.BluetoothState.*
import core.bluetooth.spp.MbtBluetoothSPP
import core.device.DeviceEvents.*
import core.device.model.DeviceInfo
import core.device.model.MbtDevice
import core.device.model.MelomindsQRDataBase
import engine.SimpleRequestCallback
import engine.clientevents.BaseError
import engine.clientevents.BaseErrorEvent
import engine.clientevents.BasicError
import engine.clientevents.ConnectionStateReceiver
import eventbus.MbtEventBus
import eventbus.events.*
import features.MbtDeviceType.*
import features.MbtFeatures
import org.apache.commons.lang.ArrayUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import utils.*
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException

/**
 * Created by Etienne on 08/02/2018.
 *
 * This class contains all necessary methods to manage the Bluetooth communication with the myBrain peripheral devices.
 * - 3 Bluetooth layers are used :
 * - Bluetooth Low Energy protocol is used with Melomind Headset for communication.
 * - Bluetooth SPP protocol which is used for the VPro headset communication.
 * - Bluetooth A2DP is used for Audio stream.
 *
 * We scan first with the Low Energy Scanner as it is more efficient than the classical Bluetooth discovery scanner.
 */
class MbtBluetoothManager(context: Context) : BaseModuleManager(context) {
    private lateinit var bluetoothContext: BluetoothContext
    private lateinit var bluetoothInitializer: BluetoothInitializer
    private lateinit var bluetoothForDataStreaming: MbtDataBluetooth
    private var bluetoothForAudioStreaming: MbtAudioBluetooth? = null
    private var requestBeingProcessed = false
    private var isRequestCompleted = false

    @get:VisibleForTesting
    @set:VisibleForTesting
    var requestThread: RequestThread

    /**
     * Handler object enqueues an action to be performed on a different thread than the main thread
     */
    private var requestHandler: Handler
    private var isConnectionInterrupted = false
    private val asyncOperation = MbtAsyncWaitOperation<Boolean>()
    private val asyncSwitchOperation = MbtAsyncWaitOperation<Boolean>()
    private var connectionRetryCounter = 0

    private val receiver: ConnectionStateReceiver = object : ConnectionStateReceiver() {
        override fun onError(error: BaseError, additionalInfo: String) {}
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                Log.d(TAG, " received intent " + action + " for device " + (device?.name))
                if ((bluetoothForAudioStreaming != null) && bluetoothContext.connectAudioIfDeviceCompatible && (action == BluetoothAdapter.ACTION_STATE_CHANGED)) (bluetoothForAudioStreaming as MbtBluetoothA2DP).resetA2dpProxy(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1))
            }
        }
    }

    companion object {
        private val TAG = MbtBluetoothManager::class.java.simpleName
        private const val MAX_CONNECTION_RETRY = 2
    }

    init {
        //Init thread that will handle messages synchronously. Using HandlerThread looks like it is the best way for CPU consomption as infinite loop in async thread was too heavy for cpu
        requestThread = RequestThread("requestThread", Thread.MAX_PRIORITY)
        requestThread.start()
        requestHandler = Handler(requestThread.looper)
        BroadcastUtils.registerReceiverIntents(context, receiver, BluetoothAdapter.ACTION_STATE_CHANGED)
    }

    /**
     * This class is a specific thread that will handle all bluetooth operations. Bluetooth operations
     * are synchronous, meaning two or more operations can't be run simultaneously. This [HandlerThread]
     * extended class is able to hold pending operations.
     */
    inner class RequestThread(name: String?, priority: Int) : HandlerThread(name, priority) {
        override fun onLooperPrepared() {
            super.onLooperPrepared()
        }

        /**
         * Checks the subclass type of [BluetoothRequests] and handles the correct method/action to perform.
         *
         * @param request the [BluetoothRequests] request to execute.
         */
        fun parseRequest(request: BluetoothRequests) {

            //BluetoothRequests request = pendingRequests.remove();
            //disconnect request doesn't need to be in "queue" as it is top priority
            Log.d(TAG, "Parse request $request")
            while (requestBeingProcessed);
            setRequestAsProcessing()
            if (request is StartOrContinueConnectionRequestEvent) {
                initBluetoothParameters(request)
                startOrContinueConnectionOperation(request.isClientUserRequest)
            } else if (request is ReadRequestEvent) {
                startReadOperation(request.deviceInfo)
            } else if (request is DisconnectRequestEvent) {
                if (request.isInterrupted) cancelPendingConnection(request.isInterrupted) else disconnectAllBluetooth(true)
            } else if (request is StreamRequestEvent) {
                if (request.isStart) {
                    startStreamOperation(request.monitorDeviceStatus())
                } else if (request.stopStream()) stopStreamOperation() else setRequestAsProcessed()
            } else if (request is CommandRequestEvent) {
                sendCommand(request.command)
            }
        }

        private fun initBluetoothParameters(event: StartOrContinueConnectionRequestEvent) {
            bluetoothInitializer = BluetoothInitializer()
            bluetoothContext = BluetoothContext(
                    mContext,
                    event.typeOfDeviceRequested,
                    event.connectAudioIfDeviceCompatible(),
                    event.nameOfDeviceRequested,
                    event.qrCodeOfDeviceRequested,
                    event.mtu)
            if (event.isClientUserRequest && !::bluetoothForDataStreaming.isInitialized) {
                if ((bluetoothContext.deviceTypeRequested == MELOMIND)) {
                    bluetoothForDataStreaming = MbtBluetoothLE(mContext, this@MbtBluetoothManager)
                    if (bluetoothContext.connectAudioIfDeviceCompatible) bluetoothForAudioStreaming = MbtBluetoothA2DP(mContext, this@MbtBluetoothManager)
                } else if ((bluetoothContext.deviceTypeRequested == VPRO)) bluetoothForDataStreaming = MbtBluetoothSPP(mContext, this@MbtBluetoothManager)
            }
        }

        /**
         * This method do the following operations:
         * - 1) Check the prerequisites to ensure that the connection can be performed
         * - 2) Scan for [BluetoothDevice] filtering on deviceName. The scan is performed by LE scanner if the device is LE compatible. Otherwise, the discovery scan is performed instead.
         * - 3) Perform the BLE/SPP connection operation if scan resulted in a found device
         * - 4) Discovering services once the headset is connected.
         * - 5) Reading the device info (firmware version, hardware version, serial number, model number) once the services has been discovered
         * - 6) Bond the headset if the firmware version supports it (version > 1.6.7)
         * - 7) Send the QR code number to the headset if it doesn't know its own value and if the firmware version supports it (version > 1.7.1)
         * - 8) Connect audio in A2dp is the user requested it
         */
        private fun startOrContinueConnectionOperation(isClientUserRequest: Boolean) {
            if (isClientUserRequest) isConnectionInterrupted = false
            if (!isConnectionInterrupted) {
                LogUtils.d(TAG, "State is $currentState")
                when (currentState) {
                    IDLE, DATA_BT_DISCONNECTED -> getReadyForBluetoothOperation()
                    READY_FOR_BLUETOOTH_OPERATION -> startScan()
                    DEVICE_FOUND, DATA_BT_CONNECTING -> startConnectionForDataStreaming()
                    DATA_BT_CONNECTION_SUCCESS -> startDiscoveringServices()
                    DISCOVERING_SUCCESS -> startReadingDeviceInfo(DeviceInfo.FW_VERSION) // read all device info (except battery) : first device info to read is firmware version
                    READING_FIRMWARE_VERSION_SUCCESS -> startReadingDeviceInfo(DeviceInfo.HW_VERSION) // read next device info : second device info to read is hardware version
                    READING_HARDWARE_VERSION_SUCCESS -> startReadingDeviceInfo(DeviceInfo.SERIAL_NUMBER) // read next device info : third device info to read is serial number (device ID)
                    READING_SERIAL_NUMBER_SUCCESS -> startReadingDeviceInfo(DeviceInfo.MODEL_NUMBER) // read next device info : fourth device info to read is model number
                    READING_SUCCESS -> startBonding()
                    BONDED -> changeMTU()
                    BT_PARAMETERS_CHANGED -> startSendingExternalName()
                    CONNECTED -> startConnectionForAudioStreaming()
                    else -> setRequestAsProcessed()
                }
            } else setRequestAsProcessed()
        }
    }

    @VisibleForTesting
    fun setRequestHandler(requestHandler: Handler) {
        this.requestHandler = requestHandler
    }

    @VisibleForTesting
    fun setBluetoothForDataStreaming(bluetoothForDataStreaming: MbtBluetoothLE) {
        this.bluetoothForDataStreaming = bluetoothForDataStreaming
    }

    private fun switchToNextConnectionStep() {
        setRequestAsProcessed()
        if (!currentState.isAFailureState && !isConnectionInterrupted && currentState != IDLE) {  //if nothing went wrong during the current step of the connection process, we continue the process
            onNewBluetoothRequest(StartOrContinueConnectionRequestEvent(false, bluetoothContext))
        }
    }

    /**
     * As the Bluetooth scan requires access to the mobile device Location,
     * the Bluetooth Manager must always check this prerequisite before starting any connection operation by calling the isLocationDisabledOrNotGranted() method.
     * @return true if a headset is already connected in BLE and Audio (if Audio has been enabled)
     * or if no headset is connected.
     */
    private fun isAlreadyConnected(currentConnectedDevice: MbtDevice): Boolean {
        if (isConnected) {
            if (!isAlreadyConnectedToRequestedDevice(bluetoothContext.deviceNameRequested, currentConnectedDevice)) {
                updateConnectionState(ANOTHER_DEVICE_CONNECTED)
            } else updateConnectionState(CONNECTED_AND_READY)
            return true
        }
        return false
    }

    /**
     * Check the bluetooth prerequisites before starting any bluetooth operation.
     * The started Bluetooth connection process is stopped if the prerequisites are not valid.
     */
    private fun getReadyForBluetoothOperation() {
        connectionRetryCounter = 0
        //Request sent to the BUS in order to get device from the device manager : the BUS should return a null object if it's the first connection, or return a non null object if the user requests connection whereas a headset is already connected
        LogUtils.i(TAG, "Checking Bluetooth Prerequisites and initialize")
        requestCurrentConnectedDevice(SimpleRequestCallback<MbtDevice> { device ->
            if (device != null && isAlreadyConnected(device)) // assert that headset is not already connected
                return@SimpleRequestCallback
            val state = bluetoothInitializer.getBluetoothPrerequisitesState((bluetoothContext)) //check that Bluetooth is on, Location is on, Location permission is granted

            if (state != READY_FOR_BLUETOOTH_OPERATION) { //assert that Bluetooth is on, Location is on, Location permission is granted
                updateConnectionState(state)
                return@SimpleRequestCallback
            }

            if (bluetoothForAudioStreaming != null && bluetoothForAudioStreaming is MbtBluetoothA2DP) (bluetoothForAudioStreaming as MbtBluetoothA2DP).initA2dpProxy() //initialization to check if a Melomind is already connected in A2DP : as the next step is the scanning, the SDK is able to filter on the name of this device
            if ((currentState == IDLE)) updateConnectionState(false) //current state is set to READY_FOR_BLUETOOTH_OPERATION
            switchToNextConnectionStep()
        })
    }


    /**
     * This method starts a bluetooth scan operation, loooking for a single device by filtering on its name.
     * This method is asynchronous.
     * If the device to scan is a Melomind, the bluetooth LE scanner is invoked.
     * Otherwise, the classic discovery scan is invoked.
     *
     * Requires [Manifest.permission.ACCESS_FINE_LOCATION] or [Manifest.permission.ACCESS_COARSE_LOCATION] permission if a GPS sensor is available.
     * If permissions are not given and/or bluetooth device is not Le compatible, discovery scan is started.
     * The started Bluetooth connection process is stopped if the prerequisites are not valid.
     */
    private fun startScan() {
        var newState: BluetoothState = SCAN_FAILURE
        asyncOperation.tryOperation({ bluetoothForDataStreaming.startScan() },
            BaseErrorEvent { exception, _ ->
                if (exception is TimeoutException) newState = SCAN_TIMEOUT //stop the current Bluetooth connection process
                else if (exception is CancellationException) asyncOperation.resetWaitingOperation() },
            { stopScan() },
            MbtConfig.getBluetoothScanTimeout())
//        try {
//            AsyncUtils.executeAsync { bluetoothForDataStreaming.startScan() }
//            asyncOperation.waitOperationResult(MbtConfig.getBluetoothScanTimeout())
//        } catch (e: Exception) {
//            if (e is TimeoutException)
//                newState = SCAN_TIMEOUT //stop the current Bluetooth connection process
//            else if (e is CancellationException)
//                asyncOperation.resetWaitingOperation()
//            LogUtils.w(TAG, "Exception raised during scanning : \n $e")
//        } finally {
//            stopScan()
//        }
        if ((currentState == SCAN_STARTED)) ////at this point : current state should be DEVICE_FOUND if scan succeeded
            updateConnectionState(newState) //scan failure or timeout
        switchToNextConnectionStep()
    }

    /**
     * This method stops the currently running bluetooth scan, either Le scan or discovery scan
     */
    private fun stopScan() {
        asyncOperation.stopWaitingOperation(null)
        bluetoothForDataStreaming.stopScan()
    }

    /**
     * This connection step is the BLE (Melomind) /SPP (Vpro) connection to the found device
     * It allows communication between the headset device and the SDK for data streaming (EEG, battery level, etc.)
     */
    private fun startConnectionForDataStreaming() {
        LogUtils.i(TAG, "start connection data streaming")
        try {
            AsyncUtils.Companion.executeAsync { connect(bluetoothContext.deviceTypeRequested.protocol) }
            asyncOperation.waitOperationResult(MbtConfig.getBluetoothConnectionTimeout()) // blocked until the futureOperation.complete() is called or until timeout
        } catch (e: Exception) {
            LogUtils.w(TAG, "Exception raised during connection : \n $e")
            if (e is CancellationException) asyncOperation.resetWaitingOperation()
        } finally {
            asyncOperation.stopWaitingOperation(null)
        }
        if (currentState != CONNECTED_AND_READY && currentState != DATA_BT_CONNECTION_SUCCESS && currentState != IDLE) updateConnectionState(CONNECTION_FAILURE)
        switchToNextConnectionStep()
    }

    private fun connect(protocol: BluetoothProtocol) {
        isConnectionInterrupted = false // resetting the flag when starting a new connection
        val isConnectionSuccessful = when (protocol) {
            LOW_ENERGY, SPP -> bluetoothForDataStreaming.connect(mContext, currentDevice)
            A2DP -> bluetoothForAudioStreaming?.connect(mContext, currentDevice)?:false
        }
        if (isConnectionSuccessful) {
            if (protocol == A2DP) {
                if (isAudioBluetoothConnected) asyncOperation.stopWaitingOperation(false)
            } else updateConnectionState(isDataBluetoothConnected)
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
    private fun startDiscoveringServices() {
        if (bluetoothForDataStreaming is MbtBluetoothLE) {
            LogUtils.i(TAG, "start discovering services ")
            if (currentState.ordinal >= DATA_BT_CONNECTION_SUCCESS.ordinal) { //if connection is in progress and BLE is at least connected, we can discover services
                try {
                    AsyncUtils.Companion.executeAsync { (bluetoothForDataStreaming as MbtBluetoothLE).discoverServices() }
                    asyncOperation.waitOperationResult(MbtConfig.getBluetoothDiscoverTimeout())
                } catch (e: Exception) {
                    if (e is CancellationException) asyncOperation.resetWaitingOperation()
                    LogUtils.i(TAG, "Exception raised discovery connection : \n $e")

                } finally {
                    asyncOperation.stopWaitingOperation(null)
                }
                if (currentState != DISCOVERING_SUCCESS) { ////at this point : current state should be DISCOVERING_SUCCESS if discovery succeeded
                    updateConnectionState(DISCOVERING_FAILURE)
                }
                switchToNextConnectionStep()
            }
        }
    }

    private fun startReadingDeviceInfo(deviceInfo: DeviceInfo) {
        updateConnectionState(false) //current state is set to READING_FIRMWARE_VERSION or READING_HARDWARE_VERSION or READING_SERIAL_NUMBER or READING_MODEL_NUMBER
        try {
            AsyncUtils.Companion.executeAsync { startReadOperation(deviceInfo) }
            asyncOperation.waitOperationResult(MbtConfig.getBluetoothReadingTimeout())
        } catch (e: Exception) {
            LogUtils.w(TAG, "Exception raised during reading device info : \n $e")
            if (e is CancellationException) asyncOperation.resetWaitingOperation()
        } finally {
            asyncOperation.stopWaitingOperation(null)
        }
        when (deviceInfo) {
            DeviceInfo.FW_VERSION -> if (currentState != READING_FIRMWARE_VERSION_SUCCESS) updateConnectionState(READING_FAILURE)//at this point : current state should be READING...SUCCESS if reading succeeded
            DeviceInfo.HW_VERSION -> if (currentState != READING_HARDWARE_VERSION_SUCCESS) updateConnectionState(READING_FAILURE) //at this point : current state should be READING...SUCCESS if reading succeeded
            DeviceInfo.SERIAL_NUMBER -> if (currentState != READING_SERIAL_NUMBER_SUCCESS) updateConnectionState(READING_FAILURE)//at this point : current state should be READING...SUCCESS if reading succeeded
            DeviceInfo.MODEL_NUMBER -> if (currentState != READING_SUCCESS) updateConnectionState(READING_FAILURE)//at this point : current state should be READING...SUCCESS if reading succeeded

        }
        switchToNextConnectionStep()
    }

    /**
     * If the [request][BluetoothRequests] is a [ReadRequestEvent] event, this method
     * is called to parse which read operation is to be executed according to the [DeviceInfo].
     * @param deviceInfo the [DeviceInfo] info that determine which read to operation to execute.
     */
    fun startReadOperation(deviceInfo: DeviceInfo) {
        val hasOperationFailed = when (deviceInfo) {
            DeviceInfo.BATTERY -> bluetoothForDataStreaming.readBattery() //Initiates a read battery operation on this correct BtProtocol.
            DeviceInfo.FW_VERSION -> (bluetoothForDataStreaming as IDeviceInfoMonitor).readFwVersion() //Initiates a read firmware version operation on this correct BtProtocol
            DeviceInfo.HW_VERSION -> (bluetoothForDataStreaming as IDeviceInfoMonitor).readHwVersion() //Initiates a read hardware version operation on this correct BtProtocol
            DeviceInfo.SERIAL_NUMBER -> (bluetoothForDataStreaming as IDeviceInfoMonitor).readSerialNumber() //Initiates a read serial number operation on this correct BtProtocol
            DeviceInfo.MODEL_NUMBER -> (bluetoothForDataStreaming as IDeviceInfoMonitor).readModelNumber() //Initiates a read model number operation on this correct BtProtocol
            else -> {
                false
            }
        }
        if (hasOperationFailed) {
            setRequestAsProcessed()
            MbtEventBus.postEvent(DeviceInfoEvent<Any>(deviceInfo, null))
        }
    }

    private fun startBonding() {
        if (bluetoothForDataStreaming is MbtBluetoothLE) {
            LogUtils.i(TAG, "start bonding if supported")
            requestCurrentConnectedDevice(SimpleRequestCallback<MbtDevice> { device ->
                val isBondingSupported = VersionHelper(device.firmwareVersion.toString()).isValidForFeature(VersionHelper.Feature.BLE_BONDING)
                if (isBondingSupported) { //if firmware version bonding is higher than 1.6.7, the bonding is launched
                    try {
                        AsyncUtils.Companion.executeAsync(Runnable {
                            if (currentState == BONDING) //avoid double bond if several requestCurrentConnectedDevice are called at the same moment
                                return@Runnable
                            (bluetoothForDataStreaming as MbtBluetoothLE).bond()
                        })
                        asyncOperation.waitOperationResult(MbtConfig.getBluetoothBondingTimeout())
                    } catch (e: Exception) {
                        LogUtils.w(TAG, "Exception raised during bonding : \n $e")
                        if (e is CancellationException) asyncOperation.resetWaitingOperation()
                    } finally {
                        asyncOperation.stopWaitingOperation(null)
                    }
                } else  //if firmware version bonding is older than 1.6.7, the connection process is considered completed
                    updateConnectionState(CONNECTED)
            })
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if ((currentState == BONDING)) { //at this point : current state should be BONDED if bonding succeeded
                updateConnectionState(BONDING_FAILURE)
            }
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            switchToNextConnectionStep()
        }
    }

    private fun startSendingExternalName() {
        LogUtils.i(TAG, "start sending QR code if supported")
        requestCurrentConnectedDevice(SimpleRequestCallback<MbtDevice> { device ->
            LogUtils.d(TAG, "device $device")
            updateConnectionState(true) //current state is set to QR_CODE_SENDING
            if (((device.serialNumber != null) && (device.externalName != null) && ((device.externalName == MbtFeatures.MELOMIND_DEVICE_NAME) || device.externalName?.length == MbtFeatures.DEVICE_QR_CODE_LENGTH - 1) //send the QR code found in the database if the headset do not know its own QR code
                            && VersionHelper(device.firmwareVersion.toString()).isValidForFeature(VersionHelper.Feature.REGISTER_EXTERNAL_NAME))) {
                AsyncUtils.Companion.executeAsync {
                    val externalName = MelomindsQRDataBase(mContext, false)[device.serialNumber]
                    sendCommand(UpdateExternalName(externalName))
                }
            }
            updateConnectionState(true) //current state is set to CONNECTED in any case (success or failure) the connection process is completed and the SDK consider that everything is ready for any operation (for example ready to acquire EEG data)

        })
        switchToNextConnectionStep()
    }

    private fun startConnectionForAudioStreaming() {
        if (bluetoothContext.connectAudioIfDeviceCompatible && !isAudioBluetoothConnected) {
            LogUtils.i(TAG, "start connection audio streaming")
            isRequestCompleted = false
            requestCurrentConnectedDevice(SimpleRequestCallback<MbtDevice> { device ->
                if (device == null) return@SimpleRequestCallback
                if (!isRequestCompleted) {
                    isRequestCompleted = true
                    val connectionFromBleAvailable = VersionHelper(device.firmwareVersion.toString()).isValidForFeature(VersionHelper.Feature.A2DP_FROM_HEADSET)
                    try {
                        AsyncUtils.Companion.executeAsync {
                            if (connectionFromBleAvailable) //A2DP cannot be connected from BLE if BLE connection state is not CONNECTED_AND_READY or CONNECTED
                                sendCommand(ConnectAudio()) else { // if connectA2DPFromBLE failed or is not supported by the headset firmware version
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || ((bluetoothForAudioStreaming as MbtBluetoothA2DP?)?.isPairedDevice(currentDevice) == true)) connect(A2DP) else notifyConnectionStateChanged(AUDIO_CONNECTION_UNSUPPORTED)
                            }
                        }
                        asyncOperation.waitOperationResult(MbtConfig.getBluetoothA2DpConnectionTimeout())
                    } catch (e: Exception) {
                        LogUtils.w(TAG, "Exception raised during audio connection : \n $e")
                        if (e is CancellationException) asyncOperation.resetWaitingOperation()
                    } finally {
                        asyncOperation.stopWaitingOperation(null)
                    }
                }
            })
            if (bluetoothForAudioStreaming?.isConnected != true) {
                if (connectionRetryCounter < MAX_CONNECTION_RETRY) {
                    connectionRetryCounter++
                    try {
                        Thread.sleep(200)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    startConnectionForAudioStreaming()
                } else {
                    connectionRetryCounter = 0
                    bluetoothForAudioStreaming?.notifyConnectionStateChanged(CONNECTION_FAILURE) //at this point : current state should be AUDIO_CONNECTED if audio connection succeeded
                    bluetoothForDataStreaming.notifyConnectionStateChanged(CONNECTION_FAILURE)
                }
            }
        }
        if (isConnected) updateConnectionState(true) //BLE and audio (if SDK user requested it) are connected so the client is notified that the device is fully connected
        setRequestAsProcessed()
        LogUtils.i(TAG, "connection completed")
    }

    /**
     * Command sent from the SDK to the connected headset
     * in order to change its Maximum Transmission Unit
     * (maximum size of the data sent by the headset to the SDK).
     */
    private fun changeMTU() {
        updateConnectionState(true) //current state is set to CHANGING_BT_PARAMETERS
        sendCommand(Mtu(bluetoothContext.mtu))
    }

    /**
     * This method handle a single command in order to
     * reconfigure some headset's parameters
     * or get values stored by the headset
     * or ask the headset to perform an action.
     * The command's parameters are bundled in a [instance][DeviceCommand]
     * that can provide a nullable response callback.
     * All method inside are blocking.
     * @param command is the [DeviceCommand] object that defines the type of command to send
     * and the asociated command parameters.
     */
    private fun sendCommand(command: MbtCommand<*>) {
        bluetoothForDataStreaming.sendCommand(command)
    }

    /**
     * Initiates the acquisition of EEG data. This method chooses between the correct BtProtocol.
     * If there is already a streaming session in progress, nothing happens and the method returns silently.
     */
    private fun startStreamOperation(enableDeviceStatusMonitoring: Boolean) {
        Log.d(TAG, "Bluetooth Manager starts streaming")
        if (!bluetoothForDataStreaming.isConnected) {
            notifyStreamStateChanged(StreamState.DISCONNECTED)
            setRequestAsProcessed()
            return
        }
        if (bluetoothForDataStreaming.isStreaming) {
            setRequestAsProcessed()
            return
        }
        if (enableDeviceStatusMonitoring && bluetoothForDataStreaming is MbtBluetoothLE) (bluetoothForDataStreaming as MbtBluetoothLE).activateDeviceStatusMonitoring()
        try {
            AsyncUtils.Companion.executeAsync(Runnable {
                if (!bluetoothForDataStreaming.startStream()) {
                    MbtEventBus.postEvent(StreamState.FAILED)
                }
            })
            val startSucceeded: Boolean? = asyncOperation.waitOperationResult(6000) as Boolean
            if (startSucceeded != null && !startSucceeded) MbtEventBus.postEvent(StreamState.FAILED)
            setRequestAsProcessed()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Initiates the acquisition of EEG data from the correct BtProtocol
     * If there is no streaming session in progress, nothing happens and the method returns silently.
     */
    private fun stopStreamOperation() {
        Log.d(TAG, "Bluetooth Manager stops streaming")
        if (!bluetoothForDataStreaming.isStreaming) {
            setRequestAsProcessed()
            return
        }
        try {
            AsyncUtils.Companion.executeAsync(Runnable {
                if (!bluetoothForDataStreaming.stopStream()) {
                    asyncOperation.stopWaitingOperation(false)
                    bluetoothForDataStreaming.notifyStreamStateChanged(StreamState.FAILED)
                }
            })
            val stopSucceeded: Boolean? = asyncOperation.waitOperationResult(6000) as Boolean
            if (stopSucceeded != null && !stopSucceeded) bluetoothForDataStreaming.notifyStreamStateChanged(StreamState.FAILED)
        } catch (e: Exception) {
            e.printStackTrace()
            bluetoothForDataStreaming.notifyStreamStateChanged(StreamState.FAILED)
        } finally {
            setRequestAsProcessed()
        }
    }

    /**
     * Start the disconnect operation on the currently connected bluetooth device according to the [BluetoothProtocol] currently used.
     */
    private fun disconnect(protocol: BluetoothProtocol) {
        if (isAudioBluetoothConnected || isDataBluetoothConnected || currentState.isConnectionInProgress) {
            when (protocol) {
                LOW_ENERGY, SPP -> bluetoothForDataStreaming.disconnect()
                A2DP -> bluetoothForAudioStreaming?.disconnect()
            }
        }
    }

    fun disconnectAllBluetooth(disconnectAudioIfConnected: Boolean) {
        LogUtils.i(TAG, "Disconnect all bluetooth")
        if (isAudioBluetoothConnected && disconnectAudioIfConnected) disconnect(A2DP)
        bluetoothContext.deviceTypeRequested.protocol?.let { disconnect(it) }
    }

    /**
     * Stops current pending connection according to its current [state][BluetoothState].
     * It can be either stop scan or connection process interruption
     */
    private fun cancelPendingConnection(isClientUserAbortion: Boolean) {
        LogUtils.i(TAG, "cancelling pending connection")
        setRequestAsProcessed()
        disconnectAllBluetooth(!asyncSwitchOperation.isWaiting)
        if (isClientUserAbortion) {
            isConnectionInterrupted = true
            updateConnectionState(CONNECTION_INTERRUPTED)
        }
        asyncOperation.stopWaitingOperation(null)
    }

    /**
     * Posts a BluetoothEEGEvent event to the bus so that MbtEEGManager can handle raw EEG data received
     * @param data the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     */
    fun handleDataAcquired(data: ByteArray) {
        MbtEventBus.postEvent(BluetoothEEGEvent(data)) //MbtEEGManager will convert data from raw packets to eeg values
        setRequestAsProcessed(false)
    }

    /**
     * Unregister the MbtBluetoothManager class from the bus to avoid memory leak
     */
    fun deinit() {
        MbtEventBus.registerOrUnregister(false, this)
    }

    /**
     * This method is called from Bluetooth classes and is meant to post an event to the main manager
     * that contains the new [BluetoothState]
     * @param newState the new [BluetoothState]
     */
    fun notifyConnectionStateChanged(newState: BluetoothState) {
        setRequestAsProcessed()
        when (newState) {
            DATA_BT_DISCONNECTED -> notifyDataBluetoothDisconnected() //a disconnection occurred
            AUDIO_BT_DISCONNECTED -> notifyAudioBluetoothDisconnected(newState)
            AUDIO_BT_CONNECTION_SUCCESS -> notifyAudioBluetoothConnected(newState)
            JACK_CABLE_CONNECTED -> notifyJackCableConnected()
            DEVICE_FOUND -> notifyDeviceFound(newState)
        }
        requestCurrentConnectedDevice(SimpleRequestCallback {
            MbtEventBus.postEvent(ConnectionStateEvent(newState, it)) //This event is sent to MbtManager for user notifications and to MbtDeviceManager
        })
    }

    private fun notifyDataBluetoothDisconnected() {
        if (asyncSwitchOperation.isWaiting) asyncSwitchOperation.stopWaitingOperation(false) //a new a2dp connection was detected while an other headset was connected : here the last device has been well disconnected so we can connect BLE from A2DP
        else cancelPendingConnection(false)
    }

    private fun notifyAudioBluetoothDisconnected(newState: BluetoothState) {
        bluetoothForAudioStreaming?.notifyConnectionStateChanged(newState, false)
        MbtEventBus.postEvent(AudioDisconnectedDeviceEvent())
    }

    private fun notifyAudioBluetoothConnected(newState: BluetoothState) {
        if (bluetoothForAudioStreaming == null) { return }

        bluetoothForAudioStreaming?.notifyConnectionStateChanged(newState, false)
        asyncOperation.stopWaitingOperation(false)
        if (bluetoothForAudioStreaming?.getCurrentDevice() != null && bluetoothForDataStreaming is MbtBluetoothLE) {
            val bleDeviceName = (bluetoothForDataStreaming as MbtBluetoothLE).getBleDeviceNameFromA2dp(bluetoothForAudioStreaming?.getCurrentDevice()?.name, mContext)
            if (!isDataBluetoothConnected || !(bluetoothForDataStreaming as MbtBluetoothLE).isCurrentDeviceNameEqual(bleDeviceName)) connectBLEFromA2DP(bleDeviceName)
            MbtEventBus.postEvent(AudioConnectedDeviceEvent(bluetoothForAudioStreaming?.getCurrentDevice()))
        }

    }

    private fun notifyJackCableConnected() {
        if (asyncOperation.isWaiting) asyncOperation.stopWaitingOperation(false)
    }

    private fun notifyDeviceFound(newState: BluetoothState) {
        MbtEventBus.postEvent(ConnectionStateEvent(newState, currentDevice, bluetoothContext.deviceTypeRequested))
        if (bluetoothContext.connectAudioIfDeviceCompatible) {
            if (bluetoothForAudioStreaming == null) {
                bluetoothForAudioStreaming = MbtBluetoothA2DP(mContext, this@MbtBluetoothManager)
            } else if (bluetoothForAudioStreaming?.currentDevice != null) {
                MbtEventBus.postEvent(AudioConnectedDeviceEvent(bluetoothForAudioStreaming?.currentDevice))
            }
        }
    }

    /**
     * This method is called from Bluetooth classes and is meant to post an event to the main manager
     * that contains the [DeviceInfo] with the associated value
     * @param deviceInfo the [DeviceInfo]
     * @param deviceValue the new value as String
     */
    fun notifyDeviceInfoReceived(deviceInfo: DeviceInfo, deviceValue: String) {
        Log.d(TAG, " Device info returned by the headset $deviceInfo : $deviceValue")
        setRequestAsProcessed()
        if ((deviceInfo == DeviceInfo.BATTERY) && (currentState == BONDING)) {
            updateConnectionState(false) //current state is set to BONDED
            switchToNextConnectionStep()
        } else {
            MbtEventBus.postEvent(DeviceInfoEvent(deviceInfo, deviceValue))
        }
    }

    /**
     * This method is called from Bluetooth classes and is meant to post an event to the main manager
     * that contains the [StreamState] new state
     * @param newStreamState the [StreamState] new state
     */
    fun notifyStreamStateChanged(newStreamState: StreamState) {
        if ((newStreamState == StreamState.STOPPED) || (newStreamState == StreamState.STARTED)) asyncOperation.stopWaitingOperation(true)
        setRequestAsProcessed()
        MbtEventBus.postEvent(newStreamState)
    }

    fun notifyNewHeadsetStatus(payload: ByteArray) {
        MbtEventBus.postEvent(RawDeviceMeasure(payload))
    }

    /**
     * Notify the DeviceManager and the EEGManager that the headset returned its stored configuration
     */
    private fun notifyDeviceConfigReceived(returnedConfig: Array<Byte>) {
        requestCurrentConnectedDevice(SimpleRequestCallback<MbtDevice> { device ->
            MbtEventBus.postEvent(EEGConfigEvent(device, returnedConfig))
        })
    }

    /**
     * Notify a command response has been received from the headset
     * @param command is the corresponding type of command
     */
    fun notifyResponseReceived(response: Any?, command: MbtCommand<*>?) {
        LogUtils.d(TAG, "Received response from device : $response")
        if (command is DeviceCommand<*, *>) notifyDeviceResponseReceived(response, command) else if (command is BluetoothCommand<*, *>) onBluetoothResponseReceived(response, command)
        setRequestAsProcessed()
    }

    /**
     * Notify the event subscribers when a message/response of the headset device
     * is received by the Bluetooth unit
     */
    fun notifyEventReceived(eventIdentifier: DeviceCommandEvent?, eventData: Any?) {
        MbtEventBus.postEvent(BluetoothResponseEvent(eventIdentifier, eventData))
    }

    private fun notifyDeviceResponseReceived(response: Any?, command: DeviceCommand<*, *>) {
        if (response != null) {
            when (command) {
                is EegConfig -> notifyDeviceConfigReceived(ArrayUtils.toObject(response as ByteArray?))

                is UpdateSerialNumber -> (response as ByteArray?)?.let { String(it) }?.let { notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER, it) }

                is GetDeviceInfo -> {
                    notifyDeviceInfoReceived(DeviceInfo.FW_VERSION, String(ArrayUtils.subarray(response as ByteArray?, 0, MbtBluetoothSPP.VERSION_NB_BYTES)))
                    notifyDeviceInfoReceived(DeviceInfo.HW_VERSION, String(ArrayUtils.subarray(response as ByteArray?, MbtBluetoothSPP.VERSION_NB_BYTES, MbtBluetoothSPP.VERSION_NB_BYTES + MbtBluetoothSPP.VERSION_NB_BYTES)))
                    notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER, String(ArrayUtils.subarray(response as ByteArray?, MbtBluetoothSPP.VERSION_NB_BYTES + MbtBluetoothSPP.VERSION_NB_BYTES, MbtBluetoothSPP.VERSION_NB_BYTES + MbtBluetoothSPP.VERSION_NB_BYTES + MbtBluetoothSPP.SERIAL_NUMBER_NB_BYTES)))
                }
                is UpdateExternalName ->
                    (response as ByteArray?)?.let { String(it) }?.let { notifyDeviceInfoReceived(DeviceInfo.MODEL_NUMBER, it) }

                is GetBattery ->
                    (response as Int?)?.toString()?.let { notifyDeviceInfoReceived(DeviceInfo.BATTERY, it) }

                is OADCommands -> {
                    updateConnectionState(UPGRADING)
                    bluetoothForAudioStreaming?.currentState = UPGRADING
                    notifyEventReceived(command.identifier, response)
                }
            }
        }
    }

    fun requestCurrentConnectedDevice(callback: SimpleRequestCallback<MbtDevice>) {
        MbtEventBus.postEvent(GetDeviceEvent(), object : MbtEventBus.Callback<PostDeviceEvent> {
            @Subscribe
            override fun onEventCallback(event: PostDeviceEvent): Unit? {
                MbtEventBus.registerOrUnregister(false, this)
                callback.onRequestComplete(event.device)
                return null
            }
        })
    }

    /**
     * Set the current bluetooth connection state to the value given in parameter
     * and notify the bluetooth manager of this change.
     * This method should be called :
     * - if something went wrong during the connection process
     * - or if the new state does not correspond to the state that follow the current state in chronological order ([BluetoothState] enum order)
     * The updateConnectionState(boolean) method with no parameter should be call if nothing went wrong and user wants to continue the connection process
     */
    private fun updateConnectionState(state: BluetoothState?) {
        if ((state != null) && !state.isAudioState && (!isConnectionInterrupted || (state == CONNECTION_INTERRUPTED))) {
            bluetoothForDataStreaming.notifyConnectionStateChanged(state)
        }
    }

    /**
     * Set the current bluetooth connection state to the value of the next step in chronological order (according to enum order)
     * and notify the bluetooth manager of this change.
     * This method should be called if no error occured.
     */
    fun updateConnectionState(isCompleted: Boolean) {
        val nextStep = currentState.nextConnectionStep
        if (!isConnectionInterrupted) updateConnectionState(if (nextStep != IDLE) nextStep else null)
        if (isCompleted) asyncOperation.stopWaitingOperation(false)
    }

    /**
     * Return true if the user has requested connection with an already connected device, false otherwise
     */
    private fun isAlreadyConnectedToRequestedDevice(nameDeviceToConnect: String?, deviceConnected: MbtDevice?): Boolean {
        return ((deviceConnected != null) && (deviceConnected.externalName != null) && (deviceConnected.externalName == nameDeviceToConnect))
    }

    /**
     * Tells whether or not the end-user device is currently connected to the headset in Low Energy.
     *
     * @return `true` if connected, `false` otherwise
     */
    private val isDataBluetoothConnected: Boolean
        get() = bluetoothForDataStreaming.isConnected

    /**
     * Tells whether or not the end-user device is currently connected to the headset in A2DP.
     * @return `true` if connected, `false` otherwise
     */
    private val isAudioBluetoothConnected: Boolean
        get() = bluetoothForAudioStreaming?.isConnected ?: false

    /**
     * Tells whether or not the end-user device is currently connected to the headset both in Low Energy and A2DP.
     * @return `true` if connected, `false` otherwise
     */
    val isConnected: Boolean
        get() = (if (bluetoothContext.connectAudioIfDeviceCompatible)
            (isDataBluetoothConnected && isAudioBluetoothConnected)
        else
            isDataBluetoothConnected)

    /**
     * Gets current state according to bluetooth protocol value
     */
    private val currentState: BluetoothState
        get() = bluetoothForDataStreaming.currentState

    val deviceNameRequested: String?
        get() = bluetoothContext.deviceNameRequested

    private val currentDevice: BluetoothDevice
        get() = bluetoothForDataStreaming.currentDevice

    fun disconnectA2DPFromBLE() {
        LogUtils.i(TAG, " disconnect A2dp from ble")
        if (isDataBluetoothConnected && isAudioBluetoothConnected) sendCommand(DisconnectAudio())
    }

    /**
     * Starts a Low Energy connection process if a Melomind is connected for Audio Streaming in A2DP.
     */
    private fun connectBLEFromA2DP(newDeviceBleName: String) {
        LogUtils.i(TAG, "connect BLE from a2dp ")
        if (isAudioBluetoothConnected) {
            val currentStateBeforeDisconnection = currentState
            if (bluetoothForDataStreaming.isConnected && !(bluetoothForDataStreaming as MbtBluetoothLE).isCurrentDeviceNameEqual(newDeviceBleName)) //Disconnecting another melomind if already one connected in BLE
                bluetoothForDataStreaming.disconnect()
            bluetoothContext.deviceNameRequested = newDeviceBleName
            if (currentStateBeforeDisconnection != IDLE) {
                try {
                    asyncSwitchOperation.waitOperationResult(8000)
                } catch (e: Exception) {
                    LogUtils.w(TAG, "Exception raised during disconnection $e")
                }
                MbtEventBus.postEvent(StartOrContinueConnectionRequestEvent(false, bluetoothContext)) //current state should be IDLE
            }
        }
    }


    private fun setRequestAsProcessing() {
        Log.d(TAG, "Processing request")
        requestBeingProcessed = true
    }

    private fun setRequestAsProcessed() {
        setRequestAsProcessed(true)
    }

    private fun setRequestAsProcessed(displayLog: Boolean) {
        if (displayLog) LogUtils.d(TAG, "Request processed")
        requestBeingProcessed = false
    }

    //CALLBACKS

    /**
     * Notify the Connection process handler if the MTU has been well changed
     * @param command is the corresponding type of bluetooth command
     */
    private fun onBluetoothResponseReceived(response: Any?, command: BluetoothCommand<*, *>) {
        if (command is Mtu) onMtuChanged()
    }

    private fun onMtuChanged() {
        updateConnectionState(true) //current state is set to BT_PARAMETERS_CHANGED
        switchToNextConnectionStep()
    }

    /**
     * Add the new [BluetoothRequests] to the handler thread that will execute tasks one after another
     * This method must return quickly in order not to block the thread.
     * @param request the new [BluetoothRequests] to execute
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onNewBluetoothRequest(request: BluetoothRequests) {//Specific case: disconnection has main priority so we don't add it to queue
        if (request is DisconnectRequestEvent && request.isInterrupted) cancelPendingConnection(request.isInterrupted)
        else requestHandler.post(Runnable { requestThread.parseRequest(request) }) // enqueue a Runnable object to be called by the Handler message queue when they are received. When posting or sending to a Handler, the item is processed as soon as the message queue is ready to do so
    }
}