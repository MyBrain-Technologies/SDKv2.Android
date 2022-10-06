package com.mybraintech.sdk.core.model

class EEGSignalPack(val timestamp: Long, val index: Long, val eegSignals: List<List<Float>>, val triggers: List<Float>)