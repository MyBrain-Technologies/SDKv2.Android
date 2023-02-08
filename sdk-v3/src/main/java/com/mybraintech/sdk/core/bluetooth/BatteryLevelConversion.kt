package com.mybraintech.sdk.core.bluetooth

class BatteryLevelConversion {

    fun parseForIndus5(value: Byte): Float {
        return when {
            value in 0x00..0x04 -> 0f
            value == 0x05.toByte() -> 15f
            value == 0x06.toByte() -> 30f
            value == 0x07.toByte() -> 45f
            value == 0x08.toByte() -> 60f
            value == 0x09.toByte() -> 75f
            value == 0x0A.toByte() -> 90f
            value >= 0x0B -> 100f
            else -> -1f
        }
    }

    fun parseForMelomind(value: Byte): Float {
        val level: Float = when (value) {
            0x00.toByte() -> 0f
            0x01.toByte() -> 15f
            0x02.toByte() -> 30f
            0x03.toByte() -> 50f
            0x04.toByte() -> 65f
            0x05.toByte() -> 85f
            0x06.toByte() -> 100f
            0xFF.toByte() -> -1f
            else -> -1f
        }
        return level
    }

}