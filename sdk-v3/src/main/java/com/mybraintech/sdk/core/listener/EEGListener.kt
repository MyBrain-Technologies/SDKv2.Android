package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.MbtEEGPacket2

interface EEGListener {
    fun onEEGStatusChange(isEnabled: Boolean)
    fun onEegPacket(mbtEEGPacket2: MbtEEGPacket2)
    fun onEegError(error: Throwable)
}