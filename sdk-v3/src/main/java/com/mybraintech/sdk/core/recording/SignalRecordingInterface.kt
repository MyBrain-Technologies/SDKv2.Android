package com.mybraintech.sdk.core.recording

import com.mybraintech.sdk.core.acquisition.EnumSignalType

interface SignalRecordingInterface<T,R> {
    fun getSignalType() : EnumSignalType
    fun startRecording()
    fun stopRecording()
    fun isRecording(): Boolean
    fun onFrame(data: T)
    fun getBuffer(): List<R>
    fun getBufferSize(): Int
    fun clearBuffer()
}