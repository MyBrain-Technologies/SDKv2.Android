package com.mybraintech.sdk.core.acquisition.ims

import com.mybraintech.sdk.core.model.ThreeDimensionalPosition
import com.mybraintech.sdk.core.recording.BaseAccelerometerRecording

class AccelerometerSignalProcessingDisabled : BaseAccelerometerRecording() {
    override fun clearBuffer() {}
    override fun stopRecording() {}
    override fun isRecording() = false
    override fun onSignalData(data: ByteArray) {}
    override fun getBuffer(): List<ThreeDimensionalPosition> = emptyList()
    override fun getBufferSize(): Int = -1
    override fun startRecording() {}
}