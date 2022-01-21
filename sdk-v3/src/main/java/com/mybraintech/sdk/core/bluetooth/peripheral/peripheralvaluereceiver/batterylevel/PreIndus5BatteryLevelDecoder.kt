package com.mybraintech.sdk.core.bluetooth.peripheral.peripheralvaluereceiver.batterylevel

class PreIndus5BatteryLevelDecoder: IBatteryLevelDecoder {

  override fun decodeBatteryValue(value: Byte): Float? {
    val intValue = value.toInt()
    val decodedBatteryLevel = when (intValue) {
      0 -> 0
      1 -> 15
      2 -> 30
      3 -> 50
      4 -> 65
      5 -> 85
      6 -> 100
      else -> null
    }
    return decodedBatteryLevel?.toFloat()
  }

}