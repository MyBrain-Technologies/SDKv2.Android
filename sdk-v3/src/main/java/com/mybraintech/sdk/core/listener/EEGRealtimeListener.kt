package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.LabStreamingLayer
import com.mybraintech.sdk.core.model.EEGSignalPack

@LabStreamingLayer
interface EEGRealtimeListener {
    fun onEEGFrame(pack: EEGSignalPack)
}