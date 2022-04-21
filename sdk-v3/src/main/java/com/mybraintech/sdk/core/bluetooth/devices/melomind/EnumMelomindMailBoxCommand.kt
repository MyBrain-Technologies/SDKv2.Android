package com.mybraintech.sdk.core.bluetooth.devices.melomind

enum class EnumMelomindMailBoxCommand(val bytes: ByteArray) {
    TRIGGER_STATUS(byteArrayOf(0x0F))
}