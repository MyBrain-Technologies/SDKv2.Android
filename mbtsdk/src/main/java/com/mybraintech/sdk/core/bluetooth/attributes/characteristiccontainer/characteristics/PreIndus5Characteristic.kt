package com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.characteristics

import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.IMBTAttribute
import java.util.*

enum class PreIndus5Characteristic(val raw: String): IMBTAttribute {

  //--------------------------------------------------------------------------
  // MARK: - Cases
  //--------------------------------------------------------------------------

  ProductName("0x2A24"),
  SerialNumber("0x2A25"),
  HardwareRevision("0x2A27"),
  FirmwareRevision("0x2A26"),
  BrainActivityMeasurement("0xB2A5"),
  DeviceBatteryStatus("0xB2A2"),
  HeadsetStatus("0xB2A3"),
  OadTransfert("0xB2A6"),
  MailBox("0xB2A4");

  companion object {
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


  //--------------------------------------------------------------------------
  // MARK: - MBTAttributeProtocol
  //--------------------------------------------------------------------------

  // TODO: Anh Tuan To check
  override val uuid: UUID
    get() {
      val uuidBytes = this.raw.toByteArray()
      return UUID.nameUUIDFromBytes(uuidBytes)
    }

}