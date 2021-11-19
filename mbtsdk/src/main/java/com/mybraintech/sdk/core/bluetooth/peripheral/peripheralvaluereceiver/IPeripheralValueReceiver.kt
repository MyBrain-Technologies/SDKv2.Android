package com.mybraintech.sdk.core.bluetooth.peripheral.peripheralvaluereceiver

import android.bluetooth.BluetoothGattCharacteristic

interface IPeripheralValueReceiver {
  var listener: IPeripheralValueReceiverListener?

  fun handleValueUpdateFor(characteristic: BluetoothGattCharacteristic,
                           status: Int)

  fun handleNotificationStateUpdateFor(
    characteristic: BluetoothGattCharacteristic,
    status: Int
  )

  fun handleValueWriteFor(characteristic: BluetoothGattCharacteristic,
                          status: Int)
}