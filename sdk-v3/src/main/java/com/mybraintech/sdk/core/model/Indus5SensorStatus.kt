package com.mybraintech.sdk.core.model

data class Indus5SensorStatus(
    /**
     * sensor status data is received and is decoded successfully.
     */
    val isSuccessful: Boolean,
    val eegInitStatus: InitStatus,
    val imsInitStatus: InitStatus,
    val ppgInitStatus: InitStatus,
    val tempInitStatus: InitStatus,
    val isEEGStarted: StartStatus,
    val isIMSStarted: StartStatus,
    val isPPGStarted: StartStatus,
    val isTEMPStarted: StartStatus,
    val isTriggerStarted: StartStatus
) {
    enum class InitStatus(val code: Byte) {
        INIT_SUCCEEDED(0x01), INIT_FAILED(0x02)
    }

    enum class StartStatus(val code: Byte) {
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
        fun parse(bytes: ByteArray): Indus5SensorStatus {
            if (bytes.size < 10) {
                return Indus5SensorStatus(
                    false,
                    InitStatus.INIT_FAILED,
                    InitStatus.INIT_FAILED,
                    InitStatus.INIT_FAILED,
                    InitStatus.INIT_FAILED,
                    StartStatus.NOT_STARTED,
                    StartStatus.NOT_STARTED,
                    StartStatus.NOT_STARTED,
                    StartStatus.NOT_STARTED,
                    StartStatus.NOT_STARTED
                )
            } else {
                return Indus5SensorStatus(
                    isSuccessful = true,
                    eegInitStatus = parseInitStatus(bytes[1]),
                    imsInitStatus = parseInitStatus(bytes[2]),
                    ppgInitStatus = parseInitStatus(bytes[3]),
                    tempInitStatus = parseInitStatus(bytes[4]),
                    isEEGStarted = parseStartStatus(bytes[5]),
                    isIMSStarted = parseStartStatus(bytes[6]),
                    isPPGStarted = parseStartStatus(bytes[7]),
                    isTEMPStarted = parseStartStatus(bytes[8]),
                    isTriggerStarted = parseStartStatus(bytes[9])
                )
            }
        }

        private fun parseInitStatus(byte: Byte): InitStatus {
            return if (byte == InitStatus.INIT_SUCCEEDED.code) {
                InitStatus.INIT_SUCCEEDED
            } else {
                InitStatus.INIT_FAILED
            }
        }

        private fun parseStartStatus(byte: Byte): StartStatus {
            return if (byte == StartStatus.STARTED.code) {
                StartStatus.STARTED
            } else {
                StartStatus.NOT_STARTED
            }
        }
    }
}