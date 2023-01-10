package com.mybraintech.sdk.core.model

@Suppress("unused")
class AccelerometerConfig private constructor(
    val sampleRate: EnumAccelerometerSampleRate,
    val axisSettingCode: Byte,
    val fullScaleCode: Byte
) {

    companion object {

        private val DEFAULT_SAMPLE_RATE = EnumAccelerometerSampleRate.F_100_HZ
        private const val DEFAULT_AXIS_SETTING_CODE: Byte = 0x07 // All axis are enabled
        private const val DEFAULT_FULL_SCALE_CODE: Byte = 0x00 // ±2g

        /**
         * starts with `MBX_GET_IMS_CONFIG [0x3A]` or `MBX_SET_IMS_CONFIG [0x39]`, the array length should be 3 or 4
         */
        fun parse(bytes: ByteArray): AccelerometerConfig {
            var sampRate = DEFAULT_SAMPLE_RATE
            var axis = DEFAULT_AXIS_SETTING_CODE
            var scale = DEFAULT_FULL_SCALE_CODE
            if (bytes.size >= 2) {
                sampRate = EnumAccelerometerSampleRate.parse(bytes[1])
                if (bytes.size >= 3) {
                    axis = bytes[2]
                    if (bytes.size >= 4) {
                        scale = bytes[3]
                    }
                }
            }
            return AccelerometerConfig(sampRate, axis, scale)
        }
    }
}