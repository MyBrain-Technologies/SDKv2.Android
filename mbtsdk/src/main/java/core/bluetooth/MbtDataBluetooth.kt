package core.bluetooth

import core.bluetooth.BluetoothInterfaces.IDataBluetooth

class MbtDataBluetooth {
companion object{
  lateinit var instance : MainBluetooth
  fun isInitialized() : Boolean{
    return ::instance.isInitialized
  }
}


abstract class MainBluetooth(
    protocol: BluetoothProtocol,
    manager: MbtBluetoothManager) :

    MbtBluetooth(
    protocol,
    manager),

    IDataBluetooth

}