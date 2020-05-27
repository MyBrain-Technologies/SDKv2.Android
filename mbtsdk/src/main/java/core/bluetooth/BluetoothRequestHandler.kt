package core.bluetooth

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import core.bluetooth.lowenergy.MbtBluetoothLE
import core.bluetooth.requests.*
import core.bluetooth.spp.MbtBluetoothSPP
import core.device.model.DeviceInfo
import features.MbtDeviceType
import utils.LogUtils

class BluetoothRequestHandler {

    private val bluetoothForDataStreaming: MbtDataBluetooth? = null
    private val bluetoothForAudioStreaming: MbtAudioBluetooth? = null
    private val bluetoothInitializer: BluetoothInitializer? = null

    private val requestBeingProcessed = false
    private val isRequestCompleted = false

    private var isConnectionInterrupted = false

    private var requestThread: RequestThread? = null

    /**
     * Handler object enqueues an action to be performed on a different thread than the main thread
     */
    private var requestHandler: Handler? = null

    init {

        //Init thread that will handle messages synchronously. Using HandlerThread looks like it is the best way for CPU consumption as infinite loop in async thread was too heavy for cpu
        requestThread = RequestThread("requestThread", Thread.MAX_PRIORITY)
        requestThread?.start()
        requestHandler = Handler(requestThread?.looper)
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
        if (state != null && !state.isAudioState && (!isConnectionInterrupted || state == BluetoothState.CONNECTION_INTERRUPTED)) {
            bluetoothForDataStreaming?.notifyConnectionStateChanged(state)
        }
    }

    /**
     * This class is a specific thread that will handle all bluetooth operations. Bluetooth operations
     * are synchronous, meaning two or more operations can't be run simultaneously. This [HandlerThread]
     * extended class is able to hold pending operations.
     */
    internal class RequestThread(name: String?, priority: Int) : HandlerThread(name, priority) {
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
            Log.d(MbtBluetoothManager.TAG, "Parse request $request")
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
            bluetoothContext = BluetoothContext(
                    mContext,
                    event.typeOfDeviceRequested,
                    event.connectAudioIfDeviceCompatible(),
                    event.nameOfDeviceRequested,
                    event.qrCodeOfDeviceRequested,
                    event.mtu)
            if (event.isClientUserRequest && bluetoothForDataStreaming == null) {
                if (bluetoothContext.deviceTypeRequested == MbtDeviceType.MELOMIND) {
                    bluetoothForDataStreaming = MbtBluetoothLE(mContext, this@MbtBluetoothManager)
                    if (bluetoothContext.connectAudioIfDeviceCompatible) bluetoothForAudioStreaming = MbtBluetoothA2DP(mContext, this@MbtBluetoothManager)
                } else if (bluetoothContext.deviceTypeRequested == MbtDeviceType.VPRO) bluetoothForDataStreaming = MbtBluetoothSPP(mContext, this@MbtBluetoothManager)
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
            if (isClientUserRequest)
                isConnectionInterrupted = false
            if (!isConnectionInterrupted) {
                LogUtils.d(MbtBluetoothManager.TAG, "State is " + getCurrentState())
                when (getCurrentState()) {
                    BluetoothState.IDLE, BluetoothState.DATA_BT_DISCONNECTED -> getReadyForBluetoothOperation()
                    BluetoothState.READY_FOR_BLUETOOTH_OPERATION -> startScan()
                    BluetoothState.DEVICE_FOUND, BluetoothState.DATA_BT_CONNECTING -> startConnectionForDataStreaming()
                    BluetoothState.DATA_BT_CONNECTION_SUCCESS -> startDiscoveringServices()
                    BluetoothState.DISCOVERING_SUCCESS -> startReadingDeviceInfo(DeviceInfo.FW_VERSION) // read all device info (except battery) : first device info to read is firmware version
                    BluetoothState.READING_FIRMWARE_VERSION_SUCCESS -> startReadingDeviceInfo(DeviceInfo.HW_VERSION) // read next device info : second device info to read is hardware version
                    BluetoothState.READING_HARDWARE_VERSION_SUCCESS -> startReadingDeviceInfo(DeviceInfo.SERIAL_NUMBER) // read next device info : third device info to read is serial number (device ID)
                    BluetoothState.READING_SERIAL_NUMBER_SUCCESS -> startReadingDeviceInfo(DeviceInfo.MODEL_NUMBER) // read next device info : fourth device info to read is model number
                    BluetoothState.READING_SUCCESS -> startBonding()
                    BluetoothState.BONDED -> changeMTU()
                    BluetoothState.BT_PARAMETERS_CHANGED -> startSendingExternalName()
                    BluetoothState.CONNECTED -> startConnectionForAudioStreaming()
                    else -> setRequestAsProcessed()
                }
            } else setRequestAsProcessed()
        }
    }

}