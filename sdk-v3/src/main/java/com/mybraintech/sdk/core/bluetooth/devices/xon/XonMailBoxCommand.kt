package com.mybraintech.sdk.core.bluetooth.devices.xon

enum class XonMailBoxCommand(val bytes: ByteArray) {
    //TODO: need update here
    TRIGGER_STATUS(byteArrayOf(0x0F))
}