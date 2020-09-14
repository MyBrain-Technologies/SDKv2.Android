package eventbus.events

import android.bluetooth.BluetoothDevice
import core.bluetooth.BluetoothState
import core.device.model.MbtDevice
import core.device.model.MelomindDevice
import core.device.model.VProDevice
import features.MbtDeviceType

class ConnectionStateEvent : IEvent {
  var newState: BluetoothState
    private set
  var additionalInfo: String? = null
    private set
  var device: MbtDevice? = null
    private set

  constructor(newState: BluetoothState) {
    this.newState = newState
  }

  constructor(newState: BluetoothState, device: MbtDevice?) {
    this.newState = newState
    this.device = device
  }

  constructor(newState: BluetoothState, device: BluetoothDevice, deviceType: MbtDeviceType) {
    this.newState = newState
    this.device = if (deviceType == MbtDeviceType.MELOMIND) MelomindDevice(device) else VProDevice(device)
  }

  constructor(newState: BluetoothState, additionalInfo: String?) {
    this.newState = newState
    this.additionalInfo = additionalInfo
  }

}