package com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.characteristics

import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.IMBTAttribute
import com.mybraintech.sdk.core.bluetooth.deviceinformation.IndusVersion
import java.util.*

enum class PostIndus5Characteristic(val raw: UUID) {

  Rx(UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616")),
  Tx(UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3")),
  Unknown(UUID.fromString("49535343-4C8A-39B3-2F49-511CFF073B7E"));


  companion object {
    private val rawToEnum = mapOf(
      Rx.raw to Rx,
      Tx.raw to Tx,
      Unknown.raw to Unknown
    )

    fun ofRaw(raw: UUID): PostIndus5Characteristic? {
      return rawToEnum[raw]
    }
  }

}