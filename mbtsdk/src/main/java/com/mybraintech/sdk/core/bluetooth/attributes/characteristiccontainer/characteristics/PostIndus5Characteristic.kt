package com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.characteristics

import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.IMBTAttribute
import java.util.*

enum class PostIndus5Characteristic(val raw: String): IMBTAttribute {

  //--------------------------------------------------------------------------
  // MARK: - Cases
  //--------------------------------------------------------------------------

  Rx("49535343-1E4D-4BD9-BA61-23C647249616"),
  Tx("49535343-8841-43F4-A8D4-ECBE34729BB3"),
  Unknown("49535343-4C8A-39B3-2F49-511CFF073B7E");


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