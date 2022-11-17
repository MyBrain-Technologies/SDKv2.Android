package com.mybraintech.sdk.core.bluetooth.devices.melomind

import java.util.*

enum class MelomindCharacteristic(val uuid: UUID) {

  AUDIO_NAME(UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")),
  SERIAL_NUMBER(UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")),
  HARDWARE_VERSION(UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")),
  FIRMWARE_VERSION(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")),
  EEG_ACQUISITION(UUID.fromString("0000b2a5-0000-1000-8000-00805f9b34fb")),
  BATTERY_LEVEL(UUID.fromString("0000b2a2-0000-1000-8000-00805f9b34fb")),

  /**
   * saturation, new dc offset...
   */
  HEADSET_STATUS(UUID.fromString("0000b2a3-0000-1000-8000-00805f9b34fb")),

  OAD_TRANSFER(UUID.fromString("0000b2a6-0000-1000-8000-00805f9b34fb")),
  MAIL_BOX(UUID.fromString("0000b2a4-0000-1000-8000-00805f9b34fb"));

}