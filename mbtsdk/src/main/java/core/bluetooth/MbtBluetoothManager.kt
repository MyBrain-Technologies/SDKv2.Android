package core.bluetooth

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.VisibleForTesting
import command.BluetoothCommand
import command.BluetoothCommands.Mtu
import core.BaseModuleManager
import core.bluetooth.lowenergy.MbtBluetoothLE
import core.bluetooth.requests.*
import core.bluetooth.spp.MbtBluetoothSPP
import core.device.DeviceEvents
import engine.clientevents.BaseError
import engine.clientevents.BaseErrorEvent
import eventbus.MbtEventBus
import eventbus.events.BluetoothEEGEvent
import eventbus.events.ConnectionStateEvent
import eventbus.events.EEGConfigEvent
import features.MbtDeviceType.MELOMIND
import features.MbtDeviceType.VPRO
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import utils.MbtAsyncWaitOperation

/** Created by Sophie on 02/06/2020.
 * This class contains all necessary methods to manage the Bluetooth communication with the myBrain peripheral devices.
 * - 3 Bluetooth layers are used :
 * - Bluetooth Low Energy protocol is used with Melomind Headset for communication.
 * - Bluetooth SPP protocol which is used for the VPro headset communication.
 * - Bluetooth A2DP is used for Audio stream.
 * We scan first with the Low Energy Scanner as it is more efficient than the classical Bluetooth discovery scanner.
 */
class MbtBluetoothManager(context: Context) : BaseModuleManager(context) {

  //----------------------------------------------------------------------------
  // Properties
  //----------------------------------------------------------------------------
  lateinit var context: BluetoothContext //contains context & some bluetooth headset information

  lateinit var connecter: MbtBluetoothConnecter // is responsible for all the connection / disconnection operations
  lateinit var reader: MbtBluetoothReader // is responsible for all the reading / streaming operations
  lateinit var writer: MbtBluetoothWriter //is responsible for all the writing / firmware value changing operations

  var requestProcessor: RequestProcessor //processes all bluetooth requests in a thread that will handle messages synchronously, taking care of CPU consumption as infinite loop in async thread was too heavy for cpu

  companion object {
    private val TAG = MbtBluetoothManager::class.java.simpleName
  }

  //----------------------------------------------------------------------------
  // SET UP
  //----------------------------------------------------------------------------
  fun changeBluetoothParameters(event: StartOrContinueConnectionRequestEvent) {
    context = BluetoothContext(mContext,
        event.typeOfDeviceRequested,
        event.connectAudioIfDeviceCompatible(),
        event.nameOfDeviceRequested,
        event.qrCodeOfDeviceRequested,
        event.mtu)

    if (event.isClientUserRequest && !MbtDataBluetooth.isInitialized()) {
      initBluetoothOperators()
    }
  }

  fun initBluetoothOperators(){
    when (context.deviceTypeRequested) {
      MELOMIND -> {
        MbtDataBluetooth.instance = MbtBluetoothLE.initInstance(this)
        if (context.connectAudio) MbtAudioBluetooth.instance = MbtBluetoothA2DP.initInstance(this)
      }
      VPRO -> MbtDataBluetooth.instance = MbtBluetoothSPP.initInstance(this)
    }

    connecter = MbtBluetoothConnecter(this)
    reader = MbtBluetoothReader(this)
    writer = MbtBluetoothWriter(this)
  }

  init {
    requestProcessor = RequestProcessor() //Init thread that will handle messages synchronously. Using HandlerThread looks like it is the best way for CPU consumption as infinite loop in async thread was too heavy for cpu
  }

  inner class RequestProcessor{
    @get:VisibleForTesting
    @set:VisibleForTesting
    var requestThread: RequestThread

    @get:VisibleForTesting
    @set:VisibleForTesting
    var requestHandler: Handler

    var asyncOperation = MbtAsyncWaitOperation<Boolean>()
    var asyncSwitchOperation = MbtAsyncWaitOperation<Boolean>()

    var isRequestProcessing = false
    init {
      //Init thread that will handle messages synchronously. Using HandlerThread looks like it is the best way for CPU consumption as infinite loop in async thread was too heavy for cpu
      requestThread = RequestThread("requestThread", Thread.MAX_PRIORITY)
      requestThread.start()
      requestHandler = Handler(requestThread.looper)
    }

    /** This class is a specific thread that will handle all bluetooth operations. Bluetooth operations
     * are synchronous, meaning two or more operations can't be run simultaneously. This [HandlerThread]
     * extended class is able to hold pending operations.
     */
    inner class RequestThread(name: String?, priority: Int) : HandlerThread(name, priority) {

      /** Checks the subclass type of [BluetoothRequests] and handles the correct method/action to perform.
       * @param request the [BluetoothRequests] request to execute.
       */
      fun parseRequest(request: BluetoothRequests) {
        Log.d(TAG, "Parse request $request")
        while (isRequestProcessing);
        isRequestProcessing = true
        when (request) {

          is StartOrContinueConnectionRequestEvent -> {
            changeBluetoothParameters(request)
            connecter.startOrContinueConnectionOperation(request.isClientUserRequest)
          }

          is ReadRequestEvent -> reader.startReadOperation(request.deviceInfo)

          is DisconnectRequestEvent -> {
            if (request.isInterrupted) connecter.cancelPendingConnection(true)
            else connecter.disconnectAllBluetooth(true)
          }

          is StreamRequestEvent -> {
            when {
              request.isStartStream -> reader.startStreamOperation(request.monitorDeviceStatus())
              request.isStopStream -> reader.stopStreamOperation()
              else -> isRequestProcessing = false
            }
          }

          is CommandRequestEvent -> MbtDataBluetooth.instance.sendCommand(request.command)

          else -> isRequestProcessing = false
        }
      }
    }

    fun isOperationWaiting(): Boolean { return asyncOperation.isWaiting }
    fun isSwitchOperationWaiting(): Boolean { return asyncSwitchOperation.isWaiting }
    fun stopWaitingOperation(isCancel: Boolean?) {  asyncOperation.stopWaitingOperation(isCancel) }
    fun startSwitchWaitingOperation(timeout : Int) { try { asyncSwitchOperation.waitOperationResult(timeout) } catch (e: Exception) { }}
    fun stopSwitchWaitingOperation() { asyncSwitchOperation.stopWaitingOperation(false) }

    fun parseRequest(request: BluetoothRequests){
      requestHandler.post (Runnable { requestThread.parseRequest(request) })
    }
  }

  //----------------------------------------------------------------------------
  // BUS CALLBACKS
  //----------------------------------------------------------------------------
  /** Notify the Connection process handler if the MTU has been well changed
   * @param command is the corresponding type of bluetooth command
   */
  public fun onBluetoothResponseReceived(response: Any?, command: BluetoothCommand<*, *>) {
    if (command is Mtu) {
      connecter.updateConnectionState(true) //current state is set to BT_PARAMETERS_CHANGED
      connecter.switchToNextConnectionStep()
    }
  }
  /** Add the new [BluetoothRequests] to the handler thread that will execute tasks one after another
   * This method must return quickly in order not to block the thread.
   * @param request the new [BluetoothRequests] to execute
   */
  @Subscribe(threadMode = ThreadMode.ASYNC)
  public fun onNewBluetoothRequest(request: BluetoothRequests) {
    if (request is DisconnectRequestEvent && request.isInterrupted) connecter.cancelPendingConnection(true)//Specific case: disconnection has main priority so we don't add it to queue
    else parseRequest(request) // enqueue a Runnable object to be called by the Handler message queue when they are received. When posting or sending to a Handler, the item is processed as soon as the message queue is ready to do so
  }

  //----------------------------------------------------------------------------
  // COMMUNICATION WITHIN THE BLUETOOTH UNIT
  //----------------------------------------------------------------------------
  fun onConnectionStateChanged(state: BluetoothState){
    MbtDataBluetooth.instance.notifyConnectionStateChanged(state)
  }

  fun setRequestProcessing(isRequestProcessing: Boolean){ requestProcessor.isRequestProcessing = isRequestProcessing}
  fun isOperationWaiting(): Boolean { return requestProcessor.isOperationWaiting() }
  fun isSwitchOperationWaiting(): Boolean { return requestProcessor.isSwitchOperationWaiting() }

  fun stopWaitingOperation(isCancel: Boolean?) { if (isOperationWaiting()) requestProcessor.stopWaitingOperation(isCancel) }
  fun stopSwitchWaitingOperation() { if (isSwitchOperationWaiting()) requestProcessor.stopSwitchWaitingOperation() }
  fun startSwitchWaitingOperation(timeout : Int) { requestProcessor.startSwitchWaitingOperation(timeout) }

  fun parseRequest(request: BluetoothRequests){
    requestProcessor.parseRequest(request)
  }
  fun tryOperation(operation: ()-> Unit, timeout: Int) {
    requestProcessor.asyncOperation.tryOperation(operation, timeout)
  }
  fun tryOperation(operation: ()-> Unit, catchCallback: BaseErrorEvent<BaseError>?, finally: (()-> Unit)?, timeout: Int) {
    requestProcessor.asyncOperation.tryOperation(operation, catchCallback, finally, timeout)
  }
  fun tryOperationForResult(operation: ()-> Unit, catchCallback: BaseErrorEvent<BaseError>?, finally: (()-> Unit)?, timeout: Int) : Boolean? {
    return requestProcessor.asyncOperation.tryOperationForResult(operation, catchCallback, finally, timeout)
  }

    //----------------------------------------------------------------------------
  // COMMUNICATION WITH OTHER UNITS MANAGERS
  //----------------------------------------------------------------------------

  fun notifyDataAcquired(data: ByteArray) {
    notifyEvent(BluetoothEEGEvent(data)) //MbtEEGManager will convert data from raw packets to eeg values
    setRequestProcessing(false)
  }

  fun notifyDeviceFound(newState: BluetoothState) {
    notifyEvent(ConnectionStateEvent(newState, MbtDataBluetooth.instance.currentDevice, context.deviceTypeRequested))
    if (!context.connectAudio) { return }

    if (MbtAudioBluetooth.instance == null) {
      MbtAudioBluetooth.instance = MbtBluetoothA2DP(this)
    } else if (MbtAudioBluetooth.instance?.currentDevice != null) {
      notifyEvent(DeviceEvents.AudioConnectedDeviceEvent(MbtAudioBluetooth.instance?.currentDevice))
    }
  }
  
  fun notifyDataBluetoothDisconnected() {
    if (isSwitchOperationWaiting()) stopSwitchWaitingOperation() //a new a2dp connection was detected while an other headset was connected : here the last device has been well disconnected so we can connect BLE from A2DP
    else connecter.cancelPendingConnection(false)
    connecter.onDeviceDisconnected()
  }

  fun notifyAudioBluetoothDisconnected(newState: BluetoothState) {
    MbtAudioBluetooth.instance?.notifyConnectionStateChanged(newState, false)
    notifyEvent(DeviceEvents.AudioDisconnectedDeviceEvent())
  }

  fun notifyAudioBluetoothConnected(newState: BluetoothState) {
    if (MbtAudioBluetooth.instance == null) {
      return
    }

    MbtAudioBluetooth.instance?.notifyConnectionStateChanged(newState, false)
    stopWaitingOperation(false)
    val audioDevice = MbtAudioBluetooth.instance?.currentDevice
    if (audioDevice == null || MbtDataBluetooth.instance !is MbtBluetoothLE) {
      return
    }

    val bleDeviceName = (MbtDataBluetooth.instance as MbtBluetoothLE).getBleDeviceNameFromA2dp(audioDevice.name)
    if (!connecter.isDataBluetoothConnected || !MbtDataBluetooth.instance.isCurrentDeviceNameEqual(bleDeviceName)) {
      bleDeviceName?.let { connecter.connectBLEFromA2DP(it) }
    }
    notifyEvent(DeviceEvents.AudioConnectedDeviceEvent(audioDevice))
  }

  /** Notify the DeviceManager and the EEGManager that the headset returned its stored configuration
   */
  fun notifyDeviceConfigReceived(returnedConfig: Array<Byte?>) {
    notifyEvent(connecter.connectedDevice?.let { EEGConfigEvent(it, returnedConfig) })
  }

  /** This method is called from Bluetooth classes and is meant to post an event to the main manager
   * that contains the [StreamState] new state
   * @param newStreamState the [StreamState] new state
   */
  fun notifyStreamStateChanged(newStreamState: StreamState) {
    if (newStreamState == StreamState.STOPPED || newStreamState == StreamState.STARTED)
      stopWaitingOperation(true)

    setRequestProcessing(false)
    notifyEvent(newStreamState)
  }

  fun notifyEvent(event: Any?) {
    MbtEventBus.postEvent(event)
  }
}