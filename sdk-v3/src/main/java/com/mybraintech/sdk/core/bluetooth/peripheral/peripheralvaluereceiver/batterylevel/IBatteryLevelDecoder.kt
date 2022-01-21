package com.mybraintech.sdk.core.bluetooth.peripheral.peripheralvaluereceiver.batterylevel

interface IBatteryLevelDecoder {
  fun decodeBatteryValue(value: Byte): Float?
}