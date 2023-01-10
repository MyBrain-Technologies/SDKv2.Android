package com.mybraintech.sdk.core.model

@Suppress("unused")
class EEGSignalPack(
    val timestamp: Long,
    val index: Long,
    val eegSignals: List<List<Float>>,
    val triggers: List<Float>
)