package com.mybraintech.sdk.core.model

import java.io.FileWriter
import java.util.*

class Kwak {
    val uuidJsonFile: String = UUID.randomUUID().toString()
    var context: KwakContext = KwakContext()
    var header: KwakHeader = KwakHeader()
    var recording: KwakRecording = KwakRecording()

    fun serializeJson(
        hasStatus: Boolean,
        recordingBuffer: List<MbtEEGPacket2>,
        recordingErrorData: RecordingErrorData2,
        fileWriter: FileWriter
    ): Boolean {
        return MbtJsonBuilder2.serializeRecording(
            context.ownerId,
            header,
            recording,
            RecordingData2(hasStatus, header.nbChannels, recordingBuffer, recordingErrorData),
            fileWriter
        )
    }
}