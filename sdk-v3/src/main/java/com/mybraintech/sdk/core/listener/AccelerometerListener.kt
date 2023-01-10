package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.AccelerometerPacket

interface AccelerometerListener {
    fun onAccelerometerStatusChange(isEnabled: Boolean)
    fun onAccelerometerPacket(accelerometerPacket: AccelerometerPacket)
    fun onAccelerometerError(error: Throwable)
}