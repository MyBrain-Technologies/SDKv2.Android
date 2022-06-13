package com.mybraintech.sdk.core.acquisition.ims

import com.mybraintech.sdk.core.model.ThreeDimensionalPosition

interface IMSSignalProcessing {
    fun startIMSRecording()
    fun stopIMSRecording()
    fun isIMSRecording(): Boolean
    fun onIMSFrame(data: ByteArray)
    fun getIMSBuffer(): List<ThreeDimensionalPosition>
    fun getIMSBufferSize(): Int
    fun clearIMSBuffer()
}