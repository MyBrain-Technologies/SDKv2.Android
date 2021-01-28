package core.bluetooth

import android.util.Log
import command.BluetoothCommand
import command.CommandInterface
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
import core.device.model.DeviceInfo
import core.device.model.DeviceInfo.*
import core.bluetooth.StreamState.*
import engine.clientevents.BaseError
import engine.clientevents.BaseErrorEvent
import engine.clientevents.BluetoothError
import eventbus.events.BluetoothResponseEvent
import eventbus.events.DeviceInfoEvent
import org.apache.commons.lang.ArrayUtils
import utils.LogUtils

/** Created by Sophie on 02/06/2020.
 * This class contains all necessary methods to manage the Bluetooth read operation with the myBrain peripheral devices.
 * A read operation is any value read/stream request sent to the headset device */
class MbtBluetoothReader(private val manager: MbtBluetoothManager) {

  private val TAG = this::class.java.simpleName


  fun startReadingDeviceInfo(deviceInfo: DeviceInfo) : BluetoothState? {
    manager.tryOperation({ startReadOperation(deviceInfo) },
        MbtConfig.getBluetoothReadingTimeout()
    )
    val deviceInfoReadingState = mapOf(
        MODEL_NUMBER to READING_SUCCESS,
        FW_VERSION to READING_FIRMWARE_VERSION_SUCCESS,
        HW_VERSION to READING_HARDWARE_VERSION_SUCCESS,
        SERIAL_NUMBER to READING_SERIAL_NUMBER_SUCCESS)
    return deviceInfoReadingState[deviceInfo]
  }

  /** If the [request][BluetoothRequests] is a [ReadRequestEvent] event, this method
   * is called to parse which read operation is to be executed according to the [DeviceInfo].
   * @param deviceInfo the [DeviceInfo] info that determine which read to operation to execute */
  fun startReadOperation(deviceInfo: DeviceInfo) {
    val hasOperationFailed = when (deviceInfo) {
      BATTERY -> MbtDataBluetooth.instance.readBattery() //Initiates a read battery operation on this correct BluetoothProtocol.
      FW_VERSION -> (MbtDataBluetooth.instance as IDeviceInfoMonitor).readFwVersion() //Initiates a read firmware version operation on this correct BluetoothProtocol
      HW_VERSION -> (MbtDataBluetooth.instance as IDeviceInfoMonitor).readHwVersion() //Initiates a read hardware version operation on this correct BluetoothProtocol
      SERIAL_NUMBER -> (MbtDataBluetooth.instance as IDeviceInfoMonitor).readSerialNumber() //Initiates a read serial number operation on this correct BluetoothProtocol
      MODEL_NUMBER -> (MbtDataBluetooth.instance as IDeviceInfoMonitor).readModelNumber() //Initiates a read model number operation on this correct BluetoothProtocol
      else -> {
        false
      }
    }
    if (hasOperationFailed) {
      manager.setRequestProcessing(false)
      manager.notifyEvent(DeviceInfoEvent<Any>(deviceInfo, null))
    }
  }

  /** Initiates the acquisition of EEG data. This method chooses between the correct BluetoothProtocol.
   * If there is already a streaming session in progress, nothing happens and the method returns silently. */
  fun startStreamOperation(enableDeviceStatusMonitoring: Boolean) {
    Log.d(TAG, "Bluetooth reader starts streaming")
    if (!MbtDataBluetooth.instance.isConnected) {
      manager.notifyStreamStateChanged(DISCONNECTED)
      return
    }
    if (MbtDataBluetooth.instance.isStreaming) {
      manager.setRequestProcessing(false)
      return
    }
    if (enableDeviceStatusMonitoring && MbtDataBluetooth.instance is MbtBluetoothLE) {
      (MbtDataBluetooth.instance as MbtBluetoothLE).activateDeviceStatusMonitoring()
    }
    manager.tryOperationForResult(
        { if (!MbtDataBluetooth.instance.startStream()) {
            throw BluetoothError.ERROR_WRITE_CHARACTERISTIC_OPERATION
          }
        },
        object : BaseErrorEvent<BaseError> {
          override fun onError(error: BaseError, additionalInfo: String?) {
            MbtDataBluetooth.instance.notifyStreamStateChanged(FAILED)
          }
        },
        { manager.setRequestProcessing(false)},
        6000
    )
  }

  /** Initiates the acquisition of EEG data from the correct BluetoothProtocol
   * If there is no streaming session in progress, nothing happens and the method returns silently. */
  fun stopStreamOperation() {
    Log.d(TAG, "Bluetooth reader stops streaming")
    if (!MbtDataBluetooth.instance.isStreaming) {
      manager.setRequestProcessing(false)
      return
    }
    manager.tryOperationForResult(
        { if (!MbtDataBluetooth.instance.stopStream()) {
            throw BluetoothError.ERROR_WRITE_CHARACTERISTIC_OPERATION
          } },
        object : BaseErrorEvent<BaseError> {
          override fun onError(error: BaseError, additionalInfo: String?) {
            MbtDataBluetooth.instance.notifyStreamStateChanged(FAILED)
          }
        },
        { manager.setRequestProcessing(false)},
        6000
    )
  }

  /** This method is called from Bluetooth classes and is meant to post an event to the main manager
   * that contains the [DeviceInfo] with the associated value
   * @param deviceInfo the [DeviceInfo]
   * @param deviceValue the new value as String */
  fun notifyDeviceInfoReceived(deviceInfo: DeviceInfo, deviceValue: String?) {
    Log.d(TAG, " Device info returned by the headset $deviceInfo : $deviceValue")
    manager.setRequestProcessing(false)
    if (deviceInfo == BATTERY) {
      if (MbtDataBluetooth.instance.currentState == BONDING) {
        val connecter = manager.connecter
        connecter.updateConnectionState(false) //current state is set to BONDED
        connecter.switchToNextConnectionStep()
      } else if(deviceValue != null)
        manager.notifyEvent(DeviceInfoEvent(deviceInfo, deviceValue))

    } else {
      manager.notifyEvent(DeviceInfoEvent(deviceInfo, deviceValue))
    }
  }

  fun notifyDeviceResponseReceived(response: Any?, command: DeviceCommand<*, *>) {
    if (response != null) {
      when (command) {
        is EegConfig -> manager.notifyDeviceConfigReceived(ArrayUtils.toObject(response as ByteArray?))

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
          manager.connecter.updateConnectionState(UPGRADING)
          MbtAudioBluetooth.instance?.currentState = UPGRADING
          manager.notifyEvent(BluetoothResponseEvent(command.identifier, response))
        }
      }
    }
  }

  /** Notify a command response has been received from the headset
   * @param command is the corresponding type of command */
  fun notifyResponseReceived(response : Any?, command : CommandInterface.MbtCommand<*>) {
    LogUtils.d(TAG, "Received response from device : $response")

    if(command is DeviceCommand<*,*>)
      notifyDeviceResponseReceived(response, command)

    else if (command is BluetoothCommand<*,*>)
      manager.onBluetoothResponseReceived(response, command)

    manager.setRequestProcessing(false);
  }
}