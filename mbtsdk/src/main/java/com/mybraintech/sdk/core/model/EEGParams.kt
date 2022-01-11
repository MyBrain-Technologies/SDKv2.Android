package com.mybraintech.sdk.core.model

data class EEGParams(
    val sampleRate: Int,
    val isStatusEnabled: Boolean,
    val isQualityCheckerEnabled: Boolean
)
