package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.ImsPacket

interface AccelerometerListener {
    fun onIMSStatusChange(isEnabled: Boolean)
    fun onAccelerometerPacket(imsPacket: ImsPacket)
    fun onAccelerometerError(error: Throwable)
}