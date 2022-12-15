package com.mybraintech.sdk.core.model

import com.mybraintech.sdk.core.LabStreamingLayer

@LabStreamingLayer
class EEGSignalPack(val timestamp: Long, val index: Long, val eegSignals: List<List<Float>>, val triggers: List<Float>)