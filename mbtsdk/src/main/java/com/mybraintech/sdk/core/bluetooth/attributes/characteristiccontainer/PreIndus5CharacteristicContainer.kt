package com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer

import android.bluetooth.BluetoothGattCharacteristic

data class PreIndus5CharacteristicContainer(
  val productName: BluetoothGattCharacteristic,
  val serialNumber: BluetoothGattCharacteristic,
  val hardwareRevision: BluetoothGattCharacteristic,
  val firmwareRevision: BluetoothGattCharacteristic,
  val brainActivityMeasurement: BluetoothGattCharacteristic,
  val deviceState: BluetoothGattCharacteristic,
  val headsetStatus: BluetoothGattCharacteristic,
  val mailBox: BluetoothGattCharacteristic,
  val oadTransfer: BluetoothGattCharacteristic,
) {
  val deviceInformation =
    arrayOf(productName, serialNumber, hardwareRevision, firmwareRevision)
}
