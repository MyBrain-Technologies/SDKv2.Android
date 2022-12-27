package com.mybraintech.sdk.core.model

@Suppress("unused")
enum class EnumAccelerometerSampleRate(val value: Int, val mailboxValue: Byte) {
    F_50_HZ(50, 0x04), F_100_HZ(100, 0x05);

    companion object {
        fun parse(byte: Byte): EnumAccelerometerSampleRate {
            for (sampleRate in values()) {
                if (byte == sampleRate.mailboxValue) {
                    return sampleRate
                }
            }
            return F_100_HZ
        }
    }
}