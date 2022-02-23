package com.mybraintech.sdk.core.model

data class EEGParams(
    val isTriggerStatusEnabled: Boolean,
    val isQualityCheckerEnabled: Boolean
) {
    val sampleRate: Int = 250
}
