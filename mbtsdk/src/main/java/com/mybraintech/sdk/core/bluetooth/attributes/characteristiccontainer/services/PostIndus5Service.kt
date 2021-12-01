package com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.services

import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.IMBTAttribute
import java.util.*

enum class PostIndus5Service(val raw: String): IMBTAttribute {

  //--------------------------------------------------------------------------
  // MARK: - Cases
  //--------------------------------------------------------------------------

  Transparent("49535343-FE7D-4AE5-8FA9-9FAFD205E455");

  //--------------------------------------------------------------------------
  // MARK: - MBTAttributeProtocol
  //--------------------------------------------------------------------------


  // TODO: To check
  override val uuid: UUID
    get() {
      return UUID.fromString(this.raw)
    }
}