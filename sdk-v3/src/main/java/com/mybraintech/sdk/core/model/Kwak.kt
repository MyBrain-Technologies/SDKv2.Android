package com.mybraintech.sdk.core.model

import java.io.FileWriter
import java.util.*

class Kwak {
    /**
     * unique id
     */
    val uuidJsonFile: String = UUID.randomUUID().toString()
    var context: KwakContext = KwakContext()
    var header: KwakHeader = KwakHeader()
    var recording: KwakRecording = KwakRecording()

    fun serializeJson(
        hasStatus: Boolean,
        eegBuffer: List<MbtEEGPacket>,
        eegErrorData: RecordingErrorData2,
        imsBuffer: List<ThreeDimensionalPosition>,
        fileWriter: FileWriter
    ): Boolean {
        return MbtJsonBuilder2.serializeRecording(
            context.ownerId,
            header,
            recording,
            EEGRecordingData(hasStatus, header.nbChannels, eegBuffer, eegErrorData),
            imsBuffer,
            fileWriter
        )
    }
}