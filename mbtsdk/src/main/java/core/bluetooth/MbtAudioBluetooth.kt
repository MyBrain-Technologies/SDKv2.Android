package core.bluetooth

import core.bluetooth.BluetoothInterfaces.IAudioBluetooth

class MbtAudioBluetooth {
  companion object{
    var instance : ExtraBluetooth? = null
  }
  abstract class ExtraBluetooth internal constructor(
      protocol: BluetoothProtocol?,
      mbtBluetoothManager: MbtBluetoothManager) :

      MbtBluetooth(
          protocol,
          mbtBluetoothManager),

      IAudioBluetooth
}