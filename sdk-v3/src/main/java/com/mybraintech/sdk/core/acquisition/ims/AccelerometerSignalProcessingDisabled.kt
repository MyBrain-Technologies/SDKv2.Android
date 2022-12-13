package com.mybraintech.sdk.core.acquisition.ims

import com.mybraintech.sdk.core.model.ThreeDimensionalPosition

class AccelerometerSignalProcessingDisabled : AccelerometerSignalProcessing {
    override fun clearBuffer() {}
    override fun stopRecording() {}
    override fun isRecording() = false
    override fun onFrame(data: ByteArray) {}
    override fun getBuffer(): List<ThreeDimensionalPosition> = emptyList()
    override fun getBufferSize(): Int = -1
    override fun startRecording() {}
}