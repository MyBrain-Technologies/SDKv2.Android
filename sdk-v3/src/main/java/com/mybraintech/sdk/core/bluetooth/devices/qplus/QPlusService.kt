package com.mybraintech.sdk.core.bluetooth.devices.qplus

import java.util.*

enum class QPlusService(val raw: String) {

  Transparent("49535343-FE7D-4AE5-8FA9-9FAFD205E455");

  val uuid: UUID
    get() {
      return UUID.fromString(this.raw)
    }
}