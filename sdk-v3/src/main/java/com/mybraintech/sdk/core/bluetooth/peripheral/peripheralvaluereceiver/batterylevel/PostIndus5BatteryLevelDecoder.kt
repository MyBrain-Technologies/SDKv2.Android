package com.mybraintech.sdk.core.bluetooth.peripheral.peripheralvaluereceiver.batterylevel

class PostIndus5BatteryLevelDecoder: IBatteryLevelDecoder {

  override fun decodeBatteryValue(value: Byte): Float? {
    val intValue = value.toInt()
    val decodedBatteryLevel = when (intValue) {
      0, 1, 2, 4,  -> 0
      5 -> 12.5
      6 -> 25
      7 -> 37.5
      8 -> 50
      9 -> 62.5
      10 -> 75
      11 -> 87.5
      12 -> 100
      else -> null
    }
    return decodedBatteryLevel?.toFloat()
  }

}