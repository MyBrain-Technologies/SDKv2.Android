package com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.characteristics

import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.IMBTAttribute
import java.util.*

enum class PreIndus5Characteristic(val raw: UUID) {

  //----------------------------------------------------------------------------
  // MARK: - Cases
  //----------------------------------------------------------------------------

  ProductName(UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")),
  SerialNumber(UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")),
  HardwareRevision(UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")),
  FirmwareRevision(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")),
  BrainActivityMeasurement(UUID.fromString("0000b2a5-0000-1000-8000-00805f9b34fb")),
  DeviceBatteryStatus(UUID.fromString("0000b2a2-0000-1000-8000-00805f9b34fb")),
  HeadsetStatus(UUID.fromString("0000b2a3-0000-1000-8000-00805f9b34fb")),
  OadTransfert(UUID.fromString("0000b2a6-0000-1000-8000-00805f9b34fb")),
  MailBox(UUID.fromString("0000b2a4-0000-1000-8000-00805f9b34fb"));

  //----------------------------------------------------------------------------
  // MARK: - Helpers
  //----------------------------------------------------------------------------

  companion object {
    private val rawToEnum = mapOf(
      PreIndus5Characteristic.ProductName.raw to ProductName,
      PreIndus5Characteristic.SerialNumber.raw to SerialNumber,
      PreIndus5Characteristic.HardwareRevision.raw to HardwareRevision,
      PreIndus5Characteristic.FirmwareRevision to FirmwareRevision,
      PreIndus5Characteristic.BrainActivityMeasurement to BrainActivityMeasurement,
      PreIndus5Characteristic.DeviceBatteryStatus to DeviceBatteryStatus,
      PreIndus5Characteristic.HeadsetStatus to HeadsetStatus,
      PreIndus5Characteristic.OadTransfert to OadTransfert,
      PreIndus5Characteristic.MailBox to MailBox,
    )

    fun ofRaw(raw: UUID): PreIndus5Characteristic? {
      return rawToEnum[raw]
    }

    fun readCharacteristics(): Array<PreIndus5Characteristic> {
      return arrayOf(
        ProductName,
        SerialNumber,
        HardwareRevision,
        FirmwareRevision,
        BrainActivityMeasurement,
        DeviceBatteryStatus,
        HeadsetStatus,
        MailBox
      )
    }

    fun writeCharacteristics(): Array<PreIndus5Characteristic> {
      return arrayOf(
        OadTransfert
      )
    }
  }



}