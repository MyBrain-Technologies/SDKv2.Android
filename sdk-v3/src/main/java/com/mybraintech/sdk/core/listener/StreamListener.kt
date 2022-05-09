package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.ImsPacket
import com.mybraintech.sdk.core.model.MbtEEGPacket2


interface StreamListener {
    fun onEegPacket(mbtEEGPacket2: MbtEEGPacket2)
    fun onEegError(error: Throwable)
    fun onAccelerometerPacket(imsPacket: ImsPacket)
    fun onAccelerometerError(error: Throwable)
}