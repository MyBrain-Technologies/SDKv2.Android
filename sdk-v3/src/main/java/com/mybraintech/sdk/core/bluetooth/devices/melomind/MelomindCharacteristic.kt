package com.mybraintech.sdk.core.bluetooth.devices.melomind

import java.util.*

enum class MelomindCharacteristic(val uuid: UUID) {

  ProductName(UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")),
  SerialNumber(UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")),
  HardwareRevision(UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")),
  FirmwareRevision(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")),
  EEG_ACQUISITION(UUID.fromString("0000b2a5-0000-1000-8000-00805f9b34fb")),
  DeviceBatteryStatus(UUID.fromString("0000b2a2-0000-1000-8000-00805f9b34fb")),
  HEADSET_STATUS(UUID.fromString("0000b2a3-0000-1000-8000-00805f9b34fb")),
  OadTransfert(UUID.fromString("0000b2a6-0000-1000-8000-00805f9b34fb")),
  MailBox(UUID.fromString("0000b2a4-0000-1000-8000-00805f9b34fb"));

}