package com.mybraintech.sdk.core.model

data class Indus5SensorStatus(
    val isOk : Boolean,
    val eegInitStatus: InitStatus,
    val imsInitStatus: InitStatus,
    val ppgInitStatus: InitStatus,
    val tempInitStatus: InitStatus,
    val isEEGStarted : StartStatus,
    val isIMSStarted: StartStatus,
    val isPPGStarted: StartStatus,
    val isTEMPStarted: StartStatus
) {
    enum class InitStatus(val code : Byte) {
        INIT_SUCCEEDED(0x01), INIT_FAILED(0x02)
    }

    enum class StartStatus(val code : Byte) {
        STARTED(0x01), NOT_STARTED(0x02)
    }

    companion object {
        /**
         * parse a byte array which contains indus5 sensor status response.
         *
         * Operation code = 0x42. Byte array length must be bigger or equal 10.
         *
         * eg : 0x42 0x01 0x01 0x02 0x02 0x01 0x02 0x02 0x02 0x02
         */
        fun parse(bytes: ByteArray) : Indus5SensorStatus? {
            return null
//            if (bytes.size < 10) {
//                return Indus5SensorStatus(
//                    isOk = false,
//                    imsInitStatus = InitStatus.INIT_FAILED
//                )
//            }
        }
    }
}