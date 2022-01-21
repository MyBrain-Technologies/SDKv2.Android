package com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.services

import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.IMBTAttribute
import java.util.*


enum class PreIndus5Service(val raw: String): IMBTAttribute {

  //--------------------------------------------------------------------------
  // MARK: - Cases
  //--------------------------------------------------------------------------

  DeviceInformation("0x180A"),
  MyBrain("0xB2A0");

  //--------------------------------------------------------------------------
  // MARK: - MBTAttributeProtocol
  //--------------------------------------------------------------------------


  // TODO: To check
  override val uuid: UUID
    get() {
      val uuidBytes = this.raw.toByteArray()
      return  UUID.nameUUIDFromBytes(uuidBytes)
  }

}