package com.mybraintech.sdk.core.acquisition.ims

import com.mybraintech.sdk.core.model.ThreeDimensionalPosition

class IMSSignalProcessingDisabled : IMSSignalProcessing {
    override fun clearIMSBuffer() {}
    override fun stopIMSRecording() {}
    override fun isIMSRecording() = false
    override fun onIMSFrame(data: ByteArray) {}
    override fun getIMSBuffer(): List<ThreeDimensionalPosition> = emptyList()
    override fun getIMSBufferSize(): Int = -1
    override fun startIMSRecording() {}
}