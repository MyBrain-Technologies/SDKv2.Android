package com.mybraintech.sdk.core.model

import java.io.FileWriter
import java.util.*

class Kwak {
    /**
     * unique id
     */
    @Suppress("unused")
    val uuidJsonFile: String = UUID.randomUUID().toString()
    var context: KwakContext = KwakContext()
    var header: KwakHeader = KwakHeader()
    var recording: KwakRecording = KwakRecording()

    fun serializeJson(
        streamingParams: StreamingParams,
        eegBuffer: List<MbtEEGPacket>,
        eegStreamingErrorCounter: EEGStreamingErrorCounter,
        imsBuffer: List<ThreeDimensionalPosition>,
        fileWriter: FileWriter
    ): Boolean {
        return MbtJsonBuilder2.serializeRecording(
            context.ownerId,
            header,
            recording,
            EEGRecordingData(streamingParams.isTriggerStatusEnabled, header.nbChannels, eegBuffer, eegStreamingErrorCounter),
            AccelerometerRecordingData(streamingParams.accelerometerSampleRate.sampleRate, imsBuffer),
            fileWriter
        )
    }
}