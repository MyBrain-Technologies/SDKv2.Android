package core.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.util.Pair
import core.bluetooth.BluetoothInterfaces.IConnect
import core.bluetooth.BluetoothInterfaces.IStream
import core.device.model.DeviceInfo
import utils.LogUtils
import utils.MbtAsyncWaitOperation
import utils.MbtLock

/**
 *
 * Abstract class that contains all fields and methods that are common to the different bluetooth types.
 * It implements[BluetoothInterfaces] interface as all bluetooth types shares this
 * functionnalities.
 *
 * Created by Etienne on 08/02/2018.
 */
abstract class MbtBluetooth(protocol: BluetoothProtocol, protected var manager: MbtBluetoothManager) : IConnect, IStream {
  private var streamState = StreamState.IDLE //todo streamState variable can be inherited from Stream abstract class instead of IStream interface that just implements methods

  @Volatile
  var currentState = BluetoothState.IDLE //todo add @NonNull annotation + rename into bluetoothState
   set(currentState) {
     if (field != currentState) {
       field = currentState
     }
   }
  protected var batteryValueAtTimestamp: Pair<String, Long>? = null
  protected var bluetoothAdapter: BluetoothAdapter? = null

  @JvmField
  protected val context: Context
  var currentDevice: BluetoothDevice? = null

  protected var protocol: BluetoothProtocol
  private var lock: MbtAsyncWaitOperation<Any> = MbtAsyncWaitOperation()
  open fun notifyDeviceInfoReceived(deviceInfo: DeviceInfo, deviceValue: String) { // This method will be called when a DeviceInfoReceived is posted (fw or hw or serial number) by MbtBluetoothLE or MbtBluetoothSPP
    manager.reader.notifyDeviceInfoReceived(deviceInfo, deviceValue)
  }

  /**
   * Set the current bluetooth connection state to the value given in parameter
   * and notify the bluetooth manager of this change.
   * This method should be called if something went wrong during the connection process, as it stops the connection prccess.
   * The [MbtBluetoothConnecter.updateConnectionState]  method with no parameter should be call if nothing went wrong and user wants to continue the connection process
   */
  override fun notifyConnectionStateChanged(newState: BluetoothState) {
    if (newState != currentState && !(newState.isAFailureState && currentState == BluetoothState.DATA_BT_DISCONNECTED)) {
      val previousState = currentState
      currentState = newState
      LogUtils.i(TAG, " current state is now  =  $currentState")
      manager.connecter.notifyConnectionStateChanged(newState)
      if (currentState.isResettableState(previousState)) {  //if a disconnection occurred
        resetCurrentState() //reset the current connection state to IDLE
        if (this is MbtBluetoothA2DP && currentState != BluetoothState.UPGRADING) manager.connecter.disconnectAllBluetooth(false) //audio has failed to connect : we disconnect BLE
      }
      if (currentState.isDisconnectableState) //if a failure occurred //todo check if a "else" is not missing here
        disconnect() //disconnect if a headset is connected
    }
  }

  private fun resetCurrentState() {
    notifyConnectionStateChanged(BluetoothState.IDLE)
  }

  open fun notifyConnectionStateChanged(newState: BluetoothState, notifyManager: Boolean) {
    if (notifyManager) notifyConnectionStateChanged(newState) else {
      currentState = newState
    }
  }

  protected open fun notifyBatteryReceived(value: Int) {
    batteryValueAtTimestamp = Pair.create(value.toString(), System.currentTimeMillis())
    manager.reader.notifyDeviceInfoReceived(DeviceInfo.BATTERY, value.toString()) //todo keep battery value as integer ?
  }

  fun notifyHeadsetStatusEvent(code: Byte, value: Int) { //todo : only available in Melomind to this day > see if vpro will need it
//        if(this.headsetStatusListener != null){
//            if(code == 0x01)
//                this.headsetStatusListener.onSaturationStateChanged(value);
//            else if (code == 0x02)
//                this.headsetStatusListener.onNewDCOffsetMeasured(value);
//        }
  }

  // Events Registration
  fun notifyNewDataAcquired(data: ByteArray) { //todo call this method to notify the device manager (and all the managers in general) when the bluetooth receives data from the headset (saturation & offset for example instead of notifyNewHeadsetStatus located in the MbtBluetoothLe)
    manager.notifyDataAcquired(data)
  }

  @Synchronized
  fun enableBluetoothOnDevice(): Boolean {
    if (bluetoothAdapter?.isEnabled == true) return true
    val lock = MbtLock<Boolean>()
    // Registering for bluetooth state change
    val bluetoothStateFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
    context.registerReceiver(object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
        if (state == BluetoothAdapter.STATE_ON) {
          // Bluetooth is now turned on
          context.unregisterReceiver(this)
          lock.setResultAndNotify(java.lang.Boolean.TRUE)
        }
      }
    }, bluetoothStateFilter)

    // Turning Bluetooth ON and waiting...
    bluetoothAdapter?.enable()
    val b = lock.waitAndGetResult(5000)
    if (b == null) {
      Log.e(TAG, "impossible to enable BT adapter")
      return false
    }
    return b
  }

  open fun isCurrentDeviceNameEqual(deviceName: String): Boolean {
    return currentDevice?.name == deviceName
  }

  protected val isAdapterReady: Boolean
    get() = bluetoothAdapter?.isEnabled == true && bluetoothAdapter?.state == BluetoothAdapter.STATE_ON

  protected val isConnectedDeviceReadyForCommand: Boolean
    get() = currentState.ordinal >= BluetoothState.DATA_BT_CONNECTION_SUCCESS.ordinal

  /**
   * This method waits until the device has returned a response
   * related to the SDK request (blocking method).
   */
  protected fun startWaitingOperation(timeout: Int): Any? { //todo rename startWait/wait
    Log.d(TAG, "Wait response of device command ")
    try {
      return lock.waitOperationResult(timeout)
    } catch (e: Exception) {
      LogUtils.e(TAG, "Device command response not received : $e")
    }
    return null
  }

  fun stopWaitingOperation(response: Any) {
    lock.stopWaitingOperation(response)
  }

  @VisibleForTesting
  fun setLock(lock: MbtAsyncWaitOperation<Any>) {
    this.lock = lock
  }

  /**
   *
   * @return true if a streaming is in progress, false otherwise
   */
  override val isStreaming: Boolean
    get() = streamState == StreamState.STARTED || streamState == StreamState.STREAMING

  /**
   * Whenever there is a new stream state, this method is called to notify the bluetooth manager about it.
   * @param newStreamState the new stream state based on [the StreamState enum][StreamState]
   */
  override fun notifyStreamStateChanged(newStreamState: StreamState) {
    LogUtils.i(TAG, "new stream state $newStreamState")
    streamState = newStreamState
    manager.notifyStreamStateChanged(newStreamState)
  }

  /**
   * Disable then enable the bluetooth adapter
   */
  fun resetMobileDeviceBluetoothAdapter(): Boolean {
    LogUtils.d(TAG, "Reset Bluetooth adapter")
    bluetoothAdapter?.disable()
    try {
      Thread.sleep(500)
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
    return enableBluetoothOnDevice()
  }

  companion object {
    //todo refactor interfaces > abstract classes : see streamstate comments to understand
    private val TAG = MbtBluetooth::class.java.name
  }

  init {
    val context = manager.context.context
    this.context = context.applicationContext
    this.protocol = protocol
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    bluetoothAdapter = bluetoothManager.adapter
    if (bluetoothAdapter == null) bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() //try another way to get the adapter
  }
}