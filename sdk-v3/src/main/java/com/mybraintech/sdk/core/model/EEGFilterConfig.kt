package com.mybraintech.sdk.core.model

import timber.log.Timber

enum class EnumEEGFilterConfig(val byteCode: Byte) {
    NO_FILTER(0x00),
    BANDSTOP(0x01),
    BANDPASS_BANDSTOP(0x02),
    DEFAULT(BANDPASS_BANDSTOP.byteCode),
    ;

    companion object {
        fun parse(byte: Byte): EnumEEGFilterConfig {
            return when (byte) {
                NO_FILTER.byteCode -> NO_FILTER
                BANDSTOP.byteCode -> BANDSTOP
                BANDPASS_BANDSTOP.byteCode -> BANDPASS_BANDSTOP
                else -> {
                    Timber.e("return DEFAULT since the value is not recognized : value = $byte")
                    DEFAULT
                }
            }
        }
    }
}
