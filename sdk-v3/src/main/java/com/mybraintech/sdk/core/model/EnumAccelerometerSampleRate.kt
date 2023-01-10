package com.mybraintech.sdk.core.model

@Suppress("unused")
enum class EnumAccelerometerSampleRate(val sampleRate: Int, val mailboxValue: Byte) {
    F_50_HZ(50, 0x04), F_100_HZ(100, 0x05);

    companion object {
        /**
         * @param byte firmware mailbox code byte
         */
        fun parse(byte: Byte): EnumAccelerometerSampleRate {
            for (entity in values()) {
                if (byte == entity.mailboxValue) {
                    return entity
                }
            }
            return F_100_HZ
        }

        fun parse(sampleRate: Int): EnumAccelerometerSampleRate {
            for (entity in values()) {
                if (sampleRate == entity.sampleRate) {
                    return entity
                }
            }
            return F_100_HZ
        }
    }
}