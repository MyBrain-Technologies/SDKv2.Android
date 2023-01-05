package com.mybraintech.sdk.core.acquisition.ppg

import com.mybraintech.sdk.core.model.PPGPacket
import com.mybraintech.sdk.core.recording.BasePPGRecording
import timber.log.Timber

class PPGSignalProcessingDisabled : BasePPGRecording() {

    override fun clearBuffer() {
        doNothing()
    }

    override fun stopRecording() {
        doNothing()
    }

    override fun isRecording() = false
    override fun addSignalData(data: ByteArray) {
        doNothing()
    }

    override fun getBuffer(): List<PPGPacket> = emptyList()
    override fun getBufferSize(): Int = -1
    override fun startRecording() {
        doNothing()
    }

    override fun dispose() {
        doNothing()
    }

    private fun doNothing() {
        Timber.i("doNothing")
    }
}