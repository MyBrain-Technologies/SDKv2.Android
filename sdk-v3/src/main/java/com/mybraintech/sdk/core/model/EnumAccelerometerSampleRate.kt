package com.mybraintech.sdk.core.model

@Suppress("unused")
enum class EnumAccelerometerSampleRate(val level: Int, val mailboxValue: Byte) {
    F_50_HZ(50, 0x04), F_100_HZ(100, 0x05)
}