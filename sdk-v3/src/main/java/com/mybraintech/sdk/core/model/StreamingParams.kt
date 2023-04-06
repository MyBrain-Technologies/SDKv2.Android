package com.mybraintech.sdk.core.model

import com.mybraintech.sdk.core.ResearchStudy

class StreamingParams private constructor(
    val isEEGEnabled: Boolean,

    /**
     * this option will be ignored if EEG is disabled
     */
    val isTriggerStatusEnabled: Boolean,

    /**
     * this option will be ignored if EEG is disabled
     */
    val isQualityCheckerEnabled: Boolean,

    /**
     * this option is only available for QPlus device, it will be ignored for Melomind device
     */
    val isAccelerometerEnabled: Boolean,

    var accelerometerSampleRate: EnumAccelerometerSampleRate,

    val filterConfig: EnumEEGFilterConfig,
) {
    val eegSampleRate: Int = 250

    @Suppress("unused")
    class Builder() {
        private var isEEGEnabled: Boolean = true

        private var isTriggerStatusEnabled: Boolean = false

        private var isQualityCheckerEnabled: Boolean = true

        private var isAccelerometerEnabled: Boolean = false

        private var accelerometerSampleRate = EnumAccelerometerSampleRate.F_100_HZ

        private var filterConfig: EnumEEGFilterConfig = EnumEEGFilterConfig.DEFAULT

        fun setEEG(isEnabled: Boolean): Builder {
            this.isEEGEnabled = isEnabled
            return this
        }

        fun setTriggerStatus(isEnabled: Boolean): Builder {
            this.isTriggerStatusEnabled = isEnabled
            return this
        }

        fun setQualityChecker(isEnabled: Boolean): Builder {
            this.isQualityCheckerEnabled = isEnabled
            return this
        }

        fun setAccelerometer(isEnabled: Boolean): Builder {
            this.isAccelerometerEnabled = isEnabled
            return this
        }

        fun setAccelerometerSampleRate(sampleRate: EnumAccelerometerSampleRate): Builder {
            this.accelerometerSampleRate = sampleRate
            return this
        }

        @ResearchStudy
        fun setEEGFilterConfig(config: EnumEEGFilterConfig): Builder {
            this.filterConfig = config
            return this
        }

        fun build(): StreamingParams {
            return if (isEEGEnabled) {
                StreamingParams(
                    isEEGEnabled = true,
                    isTriggerStatusEnabled = isTriggerStatusEnabled,
                    isQualityCheckerEnabled = isQualityCheckerEnabled,
                    isAccelerometerEnabled = isAccelerometerEnabled,
                    accelerometerSampleRate = accelerometerSampleRate,
                    filterConfig = filterConfig,
                )
            } else {
                StreamingParams(
                    isEEGEnabled = false,
                    isTriggerStatusEnabled = false,
                    isQualityCheckerEnabled = false,
                    isAccelerometerEnabled = true,
                    accelerometerSampleRate = accelerometerSampleRate,
                    filterConfig = filterConfig,
                )
            }
        }
    }
}
