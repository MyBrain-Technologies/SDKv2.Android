package com.mybraintech.sdk.core.model

class DeviceSystemStatus {

    var processorStatus: EnumState = EnumState.NO_INFORMATIONS
    var externalMemoryStatus: EnumState = EnumState.NO_INFORMATIONS
    var audioStatus: EnumState = EnumState.NO_INFORMATIONS

    /**
     * EEG Acquisition
     */
    var adsStatus: EnumState = EnumState.NO_INFORMATIONS

    enum class EnumState(val code: Byte) {
        NO_INFORMATIONS(0x00),
        STATUS_OK(0x01),
        STATUS_ERROR(0x02)
    }

    companion object {
        fun parse(byte: Byte): EnumState {
            return when (byte) {
                EnumState.NO_INFORMATIONS.code -> EnumState.NO_INFORMATIONS
                EnumState.STATUS_OK.code -> EnumState.STATUS_OK
                else -> {
                    EnumState.STATUS_ERROR
                }
            }
        }
    }
}