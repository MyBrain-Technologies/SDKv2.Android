package core.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.HandlerThread
import android.util.Log
import command.BluetoothCommand
import command.BluetoothCommands.Mtu
import command.DeviceCommands.ConnectAudio
import command.DeviceCommands.DisconnectAudio
import config.MbtConfig
import core.bluetooth.BluetoothProtocol.*
import core.bluetooth.BluetoothState.*
import core.bluetooth.lowenergy.MbtBluetoothLE
import core.bluetooth.requests.BluetoothRequests
import core.bluetooth.requests.DisconnectRequestEvent
import core.bluetooth.requests.StartOrContinueConnectionRequestEvent
import core.device.DeviceEvents.*
import core.device.model.DeviceInfo.*
import core.device.model.MbtDevice
import engine.SimpleRequestCallback
import engine.clientevents.BaseError
import engine.clientevents.BaseErrorEvent
import engine.clientevents.ConnectionStateReceiver
import eventbus.MbtEventBus
import eventbus.events.ConnectionStateEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import utils.LogUtils
import utils.MbtAsyncWaitOperation
import utils.VersionHelper
import java.util.concurrent.TimeoutException

/**
 * Created by Etienne on 08/02/2018.
 * This class contains all necessary methods to manage the Bluetooth communication with the myBrain peripheral devices.
 * - 3 Bluetooth layers are used :
 * - Bluetooth Low Energy protocol is used with Melomind Headset for communication.
 * - Bluetooth SPP protocol which is used for the VPro headset communication.
 * - Bluetooth A2DP is used for Audio stream.
 * We scan first with the Low Energy Scanner as it is more efficient than the classical Bluetooth discovery scanner.
 */
class MbtBluetoothConnecter(context: Context) : MbtBluetoothManager(context) {
  private lateinit var bluetoothContext: BluetoothContext
  private lateinit var bluetoothInitializer: BluetoothInitializer
  private lateinit var bluetoothForDataStreaming: MbtDataBluetooth
  private var bluetoothForAudioStreaming: MbtAudioBluetooth? = null
  private var requestBeingProcessed = false


  /**
   * Handler object enqueues an action to be performed on a different thread than the main thread
   */
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
    private val TAG = MbtBluetoothConnecter::class.java.simpleName
  }


  /**
   * This class is a specific thread that will handle all bluetooth operations. Bluetooth operations
   * are synchronous, meaning two or more operations can't be run simultaneously. This [HandlerThread]
   * extended class is able to hold pending operations.
   */
  inner class RequestThread(name: String?, priority: Int) : HandlerThread(name, priority) {


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
          DISCOVERING_SUCCESS -> startReadingDeviceInfo(FW_VERSION) // read all device info (except battery) : first device info to read is firmware version
          READING_FIRMWARE_VERSION_SUCCESS -> startReadingDeviceInfo(HW_VERSION) // read next device info : second device info to read is hardware version
          READING_HARDWARE_VERSION_SUCCESS -> startReadingDeviceInfo(SERIAL_NUMBER) // read next device info : third device info to read is serial number (device ID)
          READING_SERIAL_NUMBER_SUCCESS -> startReadingDeviceInfo(MODEL_NUMBER) // read next device info : fourth device info to read is model number
          READING_SUCCESS -> startBonding()
          BONDED -> changeMTU()
          BT_PARAMETERS_CHANGED -> startSendingExternalName()
          CONNECTED -> startConnectionForAudioStreaming()
          else -> isRequestProcessing(false)
        }
      } else isRequestProcessing(false)
    }
  }

  private fun switchToNextConnectionStep() {
    isRequestProcessing(false)
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
   * Requires [Manifest.permission.ACCESS_FINE_LOCATION] or [Manifest.permission.ACCESS_COARSE_LOCATION] permission if a GPS sensor is available.
   * If permissions are not given and/or bluetooth device is not Le compatible, discovery scan is started.
   * The started Bluetooth connection process is stopped if the prerequisites are not valid.
   */
  private fun startScan() {
    var newState: BluetoothState = SCAN_FAILURE
    asyncOperation.tryOperation({ bluetoothForDataStreaming.startScan() },
        BaseErrorEvent { exception, _ ->
          if (exception is TimeoutException) newState = SCAN_TIMEOUT
        },//stop the current Bluetooth connection process
        { bluetoothForDataStreaming.stopScan() },
        MbtConfig.getBluetoothScanTimeout())

    if (currentState == SCAN_STARTED) ////at this point : current state should be DEVICE_FOUND if scan succeeded
      updateConnectionState(newState) //scan failure or timeout
    switchToNextConnectionStep()
  }

  /**
   * This connection step is the BLE (Melomind) /SPP (Vpro) connection to the found device
   * It allows communication between the headset device and the SDK for data streaming (EEG, battery level, etc.)
   */
  private fun startConnectionForDataStreaming() {
    LogUtils.i(TAG, "start connection data streaming")
    asyncOperation.tryOperation({ connect(bluetoothContext.deviceTypeRequested.protocol) },
        MbtConfig.getBluetoothConnectionTimeout())

    if (currentState.notEquals(CONNECTED_AND_READY, DATA_BT_CONNECTION_SUCCESS, IDLE)) updateConnectionState(CONNECTION_FAILURE)
    switchToNextConnectionStep()
  }

  private fun connect(protocol: BluetoothProtocol) {
    isConnectionInterrupted = false // resetting the flag when starting a new connection
    val isConnectionSuccessful = when (protocol) {
      LOW_ENERGY, SPP -> bluetoothForDataStreaming.connect(mContext, currentDevice)
      A2DP -> bluetoothForAudioStreaming?.connect(mContext, currentDevice) ?: false
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
   * This is an asynchronous operation.
   * Once service discovery is completed, the BluetoothGattCallback.onServicesDiscovered callback is triggered.
   * If the discovery was successful, the remote services can be retrieved using the getServices function
   */
  private fun startDiscoveringServices() {
    if (bluetoothForDataStreaming is MbtBluetoothLE) {
      LogUtils.i(TAG, "start discovering services ")
      if (currentState.ordinal >= DATA_BT_CONNECTION_SUCCESS.ordinal) { //if connection is in progress and BLE is at least connected, we can discover services
        asyncOperation.tryOperation({ (bluetoothForDataStreaming as MbtBluetoothLE).discoverServices() },
            MbtConfig.getBluetoothDiscoverTimeout()
        )

        if (currentState != DISCOVERING_SUCCESS) { ////at this point : current state should be DISCOVERING_SUCCESS if discovery succeeded
          updateConnectionState(DISCOVERING_FAILURE)
        }
        switchToNextConnectionStep()
      }
    }
  }

  private fun startBonding() {
    if (bluetoothForDataStreaming is MbtBluetoothLE) {
      LogUtils.i(TAG, "start bonding if supported")
      requestCurrentConnectedDevice(SimpleRequestCallback<MbtDevice> { device ->
        val isBondingSupported = VersionHelper(device.firmwareVersion.toString()).isValidForFeature(VersionHelper.Feature.BLE_BONDING)

        if (isBondingSupported) { //if firmware version bonding is higher than 1.6.7, the bonding is launched
          asyncOperation.tryOperation({
            if (currentState == BONDING) {
              return@tryOperation
            }//avoid double bond if several requestCurrentConnectedDevice are called at the same moment
            (bluetoothForDataStreaming as MbtBluetoothLE).bond()
          },
              MbtConfig.getBluetoothBondingTimeout()
          )

        } else  //if firmware version bonding is older than 1.6.7, the connection process is considered completed
          updateConnectionState(CONNECTED)
      })
        sleep(500)

      if ((currentState == BONDING)) { //at this point : current state should be BONDED if bonding succeeded
        updateConnectionState(BONDING_FAILURE)
      }
      sleep(1000)
      switchToNextConnectionStep()
    }
  }


  private fun startConnectionForAudioStreaming() {
    if (bluetoothContext.connectAudioIfDeviceCompatible && !isAudioBluetoothConnected) {
      LogUtils.i(TAG, "start connection audio streaming")
      if (getConnectedDevice() == null) return
      val connectionFromBleAvailable = VersionHelper(getConnectedDevice()?.firmwareVersion.toString()).isValidForFeature(VersionHelper.Feature.A2DP_FROM_HEADSET)
      asyncOperation.tryOperation({
        if (connectionFromBleAvailable) { //A2DP cannot be connected from BLE if BLE connection state is not CONNECTED_AND_READY or CONNECTED
          bluetoothForDataStreaming.sendCommand(ConnectAudio())  // if connectA2DPFromBLE failed or is not supported by the headset firmware version
        } else {
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || (bluetoothForAudioStreaming as MbtBluetoothA2DP?)?.isPairedDevice(currentDevice) == true) {
            connect(A2DP)
          } else {
            notifyConnectionStateChanged(AUDIO_CONNECTION_UNSUPPORTED)
          }
        }
      },
          MbtConfig.getBluetoothA2DpConnectionTimeout())

      if (bluetoothForAudioStreaming?.isConnected == false) {
        if (connectionRetryCounter < MAX_CONNECTION_RETRY) {
          connectionRetryCounter++
          sleep(200)
          startConnectionForAudioStreaming()
        } else {
          connectionRetryCounter = 0
          bluetoothForAudioStreaming?.notifyConnectionStateChanged(CONNECTION_FAILURE) //at this point : current state should be AUDIO_CONNECTED if audio connection succeeded
          bluetoothForDataStreaming.notifyConnectionStateChanged(CONNECTION_FAILURE)
        }
      }
    }
    if (isConnected) updateConnectionState(true) //BLE and audio (if SDK user requested it) are connected so the client is notified that the device is fully connected
    isRequestProcessing(false)
    LogUtils.i(TAG, "connection completed")
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
    isRequestProcessing(false)
    disconnectAllBluetooth(!asyncSwitchOperation.isWaiting)
    if (isClientUserAbortion) {
      isConnectionInterrupted = true
      updateConnectionState(CONNECTION_INTERRUPTED)
    }
    asyncOperation.stopWaitingOperation(null)
  }


    fun sleep(duration : Long){
        try { Thread.sleep(duration) } catch (e: InterruptedException) { }
    }

  /**
   * This method is called from Bluetooth classes and is meant to post an event to the main manager
   * that contains the new [BluetoothState]
   * @param newState the new [BluetoothState]
   */
  fun notifyConnectionStateChanged(newState: BluetoothState) {
    isRequestProcessing(false)
    when (newState) {
      DATA_BT_DISCONNECTED -> notifyDataBluetoothDisconnected() //a disconnection occurred
      AUDIO_BT_DISCONNECTED -> notifyAudioBluetoothDisconnected(newState)
      AUDIO_BT_CONNECTION_SUCCESS -> notifyAudioBluetoothConnected(newState)
      JACK_CABLE_CONNECTED -> notifyJackCableConnected()
      DEVICE_FOUND -> notifyDeviceFound(newState)
    }
    MbtEventBus.postEvent(ConnectionStateEvent(newState, getConnectedDevice())) //This event is sent to MbtManager for user notifications and to MbtDeviceManager
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
    if (bluetoothForAudioStreaming == null) {
      return
    }

    bluetoothForAudioStreaming?.notifyConnectionStateChanged(newState, false)
    asyncOperation.stopWaitingOperation(false)
    val audioDevice = bluetoothForAudioStreaming?.getCurrentDevice()
    if (audioDevice == null || bluetoothForDataStreaming !is MbtBluetoothLE) {
      return
    }

    val bleDeviceName = (bluetoothForDataStreaming as MbtBluetoothLE).getBleDeviceNameFromA2dp(audioDevice?.name, mContext)
    if (!isDataBluetoothConnected || !bluetoothForDataStreaming.isCurrentDeviceNameEqual(bleDeviceName))
      connectBLEFromA2DP(bleDeviceName)
    MbtEventBus.postEvent(AudioConnectedDeviceEvent(audioDevice))
  }

  private fun notifyJackCableConnected() {
    if (asyncOperation.isWaiting) asyncOperation.stopWaitingOperation(false)
  }

  private fun notifyDeviceFound(newState: BluetoothState) {
    MbtEventBus.postEvent(ConnectionStateEvent(newState, currentDevice, bluetoothContext.deviceTypeRequested))
    if (bluetoothContext.connectAudioIfDeviceCompatible) {
      if (bluetoothForAudioStreaming == null) {
        bluetoothForAudioStreaming = MbtBluetoothA2DP(mContext, this@MbtBluetoothConnecter)
      } else if (bluetoothForAudioStreaming?.currentDevice != null) {
        MbtEventBus.postEvent(AudioConnectedDeviceEvent(bluetoothForAudioStreaming?.currentDevice))
      }
    }
  }


  fun getConnectedDevice(): MbtDevice? {
    val asyncWaitOperation = MbtAsyncWaitOperation<MbtDevice>()
    return asyncWaitOperation.tryOperationForResult({
      requestCurrentConnectedDevice(SimpleRequestCallback { device ->
        asyncWaitOperation.stopWaitingOperation(device)
      })
    }, null, null, 1500)
  }

  fun requestCurrentConnectedDevice(callback: SimpleRequestCallback<MbtDevice>) {
    MbtEventBus.postEvent(GetDeviceEvent(), object : MbtEventBus.Callback<PostDeviceEvent> {
      @Subscribe // DO NOT DELETE THE SUBSCRIBE ANNOTATION
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
    if (state?.isAudioState == false && (!isConnectionInterrupted || (state == CONNECTION_INTERRUPTED))) {
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

  fun disconnectA2DPFromBLE() {
    LogUtils.i(TAG, " disconnect A2dp from ble")
    if (isDataBluetoothConnected && isAudioBluetoothConnected) bluetoothForDataStreaming.sendCommand(DisconnectAudio())
  }

  /**
   * Starts a Low Energy connection process if a Melomind is connected for Audio Streaming in A2DP.
   */
  private fun connectBLEFromA2DP(newDeviceBleName: String) {
    LogUtils.i(TAG, "connect BLE from a2dp ")
    if (isAudioBluetoothConnected) {
      val currentStateBeforeDisconnection = currentState
      if (bluetoothForDataStreaming.isConnected && !bluetoothForDataStreaming.isCurrentDeviceNameEqual(newDeviceBleName)) //Disconnecting another melomind if already one connected in BLE
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

  //CALLBACKS

  /**
   * Notify the Connection process handler if the MTU has been well changed
   * @param command is the corresponding type of bluetooth command
   */
  private fun onBluetoothResponseReceived(response: Any?, command: BluetoothCommand<*, *>) {
    if (command is Mtu) {
      updateConnectionState(true) //current state is set to BT_PARAMETERS_CHANGED
      switchToNextConnectionStep()
    }
  }

  /**
   * Add the new [BluetoothRequests] to the handler thread that will execute tasks one after another
   * This method must return quickly in order not to block the thread.
   * @param request the new [BluetoothRequests] to execute
   */
  @Subscribe(threadMode = ThreadMode.ASYNC)
  fun onNewBluetoothRequest(request: BluetoothRequests) {
    if (request is DisconnectRequestEvent && request.isInterrupted) cancelPendingConnection(request.isInterrupted)//Specific case: disconnection has main priority so we don't add it to queue
    else requestHandler.post(Runnable { requestThread.parseRequest(request) }) // enqueue a Runnable object to be called by the Handler message queue when they are received. When posting or sending to a Handler, the item is processed as soon as the message queue is ready to do so
  }

  //SETTERS & GETTERS
  private fun isRequestProcessing(isRequestProcessing: Boolean) {
    requestBeingProcessed = isRequestProcessing
  }

  /**
   * Tells whether or not the end-user device is currently connected to the headset in Low Energy.
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

  private val currentDevice: BluetoothDevice
    get() = bluetoothForDataStreaming.currentDevice

  /**
   * Return true if the user has requested connection with an already connected device, false otherwise
   */
  private fun isAlreadyConnectedToRequestedDevice(nameDeviceToConnect: String?, deviceConnected: MbtDevice?): Boolean {
    return ((deviceConnected?.externalName != null) && (deviceConnected.externalName == nameDeviceToConnect))
  }

}