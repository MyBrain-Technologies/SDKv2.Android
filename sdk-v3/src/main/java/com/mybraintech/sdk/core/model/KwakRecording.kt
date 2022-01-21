package com.mybraintech.sdk.core.model

class KwakRecording {

    /**
     * Unique identifier of an acquisition session.
     * An acquisition session can be represented as a group of EEG chunks (or files); the size of
     * this group is stored in recordingNb attribute.
     */
    var recordID: String = ""

    var recordingType : KwakRecordingType = KwakRecordingType()

    /**
     * start recording timestamp (GMT)
     */
    var recordingTime: Long = System.currentTimeMillis()

//    var recordingParameters: EEGKwakRecordingParams? = null
}