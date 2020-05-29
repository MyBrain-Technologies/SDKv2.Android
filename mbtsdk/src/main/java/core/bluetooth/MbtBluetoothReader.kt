package core.bluetooth

import android.content.Context
import android.util.Log
import command.DeviceCommand
import command.DeviceCommands.*
import command.DeviceStreamingCommands.EegConfig
import command.OADCommands
import config.MbtConfig
import core.bluetooth.BluetoothInterfaces.IDeviceInfoMonitor
import core.bluetooth.BluetoothState.*
import core.bluetooth.lowenergy.MbtBluetoothLE
import core.bluetooth.requests.BluetoothRequests
import core.bluetooth.requests.ReadRequestEvent
import core.bluetooth.spp.MbtBluetoothSPP
import core.device.DeviceEvents.*
import core.device.model.DeviceInfo
import core.device.model.DeviceInfo.*
import core.device.model.MbtDevice
import engine.SimpleRequestCallback
import engine.clientevents.BaseErrorEvent
import engine.clientevents.BluetoothError
import eventbus.MbtEventBus
import eventbus.events.DeviceInfoEvent
import eventbus.events.EEGConfigEvent
import org.apache.commons.lang.ArrayUtils
import org.greenrobot.eventbus.Subscribe
import utils.MbtAsyncWaitOperation

/**
 * Created by Etienne on 08/02/2018.
 * This class contains all necessary methods to manage the Bluetooth communication with the myBrain peripheral devices.
 * - 3 Bluetooth layers are used :
 * - Bluetooth Low Energy protocol is used with Melomind Headset for communication.
 * - Bluetooth SPP protocol which is used for the VPro headset communication.
 * - Bluetooth A2DP is used for Audio stream.
 * We scan first with the Low Energy Scanner as it is more efficient than the classical Bluetooth discovery scanner.
 */
class MbtBluetoothReader(context: Context) : MbtBluetoothManager(context) {
private lateinit var bluetoothForDataStreaming: MbtDataBluetooth

  companion object {
    private val TAG = MbtBluetoothReader::class.java.simpleName
  }

  init {
  }

  private fun startReadingDeviceInfo(deviceInfo: DeviceInfo) {
    updateConnectionState(false) //current state is set to READING_FIRMWARE_VERSION or READING_HARDWARE_VERSION or READING_SERIAL_NUMBER or READING_MODEL_NUMBER
    asyncOperation.tryOperation({ startReadOperation(deviceInfo) },
        MbtConfig.getBluetoothReadingTimeout()
    )
    val deviceInfoReadingState = mapOf(
        MODEL_NUMBER to READING_SUCCESS,
        FW_VERSION to READING_FIRMWARE_VERSION_SUCCESS,
        HW_VERSION to READING_HARDWARE_VERSION_SUCCESS,
        SERIAL_NUMBER to READING_SERIAL_NUMBER_SUCCESS)
    if (currentState != deviceInfoReadingState[deviceInfo]) {
      updateConnectionState(READING_FAILURE)//at this point : current state should be READING...SUCCESS if a read result has been returned
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
      BATTERY -> bluetoothForDataStreaming.readBattery() //Initiates a read battery operation on this correct BtProtocol.
      FW_VERSION -> (bluetoothForDataStreaming as IDeviceInfoMonitor).readFwVersion() //Initiates a read firmware version operation on this correct BtProtocol
      HW_VERSION -> (bluetoothForDataStreaming as IDeviceInfoMonitor).readHwVersion() //Initiates a read hardware version operation on this correct BtProtocol
      SERIAL_NUMBER -> (bluetoothForDataStreaming as IDeviceInfoMonitor).readSerialNumber() //Initiates a read serial number operation on this correct BtProtocol
      MODEL_NUMBER -> (bluetoothForDataStreaming as IDeviceInfoMonitor).readModelNumber() //Initiates a read model number operation on this correct BtProtocol
      else -> {
        false
      }
    }
    if (hasOperationFailed) {
      isRequestProcessing(false)
      MbtEventBus.postEvent(DeviceInfoEvent<Any>(deviceInfo, null))
    }
  }

  /**
   * Initiates the acquisition of EEG data. This method chooses between the correct BtProtocol.
   * If there is already a streaming session in progress, nothing happens and the method returns silently.
   */
  private fun startStreamOperation(enableDeviceStatusMonitoring: Boolean) {
    Log.d(TAG, "Bluetooth Manager starts streaming")
    if (!bluetoothForDataStreaming.isConnected) {
      notifyStreamStateChanged(StreamState.DISCONNECTED)
      isRequestProcessing(false)
      return
    }
    if (bluetoothForDataStreaming.isStreaming) {
      isRequestProcessing(false)
      return
    }
    if (enableDeviceStatusMonitoring && bluetoothForDataStreaming is MbtBluetoothLE) (bluetoothForDataStreaming as MbtBluetoothLE).activateDeviceStatusMonitoring()
    val startSucceeded: Boolean? = asyncOperation.tryOperationForResult(
        {
          if (!bluetoothForDataStreaming.startStream()) {
            throw BluetoothError.ERROR_WRITE_CHARACTERISTIC_OPERATION
          }
        },
        BaseErrorEvent { _, _ -> bluetoothForDataStreaming.notifyStreamStateChanged(StreamState.FAILED) },
        { isRequestProcessing(false) },
        6000
    )

    if (startSucceeded != null && !startSucceeded) MbtEventBus.postEvent(StreamState.FAILED)
    isRequestProcessing(false)

  }

  /**
   * Initiates the acquisition of EEG data from the correct BtProtocol
   * If there is no streaming session in progress, nothing happens and the method returns silently.
   */
  private fun stopStreamOperation() {
    Log.d(TAG, "Bluetooth Manager stops streaming")
    if (!bluetoothForDataStreaming.isStreaming) {
      isRequestProcessing(false)
      return
    }
    val stopSucceeded: Boolean? = asyncOperation.tryOperationForResult(
        {
          if (!bluetoothForDataStreaming.stopStream()) {
            throw BluetoothError.ERROR_WRITE_CHARACTERISTIC_OPERATION
          }
        },
        BaseErrorEvent { _, _ -> bluetoothForDataStreaming.notifyStreamStateChanged(StreamState.FAILED) },
        { isRequestProcessing(false) },
        6000
    )
    if (stopSucceeded == false)
      bluetoothForDataStreaming.notifyStreamStateChanged(StreamState.FAILED)
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
   * This method is called from Bluetooth classes and is meant to post an event to the main manager
   * that contains the [DeviceInfo] with the associated value
   * @param deviceInfo the [DeviceInfo]
   * @param deviceValue the new value as String
   */
  fun notifyDeviceInfoReceived(deviceInfo: DeviceInfo, deviceValue: String) {
    Log.d(MbtBluetoothManager.TAG, " Device info returned by the headset $deviceInfo : $deviceValue")
    isRequestProcessing(false)
    if ((deviceInfo == BATTERY) && (currentState == BONDING)) {
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
    isRequestProcessing(false)
    MbtEventBus.postEvent(newStreamState)
  }

  fun notifyNewHeadsetStatus(payload: ByteArray) {
    MbtEventBus.postEvent(RawDeviceMeasure(payload))
  }

  /**
   * Notify the DeviceManager and the EEGManager that the headset returned its stored configuration
   */
  private fun notifyDeviceConfigReceived(returnedConfig: Array<Byte>) {
    MbtEventBus.postEvent(getConnectedDevice()?.let { EEGConfigEvent(it, returnedConfig) })
  }

    private fun notifyDeviceResponseReceived(response: Any?, command: DeviceCommand<*, *>) {
    if (response != null) {
      when (command) {
        is EegConfig -> notifyDeviceConfigReceived(ArrayUtils.toObject(response as ByteArray?))

        is UpdateSerialNumber -> (response as ByteArray?)?.let { String(it) }?.let { notifyDeviceInfoReceived(SERIAL_NUMBER, it) }

        is GetDeviceInfo -> {
          notifyDeviceInfoReceived(FW_VERSION, String(ArrayUtils.subarray(response as ByteArray?, 0, MbtBluetoothSPP.VERSION_NB_BYTES)))
          notifyDeviceInfoReceived(HW_VERSION, String(ArrayUtils.subarray(response as ByteArray?, MbtBluetoothSPP.VERSION_NB_BYTES, MbtBluetoothSPP.VERSION_NB_BYTES + MbtBluetoothSPP.VERSION_NB_BYTES)))
          notifyDeviceInfoReceived(SERIAL_NUMBER, String(ArrayUtils.subarray(response as ByteArray?, MbtBluetoothSPP.VERSION_NB_BYTES + MbtBluetoothSPP.VERSION_NB_BYTES, MbtBluetoothSPP.VERSION_NB_BYTES + MbtBluetoothSPP.VERSION_NB_BYTES + MbtBluetoothSPP.SERIAL_NUMBER_NB_BYTES)))
        }
        is UpdateExternalName ->
          (response as ByteArray?)?.let { String(it) }?.let { notifyDeviceInfoReceived(MODEL_NUMBER, it) }

        is GetBattery ->
          (response as Int?)?.toString()?.let { notifyDeviceInfoReceived(BATTERY, it) }

        is OADCommands -> {
          updateConnectionState(UPGRADING)
          bluetoothForAudioStreaming?.currentState = UPGRADING
          notifyEventReceived(command.identifier, response)
        }
      }
    }
  }
}