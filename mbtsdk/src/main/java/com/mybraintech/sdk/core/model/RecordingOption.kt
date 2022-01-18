package com.mybraintech.sdk.core.model

import java.io.File

data class RecordingOption(
    val outputFile: File? = null,
    val context: KwakContext,
    val deviceInformation: DeviceInformation,
    val recordId: String
) {
    var recordingNb: Int = 0
}
