package com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer

import android.bluetooth.BluetoothGattCharacteristic

data class PostIndus5CharacteristicContainer(
  val tx: BluetoothGattCharacteristic,
  val rx: BluetoothGattCharacteristic,
  val unknown: BluetoothGattCharacteristic
)
