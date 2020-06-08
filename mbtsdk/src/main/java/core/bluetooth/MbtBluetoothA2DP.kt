package core.bluetooth

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioManager
import android.util.Log
import config.MbtConfig
import core.bluetooth.MbtAudioBluetooth.ExtraBluetooth
import core.device.model.MelomindsQRDataBase
import features.MbtFeatures
import utils.LogUtils
import utils.MbtAsyncWaitOperation
import utils.VersionHelper
import java.util.*
import java.util.concurrent.CancellationException

/**
 * Created by Etienne on 08/02/2018.
 */
class MbtBluetoothA2DP(manager: MbtBluetoothManager) : ExtraBluetooth(BluetoothProtocol.A2DP, manager) {
  private var a2dpProxy: BluetoothA2dp? = null
  private val asyncInit = MbtAsyncWaitOperation<Boolean?>()
  private val asyncConnection = MbtAsyncWaitOperation<Boolean?>()
  private val asyncDisconnection = MbtAsyncWaitOperation<Boolean?>()

  /**
   * A Bluetooth connection lets users stream audio on Bluetooth-enabled devices.
   * The connect method will attempt to connect to the headset via the Advanced Audio Distribution Profile protocol.
   * This protocol is used to transmit the stereo audio signals.
   * Since Android's A2DP Bluetooth class has many hidden methods we use reflexion to access them.
   * This method can handle the case where the end-user device is already connected to the melomind via
   * the A2DP protocol or to another device in which case it will disconnected (Android support only
   * one A2DP headset at the time).
   * @param deviceToConnect    the device to connect to
   * @param context   the application context
   * @return          `true` upon success, `false otherwise`
   */
  override fun connect(context: Context, deviceToConnect: BluetoothDevice): Boolean {
    if (deviceToConnect == null) return false
    LogUtils.d(TAG, "Attempting to connect A2DP to " + deviceToConnect.name + " address is " + deviceToConnect.address)

    // First we retrieve the Audio Manager that will help monitor the A2DP status
    // and then the instance of Bluetooth A2DP to make the necessaries calls
    if (a2dpProxy == null) { // failed to retrieve instance of Bluetooth A2DP within alloted time (5 sec)
      LogUtils.e(TAG, "Failed to retrieve instance of Bluetooth A2DP. Cannot perform A2DP operations")
      //notifyConnectionStateChanged(BluetoothState.CONNECTION_FAILURE);
      return false
    }

    // First we check if the end-user device is currently connected to an A2DP device
    if (hasA2DPDeviceConnected()) {
      // End-user is indeed connected to an A2DP device. We retrieve it to see if it is the melomind
      LogUtils.d(TAG, "User device is currently connected to an A2DP Headset. Checking if it is the melomind")

//            if (a2dpProxy.getcurrentDevices() == null || a2dpProxy.getcurrentDevices().isEmpty())
//                connect(context,deviceToConnect); // Somehow end-user is no longer connected (should not happen)

      // we assume there is only one, because Android can only support one at the time
      val deviceConnected = a2dpProxy!!.connectedDevices[0]
      if (deviceConnected.address == deviceToConnect.address) { // already connected to the Melomind !
        LogUtils.d(TAG, "Already connected to the melomind.")
        notifyConnectionStateChanged(BluetoothState.AUDIO_BT_CONNECTION_SUCCESS)
        return true
      } else {
        // The user device is currently connected to a headset that is not the melomind
        // so we disconnect it now and then we connect it to the melomind
        try {
          val result = a2dpProxy!!.javaClass.getMethod(DISCONNECT_METHOD, BluetoothDevice::class.java)
              .invoke(a2dpProxy, deviceConnected) as Boolean
          if (!result) { // according to doc : "false on immediate error, true otherwise"
            //notifyConnectionStateChanged(BluetoothState.CONNECTION_FAILURE);
            return false
          }
          // Since the disconnecting process is asynchronous we use a timer to monitor the status for a short while
          Timer(true).scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
              if (!hasA2DPDeviceConnected()) { // the user device is no longer connected to the wrong headset
                cancel()
                asyncConnection.stopWaitingOperation(false)
                notifyConnectionStateChanged(BluetoothState.AUDIO_BT_DISCONNECTED)
              }
            }
          }, 100, 500)
          var status: Boolean? = false
          try {
            status = asyncConnection.waitOperationResult(5000) as Boolean?
          } catch (e: Exception) {
            if (e is CancellationException) asyncConnection.resetWaitingOperation()
          }
          return if (status != null && status) {
            LogUtils.i(TAG, "successfully disconnected from A2DP device -> " + deviceConnected.name)
            LogUtils.i(TAG, "Now connecting A2DP to " + deviceToConnect.address)
            connect(context, deviceToConnect)
          } else {
            LogUtils.e(TAG, "failed to connect A2DP! future has timed out...")
            //notifyConnectionStateChanged(BluetoothState.CONNECTION_FAILURE);
            false
          }
        } catch (e: Exception) {
          val errorMsg = " -> " + e.message
          if (e is NoSuchMethodException) LogUtils.e(TAG, "Failed to find disconnect method via reflexion$errorMsg") else if (e is IllegalAccessException) LogUtils.e(TAG, "Failed to access disconnect method via reflexion$errorMsg") else LogUtils.e(TAG, "Failed to invoke disconnect method via reflexion$errorMsg")
          Log.getStackTraceString(e)
        }
      }
    } else {
      LogUtils.i(TAG, "Initiate connection via A2DP! ")
      // Safe to connect to melomind via A2DP
      try {
        val result = a2dpProxy!!.javaClass
            .getMethod(CONNECT_METHOD, BluetoothDevice::class.java)
            .invoke(a2dpProxy, deviceToConnect) as Boolean
        if (!result) { // according to doc : "false on immediate error, true otherwise"
          notifyConnectionStateChanged(BluetoothState.CONNECTION_FAILURE)
          return false
        }

        // Since the disconnecting process is asynchronous we use a timer to monitor the status for a short while
        Timer(true).scheduleAtFixedRate(object : TimerTask() {
          override fun run() {
            if (hasA2DPDeviceConnected()) {
              cancel()
              asyncConnection.stopWaitingOperation(false)
            }
          }
        }, 100, 1500)

        // we give 20 seconds to the user to accepting bonding request
        val timeout = if (deviceToConnect.bondState == BluetoothDevice.BOND_BONDED) MbtConfig.getBluetoothA2DpConnectionTimeout() else 25000
        var status: Boolean? = false
        try {
          status = asyncConnection.waitOperationResult(timeout) as Boolean?
        } catch (e: Exception) {
          if (e is CancellationException) asyncConnection.resetWaitingOperation()
          LogUtils.i(TAG, " A2dp Connection failed: $e")
        }
        return if (status != null && status) {
          LogUtils.i(TAG, "Successfully connected via A2DP to " + deviceToConnect.address)
          notifyConnectionStateChanged(BluetoothState.AUDIO_BT_CONNECTION_SUCCESS)
          true
        } else {
          LogUtils.i(TAG, "Cannot connect to A2DP device " + deviceToConnect.address)
          notifyConnectionStateChanged(BluetoothState.CONNECTION_FAILURE)
          false
        }
      } catch (e: Exception) {
        val errorMsg = " -> " + e.message
        if (e is NoSuchMethodException) LogUtils.e(TAG, "Failed to find connect method via reflexion$errorMsg") else if (e is IllegalAccessException) LogUtils.e(TAG, "Failed to access connect method via reflexion$errorMsg") else LogUtils.e(TAG, "Failed to invoke connect method via reflexion$errorMsg")
        Log.getStackTraceString(e)
      }
    }
    return false
  }

  fun initA2dpProxy() {
    if (bluetoothAdapter != null) A2DPAccessor().initA2DPProxy(context, bluetoothAdapter!!)
  }

  /**
   * This method will attempt to disconnect to the current a2dp device via the A2DP protocol.
   * Since Android's A2DP Bluetooth class has many hidden methods we use reflexion to access them.
   * This method can handle the case where the end-user device is already connected to the melomind via
   * the A2DP protocol or to another device in which case it will disconnected (Android support only
   * one A2DP headset at the time).
   * @return `true` by default
   */
  override fun disconnect(): Boolean {
    if (asyncConnection.isWaiting) {
      asyncConnection.stopWaitingOperation(null)
    } else {
      LogUtils.d(TAG, "Disconnect audio")
      if (bluetoothAdapter != null) {
        currentDevice = null
        if (a2dpProxy != null && a2dpProxy!!.connectedDevices.size > 0) currentDevice = a2dpProxy!!.connectedDevices[0] //assuming that one device is connected and its obviously the melomind
        val device = manager.connecter.connectedDevice ?: return true
        if (VersionHelper(device.firmwareVersion.toString()).isValidForFeature(VersionHelper.Feature.A2DP_FROM_HEADSET)) {
          manager.connecter.disconnectA2DPFromBLE()
          try {
            asyncDisconnection.waitOperationResult(MbtConfig.getBluetoothA2DpConnectionTimeout())
          } catch (e: Exception) {
            if (e is CancellationException) asyncDisconnection.resetWaitingOperation()
          } finally {
            asyncDisconnection.stopWaitingOperation(null)
          }
        }
        if (isConnected) {
          try {
            if (a2dpProxy != null) a2dpProxy!!.javaClass.getMethod(DISCONNECT_METHOD, BluetoothDevice::class.java).invoke(a2dpProxy, currentDevice)
            currentDevice = null
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }
    }
    return true
  }

  /**
   * Checks whether a Bluetooth A2DP audio peripheral is connected or not.
   * @return true if a Bluetooth A2DP audio peripheral is connected, false otherwise
   */
  private fun hasA2DPDeviceConnected(): Boolean {
    // First we retrieve the Audio Manager that will help monitor the A2DP status
    // and then the instance of Bluetooth A2DP to make the necessaries calls
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        ?: return false
    // First we check if the end-user device is currently connected to an A2DP device
    return audioManager.isBluetoothA2dpOn
  }

  private val a2DPcurrentDevices: List<BluetoothDevice>
    private get() = if (a2dpProxy == null) emptyList() else a2dpProxy!!.connectedDevices

  override val isConnected: Boolean
    get() = currentState == BluetoothState.AUDIO_BT_CONNECTION_SUCCESS

  fun resetA2dpProxy(state: Int) {
    if (state == BluetoothAdapter.STATE_ON && a2dpProxy == null && bluetoothAdapter != null) A2DPAccessor().initA2DPProxy(context, bluetoothAdapter!!)
  }

  override fun startStream(): Boolean {
    return false
  }

  override fun stopStream(): Boolean {
    return false
  }

  override fun notifyConnectionStateChanged(newState: BluetoothState, notifyManager: Boolean) {
    currentState = newState
    if (newState == BluetoothState.AUDIO_BT_DISCONNECTED && asyncDisconnection.isWaiting) asyncDisconnection.stopWaitingOperation(false) else if (newState == BluetoothState.AUDIO_BT_CONNECTION_SUCCESS) {
      if (asyncConnection.isWaiting) asyncConnection.stopWaitingOperation(false)
      if (notifyManager) //if audio is connected (and BLE is not) when the user request connection to a headset
        manager.connecter.notifyConnectionStateChanged(BluetoothState.AUDIO_BT_CONNECTION_SUCCESS)
    }
  }

  fun isPairedDevice(device: BluetoothDevice?): Boolean {
    return bluetoothAdapter != null && bluetoothAdapter!!.bondedDevices.contains(device)
  }

  internal inner class A2DPAccessor : BluetoothProfile.ServiceListener {
    private val a2DPMonitor: A2DPMonitor? = A2DPMonitor()

    /**
     * The Advanced Audio Distribution Profile (A2DP) profile defines how high quality audio can be streamed from one device to another over a Bluetooth connection.
     * Android provides the BluetoothA2dp class, which is a proxy for controlling the Bluetooth A2DP Service.
     * This method blocks for 5 sec
     * onds while attempting to retrieve the instance of `BluetoothA2dp`
     * @param context   the application context
     * @param adapter   the Bluetooth Adapter to retrieve the BluetoothA2DP from
     */
    fun initA2DPProxy(context: Context, adapter: BluetoothAdapter) {
      adapter.getProfileProxy(context, this, BluetoothProfile.A2DP) //establish the connection to the proxy
      try {
        asyncInit.waitOperationResult(3000)
      } catch (e: Exception) {
        LogUtils.d(TAG, " No audio connected device ")
      }
    }

    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
      if (profile == BluetoothProfile.A2DP) {
        a2dpProxy = proxy as BluetoothA2dp
        a2DPMonitor?.start(500) //init A2DP state
      }
    }

    override fun onServiceDisconnected(profile: Int) {
      if (profile == BluetoothProfile.A2DP) {
        Log.w(TAG, "device is disconnected from service")
        a2DPMonitor!!.stop()
      }
    }
  }

  /**
   *
   */
  internal inner class A2DPMonitor {
    private var pollingTimer: Timer? = null
    private var connectedA2DpDevices: List<BluetoothDevice>
    fun start(pollingMillis: Int) {
      pollingTimer = Timer()
      pollingTimer!!.scheduleAtFixedRate(Task(), 200, pollingMillis.toLong())
    }

    fun stop() {
      if (pollingTimer != null) {
        pollingTimer!!.cancel()
        pollingTimer!!.purge()
        pollingTimer = null
      }
    }//if device name is a valid BLE name
    //or if device name is a valid QR Code name

    //if QR code contains only 9 digits
    private val isCurrentDeviceNameValid: Boolean
      get() {
        if (currentDevice?.name?.startsWith(MelomindsQRDataBase.QR_PREFIX) == true && currentDevice?.name?.length == MelomindsQRDataBase.QR_LENGTH - 1) //if QR code contains only 9 digits
          currentDevice?.name + MelomindsQRDataBase.QR_SUFFIX
        return (currentDevice?.name?.startsWith(MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX) == true || currentDevice?.name?.startsWith(MbtFeatures.A2DP_DEVICE_NAME_PREFIX) == true//if device name is a valid BLE name
            || currentDevice?.name?.startsWith(MelomindsQRDataBase.QR_PREFIX) == true && currentDevice?.name?.length == MelomindsQRDataBase.QR_LENGTH //or if device name is a valid QR Code name
            )
      }

    private inner class Task : TimerTask() {
      override fun run() {
        if (connectedA2DpDevices != a2DPcurrentDevices) { //It means that something has changed. Now we need to find out what changed (getAD2PcurrentDevices returns the connected devices for this specific profile.)
          if (connectedA2DpDevices.size < a2DPcurrentDevices.size) { //Here, we have a new A2DP connection then we notify bluetooth manager
            val previousDevice = currentDevice
            currentDevice = a2DPcurrentDevices[a2DPcurrentDevices.size - 1] //As one a2dp output is possible at a time on android, it is possible to consider that last item in list is the current one
            if (hasA2DPDeviceConnected() &&  currentDevice?.name != null && isCurrentDeviceNameValid) { //if a Bluetooth A2DP audio peripheral is connected to a device whose name is not null.
              LogUtils.d(TAG, "Detected connected device " + currentDevice?.name + " address is " + currentDevice?.address)
              if (previousDevice == null || currentDevice != null && currentDevice !== previousDevice) notifyConnectionStateChanged(BluetoothState.AUDIO_BT_CONNECTION_SUCCESS, true)
              asyncInit.stopWaitingOperation(false)
            }
          } else  //Here, either the A2DP connection has dropped or a new A2DP device is connecting.
            notifyConnectionStateChanged(BluetoothState.AUDIO_BT_DISCONNECTED)
          connectedA2DpDevices = a2DPcurrentDevices //In any case, it is mandatory to updated our local connected A2DP list
        }
      }
    }

    init {
      connectedA2DpDevices = emptyList()
    }
  }

  companion object {
    private val TAG = MbtBluetoothA2DP::class.java.simpleName
    private const val CONNECT_METHOD = "connect"
    private const val DISCONNECT_METHOD = "disconnect"
    var instance: MbtBluetoothA2DP? = null
    fun initInstance(manager: MbtBluetoothManager): ExtraBluetooth {
      return MbtBluetoothA2DP(manager).also { instance = it }
    }
  }
}