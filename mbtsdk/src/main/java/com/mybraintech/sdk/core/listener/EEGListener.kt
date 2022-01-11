package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.acquisition.eeg.MbtEEGPacket2

interface EEGListener {
    fun onEegPacket(mbtEEGPacket2: MbtEEGPacket2)
    fun onEegError(error: Throwable)
}