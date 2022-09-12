package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.EEGSignalPack

interface EEGRealtimeListener {
    fun onEEGFrame(pack: EEGSignalPack)
}