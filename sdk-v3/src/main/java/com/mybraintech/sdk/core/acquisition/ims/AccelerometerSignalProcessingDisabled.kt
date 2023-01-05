package com.mybraintech.sdk.core.acquisition.ims

import com.mybraintech.sdk.core.model.AccelerometerConfig
import com.mybraintech.sdk.core.model.ThreeDimensionalPosition
import com.mybraintech.sdk.core.recording.BaseAccelerometerRecorder
import timber.log.Timber

class AccelerometerSignalProcessingDisabled : BaseAccelerometerRecorder() {
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

    override fun getBuffer(): List<ThreeDimensionalPosition> = emptyList()
    override fun getBufferSize(): Int = -1
    override fun startRecording() {
        doNothing()
    }

    override fun onAccelerometerConfiguration(accelerometerConfig: AccelerometerConfig) {
        doNothing()
    }

    private fun doNothing() {
        Timber.i("doNothing")
    }
}