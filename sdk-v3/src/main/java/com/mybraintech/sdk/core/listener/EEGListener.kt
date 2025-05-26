package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.MbtEEGPacket

/**
 * This listener call with frequency 1s.
 */
interface EEGListener {
    fun onEEGStatusChange(isEnabled: Boolean)
    fun onEegPacket(mbtEEGPacket: MbtEEGPacket)
    fun onEegError(error: Throwable)
}