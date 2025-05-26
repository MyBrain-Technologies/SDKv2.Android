package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.EEGSignalPack

/**
 * The listener base on realtime. This will trigger when eeg signal received.
 */
interface EEGRealtimeListener {
    fun onEEGFrame(pack: EEGSignalPack)
}