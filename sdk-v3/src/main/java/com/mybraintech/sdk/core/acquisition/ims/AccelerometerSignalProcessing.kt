package com.mybraintech.sdk.core.acquisition.ims

import com.mybraintech.sdk.core.model.ThreeDimensionalPosition

interface AccelerometerSignalProcessing {
    fun startRecording()
    fun stopRecording()
    fun isRecording(): Boolean
    fun onFrame(data: ByteArray)
    fun getBuffer(): List<ThreeDimensionalPosition>
    fun getBufferSize(): Int
    fun clearBuffer()
}