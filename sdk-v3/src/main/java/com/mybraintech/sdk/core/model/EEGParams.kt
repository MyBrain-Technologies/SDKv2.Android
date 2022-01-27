package com.mybraintech.sdk.core.model

data class EEGParams(
    val sampleRate: Int,
    val isTriggerStatusEnabled: Boolean,
    val isQualityCheckerEnabled: Boolean
)
