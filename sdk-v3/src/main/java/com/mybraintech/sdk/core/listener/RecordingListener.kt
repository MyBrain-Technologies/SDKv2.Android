package com.mybraintech.sdk.core.listener

import java.io.File

interface RecordingListener {
    fun onRecordingSaved(outputFile: File)
    fun onRecordingError(error: Throwable)
}