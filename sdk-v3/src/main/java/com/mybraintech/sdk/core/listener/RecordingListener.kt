package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.acquisition.EEGRecordedDatas
import java.io.File

interface RecordingListener {
    fun onRecordingSaved(outputFile: File, eegRecordedData:EEGRecordedDatas)
    fun onRecordingError(error: Throwable)
}