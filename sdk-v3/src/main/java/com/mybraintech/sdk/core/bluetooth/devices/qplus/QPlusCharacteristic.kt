package com.mybraintech.sdk.core.bluetooth.devices.qplus

import java.util.*

enum class QPlusCharacteristic(val uuid: UUID) {

  Rx(UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616")),
  Tx(UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3")),
  Unknown(UUID.fromString("49535343-4C8A-39B3-2F49-511CFF073B7E"));


  companion object {
    private val rawToEnum = mapOf(
      Rx.uuid to Rx,
      Tx.uuid to Tx,
      Unknown.uuid to Unknown
    )

    fun ofRaw(raw: UUID): QPlusCharacteristic? {
      return rawToEnum[raw]
    }
  }

}