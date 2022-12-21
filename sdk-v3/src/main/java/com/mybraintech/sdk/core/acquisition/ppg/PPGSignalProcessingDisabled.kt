package com.mybraintech.sdk.core.acquisition.ppg

import com.mybraintech.sdk.core.model.PPGPacket
import com.mybraintech.sdk.core.recording.BasePPGRecording

class PPGSignalProcessingDisabled : BasePPGRecording() {
    override fun clearBuffer() {}
    override fun stopRecording() {}
    override fun isRecording() = false
    override fun onFrame(data: ByteArray) {}
    override fun getBuffer(): List<PPGPacket> = emptyList()
    override fun getBufferSize(): Int = -1
    override fun startRecording() {}
}