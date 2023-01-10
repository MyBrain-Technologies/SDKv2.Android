package com.mybraintech.sdk.core.acquisition

import com.mybraintech.sdk.core.listener.RecordingListener
import com.mybraintech.sdk.core.model.RecordingOption

interface RecordingInterface {
    fun startRecording(recordingListener: RecordingListener, recordingOption: RecordingOption)
    fun stopRecording()
    fun stopRecording(trim: Int)
    fun isRecordingEnabled(): Boolean
    fun clearBuffer()
    fun getRecordingBufferSize(): Int
    fun getDataLossPercentage(): Float
}