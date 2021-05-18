package core.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import command.BluetoothCommands
import command.DeviceCommands.ConnectAudio
import command.DeviceCommands.DisconnectAudio
import config.MbtConfig
import core.Indus5FastMode
import core.bluetooth.BluetoothProtocol.*
import core.bluetooth.BluetoothState.*
import core.bluetooth.lowenergy.EnumIndus5Command
import core.bluetooth.lowenergy.MbtBluetoothLE
import core.bluetooth.requests.CommandRequestEvent
import core.bluetooth.requests.Indus5CommandRequest
import core.bluetooth.requests.StartOrContinueConnectionRequestEvent
import core.device.DeviceEvents.GetDeviceEvent
import core.device.DeviceEvents.PostDeviceEvent
import core.device.model.DeviceInfo
import core.device.model.DeviceInfo.*
import core.device.model.MbtDevice
import engine.SimpleRequestCallback
import engine.clientevents.BaseError
import engine.clientevents.BaseErrorEvent
import engine.clientevents.ConnectionStateReceiver
import eventbus.MbtEventBus
import eventbus.events.ConnectionStateEvent
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import utils.BroadcastUtils
import utils.LogUtils
import utils.MbtAsyncWaitOperation
import utils.VersionHelper
import java.util.concurrent.TimeoutException

/**Created by Sophie on 02/06/2020.
 * This class contains all necessary methods to manage the Bluetooth connection & disconnection with the myBrain peripheral devices.*/
class MbtBluetoothConnecter(private val manager: MbtBluetoothManager) : ConnectionStateReceiver() {
  //----------------------------------------------------------------------------
  // Properties
  //----------------------------------------------------------------------------
  val TAG = this::class.java.simpleName
  companion object {
    private const val MAX_CONNECTION_RETRY = 2
  }
  
  var connectionRetryCounter = 0
  var isConnectionInterrupted = false
  var connectedDevice : MbtDevice? = null
    get() {
      if(field == null){
        val asyncWaitOperation = MbtAsyncWaitOperation<MbtDevice>()
        field = asyncWaitOperation.tryOperationForResult({
          requestCurrentConnectedDevice(SimpleRequestCallback { device ->
            asyncWaitOperation.stopWaitingOperation(device)
          })
        }, null, null, 500)
      }
      return field
    }
  
  fun getBluetoothContext() : BluetoothContext {return manager.context}

  val isDataBluetoothConnected: Boolean
    get() = MbtDataBluetooth.instance.isConnected

  /** Tells whether or not the end-user device is currently connected to the headset in A2DP.
   * @return `true` if connected, `false` otherwise */
  val isAudioBluetoothConnected: Boolean
    get() = MbtAudioBluetooth.instance?.isConnected ?: false

  /** Tells whether or not the end-user device is currently connected to the headset both in Low Energy and A2DP.
   * @return `true` if connected, `false` otherwise*/
  val isConnected: Boolean
    get() = (if (getBluetoothContext().connectAudio)
      (isDataBluetoothConnected && isAudioBluetoothConnected)
    else
      isDataBluetoothConnected)

  /**  As the Bluetooth scan requires access to the mobile device Location,
   * the Bluetooth Manager must always check this prerequisite before starting any connection operation by calling the isLocationDisabledOrNotGranted() method.
   * @return true if a headset is already connected in BLE and Audio (if Audio has been enabled)
   * or if no headset is connected. */
  fun MbtDevice.isAlreadyConnectedToAnyDevice(): Boolean {
    if (isConnected) {
      if (!isAlreadyConnectedToRequestedDevice(getBluetoothContext().deviceNameRequested)) {
        updateConnectionState(ANOTHER_DEVICE_CONNECTED)
      } else updateConnectionState(CONNECTED_AND_READY)
      return true
    }
    return false
  }
  
  /** Return true if the user has requested connection with an already connected device, false otherwise */
  fun MbtDevice.isAlreadyConnectedToRequestedDevice(nameDeviceToConnect: String?): Boolean {
    return (externalName != null) && (externalName == nameDeviceToConnect)
  }
  //----------------------------------------------------------------------------
  // SET UP
  //----------------------------------------------------------------------------
  init {
    val context = getBluetoothContext().context
    BroadcastUtils.registerReceiverIntents(context, this, BluetoothAdapter.ACTION_STATE_CHANGED)
  }
  //----------------------------------------------------------------------------
  // CONNECTION PROCESS METHODS
  //----------------------------------------------------------------------------
  /** This method do the following operations:
   * - 1) Check the prerequisites to ensure that the connection can be performed
   * - 2) Scan for [BluetoothDevice] filtering on deviceName. The scan is performed by LE scanner if the device is LE compatible. Otherwise, the discovery scan is performed instead.
   * - 3) Perform the BLE/SPP connection operation if scan resulted in a found device
   * - 4) Discovering services once the headset is connected.
   * - 5) Reading the device info (firmware version, hardware version, serial number, model number) once the services has been discovered
   * - 6) Bond the headset if the firmware version supports it (version > 1.6.7)
   * - 7) Send the QR code number to the headset if it doesn't know its own value and if the firmware version supports it (version > 1.7.1)
   * - 8) Connect audio in A2dp is the user requested it */
  fun startOrContinueConnectionOperation(isClientUserRequest: Boolean) {
    if (isClientUserRequest) isConnectionInterrupted = false
    if (!isConnectionInterrupted) {
      val currentState = MbtDataBluetooth.instance.currentState


      LogUtils.d(TAG, "State is $currentState")
//      LogUtils.e("ConnSteps", "State is $currentState")
      when (currentState) {
        IDLE, DATA_BT_DISCONNECTED -> {
//          LogUtils.e("ConnSteps", "4a : getReadyForBluetoothOperation");
          getReadyForBluetoothOperation()
        }
        READY_FOR_BLUETOOTH_OPERATION -> {
//          LogUtils.e("ConnSteps", "5a : start ble scan")
          startScan()
        }
        DEVICE_FOUND, DATA_BT_CONNECTING -> {
//          LogUtils.e("ConnSteps", "6a : startConnectionForDataStreaming")
          startConnectionForDataStreaming()
        }
        DATA_BT_CONNECTION_SUCCESS -> {
//          LogUtils.e("ConnSteps", "7a : startDiscoveringServices")
          startDiscoveringServices()
        }
        DISCOVERING_SUCCESS -> startReadingDeviceInfo(FW_VERSION) // read all device info (except battery) : first device info to read is firmware version
        READING_FIRMWARE_VERSION_SUCCESS -> startReadingDeviceInfo(HW_VERSION) // read next device info : second device info to read is hardware version
        READING_HARDWARE_VERSION_SUCCESS -> startReadingDeviceInfo(SERIAL_NUMBER) // read next device info : third device info to read is serial number (device ID)
        READING_SERIAL_NUMBER_SUCCESS -> startReadingDeviceInfo(MODEL_NUMBER) // read next device info : fourth device info to read is model number
        READING_SUCCESS -> startBonding()
        BONDED -> changeMTU()
        BT_PARAMETERS_CHANGED -> startSendingExternalName()
        INDUS5_DISCOVERING_SUCCESS -> {
//          LogUtils.e("ConnSteps", "subsribe")
          subscribeIndus5Tx()
        }
        INDUS5_MTU_CHANGING_1 -> {
          //no operation, mark immediately this step as done
          manager.setRequestProcessing(false)
        }
        INDUS5_MTU_CHANGED_1 -> {
          changeMtuInMailboxIndus5()
        }
        INDUS5_MTU_CHANGING_2 -> {
          //no operation, mark immediately this step as done
          manager.setRequestProcessing(false)
        }
        INDUS5_MTU_CHANGED_2 -> {
//          LogUtils.e("ConnSteps", "10a : mtu indus5 changed ok, finish connection steps")
          //for indus5 : ends connection process here
          LogUtils.i(TAG, "INDUS5_MTU_ON_TX_CHANGED")
          manager.setRequestProcessing(false)
          updateConnectionState(CONNECTED_AND_READY)
          notifyConnectionStateChanged(CONNECTED_AND_READY)
        }
        CONNECTED -> {
//          LogUtils.e("ConnSteps", "11a")
          startConnectionForAudioStreaming()
        }
        else -> {
//          LogUtils.e("ConnSteps", "stop 179")
          manager.setRequestProcessing(false)
        }
      }
    } else {
//      LogUtils.e("ConnSteps", "stop 184")
      manager.setRequestProcessing(false)
    }
  }

  fun switchToNextConnectionStep() {
    manager.setRequestProcessing(false)
    val currentState = MbtDataBluetooth.instance.currentState
    if (!currentState.isAFailureState() && !isConnectionInterrupted && currentState != IDLE) {  //if nothing went wrong during the current step of the connection process, we continue the process
//      Timber.e("Post new StartOrContinueConnectionRequestEvent")
      manager.onNewBluetoothRequest(StartOrContinueConnectionRequestEvent(false, getBluetoothContext()))
    }
  }
  
  /** Check the bluetooth prerequisites before starting any bluetooth operation.
   * The started Bluetooth connection process is stopped if the prerequisites are not valid. */
  fun getReadyForBluetoothOperation() {
    connectionRetryCounter = 0
    //Request sent to the BUS in order to get device from the device manager : the BUS should return a null object if it's the first connection, or return a non null object if the user requests connection whereas a headset is already connected
    LogUtils.i(TAG, "Checking Bluetooth Prerequisites and initialize")
    if (connectedDevice?.isAlreadyConnectedToAnyDevice() == true) // assert that headset is not already connected
      return
    val state = BluetoothInitializer().getBluetoothPrerequisitesState((getBluetoothContext())) //check that Bluetooth is on, Location is on, Location permission is granted

    if (state != READY_FOR_BLUETOOTH_OPERATION) { //assert that Bluetooth is on, Location is on, Location permission is granted
      updateConnectionState(state)
      return
    }

    if (MbtAudioBluetooth.instance is MbtBluetoothA2DP) (MbtAudioBluetooth.instance as MbtBluetoothA2DP).initA2dpProxy() //initialization to check if a Melomind is already connected in A2DP : as the next step is the scanning, the SDK is able to filter on the name of this device
    if (MbtDataBluetooth.instance.currentState == IDLE) updateConnectionState(false) //current state is set to READY_FOR_BLUETOOTH_OPERATION
//    LogUtils.e("ConnSteps", "4b : bluetooth is ready")
    switchToNextConnectionStep()
  }

  /** This method starts a bluetooth scan operation, loooking for a single device by filtering on its name.
   * This method is asynchronous.
   * If the device to scan is a Melomind, the bluetooth LE scanner is invoked.
   * Otherwise, the classic discovery scan is invoked.
   * Requires [Manifest.permission.ACCESS_FINE_LOCATION] or [Manifest.permission.ACCESS_COARSE_LOCATION] permission if a GPS sensor is available.
   * If permissions are not given and/or bluetooth device is not Le compatible, discovery scan is started.
   * The started Bluetooth connection process is stopped if the prerequisites are not valid. */
  fun startScan() {
    var newState: BluetoothState = SCAN_FAILURE
    manager.tryOperation({
      MbtDataBluetooth.instance.startScan()
    },
            object : BaseErrorEvent<BaseError> {
              override fun onError(error: BaseError, additionalInfo: String?) {
                if (error is TimeoutException) newState = SCAN_TIMEOUT
              }
            },//stop the current Bluetooth connection process
            { MbtDataBluetooth.instance.stopScan() },
            MbtConfig.getBluetoothScanTimeout())

    if (MbtDataBluetooth.instance.currentState == SCAN_STARTED) ////at this point : current state should be DEVICE_FOUND if scan succeeded
      updateConnectionState(newState) //scan failure or timeout
    switchToNextConnectionStep()
  }

  /** This connection step is the BLE (Melomind) /SPP (Vpro) connection to the found device
   * It allows communication between the headset device and the SDK for data streaming (EEG, battery level, etc.)  */
  fun startConnectionForDataStreaming() {
    LogUtils.i(TAG, "start connection data streaming")
    manager.tryOperation(
            {
//              LogUtils.e("ConnSteps", "6b : request connect bluetooth")
              connect(getBluetoothContext().deviceTypeRequested.protocol)
            },
            MbtConfig.getBluetoothConnectionTimeout())

    if (MbtDataBluetooth.instance.currentState.notEquals(CONNECTED_AND_READY, DATA_BT_CONNECTION_SUCCESS, IDLE)) updateConnectionState(CONNECTION_FAILURE)
    switchToNextConnectionStep()
  }

  fun connect(protocol: BluetoothProtocol) {
    isConnectionInterrupted = false // resetting the flag when starting a new connection
    val context = getBluetoothContext().context
    val currentDevice = MbtDataBluetooth.instance.currentDevice
    val isConnectionSuccessful = currentDevice != null && when (protocol) {
      LOW_ENERGY, SPP -> {
//        LogUtils.e("ConnSteps", "6b1 : connect bluetooth")
        MbtDataBluetooth.instance.connect(context, currentDevice)
      }
      A2DP -> {
//        LogUtils.e("ConnSteps", "6b2 : connect a2dp")
        MbtAudioBluetooth.instance?.connect(context, currentDevice) ?: false
      }
    }
    if (isConnectionSuccessful) {
      if (protocol == A2DP) {
        if (isAudioBluetoothConnected) manager.stopWaitingOperation(false)
      } else updateConnectionState(isDataBluetoothConnected)
    }
  }

  /** Starts a Low Energy connection process if a Melomind is connected for Audio Streaming in A2DP.*/
  fun connectBLEFromA2DP(newDeviceBleName: String) {
    LogUtils.i(TAG, "connect BLE from a2dp ")
    if (isAudioBluetoothConnected) {
      val currentStateBeforeDisconnection = MbtDataBluetooth.instance.currentState
      if (MbtDataBluetooth.instance.isConnected
          && !MbtDataBluetooth.instance.isCurrentDeviceNameEqual(newDeviceBleName)) { //Disconnecting another melomind if already one connected in BLE
        MbtDataBluetooth.instance.disconnect()
      }
      getBluetoothContext().deviceNameRequested = newDeviceBleName
      if (currentStateBeforeDisconnection != IDLE) {
        manager.startSwitchWaitingOperation(8000)
        manager.notifyEvent(StartOrContinueConnectionRequestEvent(false, getBluetoothContext())) //current state should be IDLE
      }
    }
  }

  /**  Once a device is connected in Bluetooth Low Energy / SPP for data streaming, we consider that the Bluetooth connection process is not fully completed.
   * The services offered by a remote device as well as their characteristics and descriptors are discovered to ensure that Data Streaming can be performed.
   * It means that the Bluetooth Manager retrieve all the services, which can be seen as categories of data that the headset is transmitting
   * This is an asynchronous operation.
   * Once service discovery is completed, the BluetoothGattCallback.onServicesDiscovered callback is triggered.
   * If the discovery was successful, the remote services can be retrieved using the getServices function */
  fun startDiscoveringServices() {
    if (MbtDataBluetooth.instance !is MbtBluetoothLE) {
      return
    }
    LogUtils.i(TAG, "start discovering services ")
    if (MbtDataBluetooth.instance.currentState.ordinal >= DATA_BT_CONNECTION_SUCCESS.ordinal) { //if connection is in progress and BLE is at least connected, we can discover services
      manager.tryOperation({ (MbtDataBluetooth.instance as MbtBluetoothLE).discoverServices() },
              MbtConfig.getBluetoothDiscoverTimeout()
      )

      if ((MbtDataBluetooth.instance.currentState != DISCOVERING_SUCCESS)
              && (MbtDataBluetooth.instance.currentState != INDUS5_DISCOVERING_SUCCESS)) { ////at this point : current state should be DISCOVERING_SUCCESS if discovery succeeded
        updateConnectionState(DISCOVERING_FAILURE)
      }

      switchToNextConnectionStep()
    }
  }

  fun startReadingDeviceInfo(deviceInfo: DeviceInfo) {
    updateConnectionState(false) //current state is set to READING_FIRMWARE_VERSION or READING_HARDWARE_VERSION or READING_SERIAL_NUMBER or READING_MODEL_NUMBER
    val state = manager.reader.startReadingDeviceInfo(deviceInfo)
    if (state != MbtDataBluetooth.instance.currentState) {
      updateConnectionState(READING_FAILURE)//at this point : current state should be READING...SUCCESS if a read result has been returned
    }
    switchToNextConnectionStep()
  }

    fun startBonding() {
    if (MbtDataBluetooth.instance !is MbtBluetoothLE) {
      return
    }
    if(connectedDevice == null){
      disconnect(LOW_ENERGY)
      return
    }
    val isBondingSupported = VersionHelper(connectedDevice?.firmwareVersion.toString()).isValidForFeature(VersionHelper.Feature.BLE_BONDING)
    LogUtils.i(TAG, "start bonding if supported $isBondingSupported")

    if (isBondingSupported) { //if firmware version bonding is higher than 1.6.7, the bonding is launched
      manager.tryOperation({
        if (MbtDataBluetooth.instance.currentState == BONDING) {
          return@tryOperation
        }//avoid double bond if several requestCurrentConnectedDevice are called at the same moment
        (MbtDataBluetooth.instance as MbtBluetoothLE).bond()
      },
              MbtConfig.getBluetoothBondingTimeout()
      )
      sleep(500)
      if ((MbtDataBluetooth.instance.currentState == BONDING)) { //at this point : current state should be BONDED if bonding succeeded
        updateConnectionState(BONDING_FAILURE)
      }
      sleep(1000)
    } else  //if firmware version bonding is older than 1.6.7, the connection process is considered completed
      updateConnectionState(CONNECTED)

    switchToNextConnectionStep()
  }

  fun changeMTU() {
//    Timber.e("changeMTU : parseRequest")
    updateConnectionState(false) //current state is set to CHANGING_BT_PARAMETERS (indus2) or INDUS5_CHANGING_MTU_1 (indus5)
    manager.parseRequest(CommandRequestEvent(BluetoothCommands.Mtu(manager.context.mtu)))
  }

  fun subscribeIndus5Tx() {
    Timber.i("subscribeIndus5Tx")
    updateConnectionState(false)
    manager.setRequestProcessing(false)
    manager.parseRequest(Indus5CommandRequest(EnumIndus5Command.MBX_RX_SUBSCRIPTION))
  }

  fun changeMtuInMailboxIndus5() {
    LogUtils.i(TAG, "changeMtuWithIndus5Tx")
    updateConnectionState(false)
    manager.parseRequest(Indus5CommandRequest(EnumIndus5Command.MBX_TRANSMIT_MTU_SIZE))
  }

  fun startSendingExternalName(){
    updateConnectionState(true) //current state is set to QR_CODE_SENDING
    connectedDevice?.let { manager.writer.startSendingExternalName(it) }
    updateConnectionState(true) //current state is set to CONNECTED in any case (success or failure) the connection process is completed and the SDK consider that everything is ready for any operation (for example ready to acquire EEG data)
    switchToNextConnectionStep()
  }

  fun startConnectionForAudioStreaming() {
    if (getBluetoothContext().connectAudio && !isAudioBluetoothConnected) {
      LogUtils.i(TAG, "start connection audio streaming with $connectedDevice")
      if (connectedDevice == null) return
      val connectionFromBleAvailable = VersionHelper(connectedDevice?.firmwareVersion.toString()).isValidForFeature(VersionHelper.Feature.A2DP_FROM_HEADSET)
      manager.tryOperation({
        if (connectionFromBleAvailable) { //A2DP cannot be connected from BLE if BLE connection state is not CONNECTED_AND_READY or CONNECTED
          MbtDataBluetooth.instance.sendCommand(ConnectAudio())  // if connectA2DPFromBLE failed or is not supported by the headset firmware version
        } else {
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || (MbtAudioBluetooth.instance as MbtBluetoothA2DP?)?.isPairedDevice(MbtDataBluetooth.instance.currentDevice) == true) {
            connect(A2DP)
          } else {
            notifyConnectionStateChanged(AUDIO_CONNECTION_UNSUPPORTED)
          }
        }
      }, MbtConfig.getBluetoothA2DpConnectionTimeout())

      if (MbtAudioBluetooth.instance?.isConnected == false) {
        retryConnectionForAudioStreaming()
      }
    }
    if (isConnected) updateConnectionState(true) //BLE and audio (if SDK user requested it) are connected so the client is notified that the device is fully connected
//    LogUtils.e("ConnSteps", "stop 404")
    manager.setRequestProcessing(false)
//    LogUtils.i(TAG, "connection completed")
  }

  fun retryConnectionForAudioStreaming() {
    if (connectionRetryCounter < MAX_CONNECTION_RETRY) {
      connectionRetryCounter++
      sleep(200)
      startConnectionForAudioStreaming()
    } else if(!isConnected){
      connectionRetryCounter = 0
      MbtAudioBluetooth.instance?.notifyConnectionStateChanged(CONNECTION_FAILURE) //at this point : current state should be AUDIO_CONNECTED if audio connection succeeded
      MbtDataBluetooth.instance.notifyConnectionStateChanged(CONNECTION_FAILURE)
    }
  }

    /** Start the disconnect operation on the currently connected bluetooth device according to the [BluetoothProtocol] currently used. */
  fun disconnect(protocol: BluetoothProtocol) {
      Log.d(TAG, "disconnect")
      if (isAudioBluetoothConnected || isDataBluetoothConnected || MbtDataBluetooth.instance.currentState.isConnectionInProgress()) {
      when (protocol) {
        LOW_ENERGY, SPP -> MbtDataBluetooth.instance.disconnect()
        A2DP -> MbtAudioBluetooth.instance?.disconnect()
      }
    }
    connectedDevice = null
  }

  fun disconnectAllBluetooth(disconnectAudioIfConnected: Boolean) {
    LogUtils.i(TAG, "Disconnect all bluetooth")
    if (isAudioBluetoothConnected && disconnectAudioIfConnected) disconnect(A2DP)
    getBluetoothContext().deviceTypeRequested.protocol?.let { disconnect(it) }
  }

  fun disconnectA2DPFromBLE() {
    LogUtils.i(TAG, " disconnect A2dp from ble");
    if(isDataBluetoothConnected && isAudioBluetoothConnected)
      MbtDataBluetooth.instance.sendCommand(DisconnectAudio())
  }

  /** Stops current pending connection according to its current [state][BluetoothState].
   * It can be either stop scan or connection process interruption  */
  fun cancelPendingConnection(isClientUserAbortion: Boolean) {
    LogUtils.i(TAG, "cancelling pending connection")
    manager.setRequestProcessing(false)
    disconnectAllBluetooth(!manager.isSwitchOperationWaiting())
    if (isClientUserAbortion) {
      isConnectionInterrupted = true
      updateConnectionState(CONNECTION_INTERRUPTED)
    }
    manager.stopWaitingOperation(null)
  }

  fun sleep(duration: Long) {
    try { Thread.sleep(duration) } catch (e: InterruptedException) { }
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

  /**  This method is called from Bluetooth classes and is meant to post an event to the main manager
   * that contains the new [BluetoothState]
   * @param newState the new [BluetoothState] */
  fun notifyConnectionStateChanged(newState: BluetoothState) {
    manager.setRequestProcessing(false)
    when (newState) {
      DATA_BT_DISCONNECTED -> manager.notifyDataBluetoothDisconnected() //a disconnection occurred
      AUDIO_BT_DISCONNECTED -> manager.notifyAudioBluetoothDisconnected()
      AUDIO_BT_CONNECTION_SUCCESS -> manager.notifyAudioBluetoothConnected()
      JACK_CABLE_CONNECTED -> manager.stopWaitingOperation(false)
      DEVICE_FOUND -> manager.notifyDeviceFound()
    }
    Log.d(TAG, "notify event with state $newState, device is $connectedDevice")
    manager.notifyEvent(ConnectionStateEvent(newState, connectedDevice))
  }

  /** Set the current bluetooth connection state to the value of the next step in chronological order (according to enum order)
   * and notify the bluetooth manager of this change.
   * This method should be called if no error occurred. */
  fun updateConnectionState(isCompleted: Boolean) {
    val nextStep = MbtDataBluetooth.instance.currentState.getNextConnectionStep()
    if (!isConnectionInterrupted) updateConnectionState(if (nextStep != IDLE) nextStep else null)
    if (isCompleted) manager.stopWaitingOperation(false)
  }

  /**  Set the current bluetooth connection state to the value given in parameter
   * and notify the bluetooth manager of this change.
   * This method should be called :
   * - if something went wrong during the connection process
   * - or if the new state does not correspond to the state that follow the current state in chronological order ([BluetoothState] enum order)
   * The updateConnectionState(boolean) method with no parameter should be call if nothing went wrong and user wants to continue the connection process */
  fun updateConnectionState(state: BluetoothState?) {
    if (state?.shouldBeNotified() == true) {
      manager.onConnectionStateChanged(state)
    }
  }

  fun BluetoothState.shouldBeNotified() : Boolean{
    return (!isAudioState()
        && getBluetoothContext().deviceTypeRequested != null
        && (!isConnectionInterrupted || this == CONNECTION_INTERRUPTED))
  }

  //----------------------------------------------------------------------------
  // CALLBACKS
  //----------------------------------------------------------------------------
  override fun onError(error: BaseError, additionalInfo: String?) {}
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action
    if (action != null) {
      val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
      Log.d(MbtBluetoothManager::class.simpleName, " received intent " + action + " for device " + (device?.name))
      if ((MbtAudioBluetooth.instance != null) && getBluetoothContext().connectAudio && (action == BluetoothAdapter.ACTION_STATE_CHANGED)) (MbtAudioBluetooth.instance as MbtBluetoothA2DP).resetA2dpProxy(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1))
    }
  }

  fun onDeviceDisconnected(){
    Log.d(TAG, "on device disconnected ")
    connectedDevice = null
  }
}