package com.mybraintech.sdk.core.model

class KwakHeader {

    var deviceInfo: DeviceInformation? = null

    /**
     * example: 0x3d
     *
     * Total number of recordings sharing the same recordId : an acquisition session may contain
     * several EEG chunks (or files).
     *
     * This number is represented in hexadecimal on a byte, as the Kwak file is generated on the fly
     * of the acquisition and each Kwak file is updated when a new EEG chunk is added to the acquisition session.
     */
    var recordingNb: String = "0x00"
        private set

    /**
     * Number of measures required to represent an EEG data buffer usable by signal processing algorithms.
     */
    var eegPacketLength = 250

    /**
     * Array of timestamped comments (user-defined) about this EEG recording.
     *
     * example: { "date" : 1500476031054, "comment" : "session" }]
     */
    var comments: List<Comment>? = null

    /**
     * in Hz
     */
    var sampleRate: Int = 250

    var nbChannels: Int = 0

    var acquisitionLocations: List<EnumAcquisitionLocation> = emptyList()

    var groundLocations: List<EnumAcquisitionLocation> = emptyList()

    var referenceLocations: List<EnumAcquisitionLocation> = emptyList()

    fun setRecordingNb(recordingNb: Int) {
        this.recordingNb = "0x" + String.format("%02X", recordingNb)
    }

    fun getQPlusHeader(): KwakHeader {
        return KwakHeader().apply {
            this.nbChannels = 4 //4 channels for Q Plus
            this.acquisitionLocations = listOf(
                EnumAcquisitionLocation.P3,
                EnumAcquisitionLocation.P4,
                EnumAcquisitionLocation.AF3,
                EnumAcquisitionLocation.AF4
            )
            this.groundLocations = listOf(EnumAcquisitionLocation.M2)
            this.referenceLocations = listOf(EnumAcquisitionLocation.M1)
        }
    }

    fun getMelomindHeader(): KwakHeader {
        return KwakHeader().apply {
            this.nbChannels = 2 //2 channels for Melomind
            this.acquisitionLocations = listOf(
                EnumAcquisitionLocation.P3,
                EnumAcquisitionLocation.P4
            )
            this.groundLocations = listOf(EnumAcquisitionLocation.M2)
            this.referenceLocations = listOf(EnumAcquisitionLocation.M1)
        }
    }

    fun getHyperionHeader(): KwakHeader {
        return KwakHeader().apply {
            this.nbChannels = 4 //4 channels for Hyperion
            this.acquisitionLocations = listOf(
                EnumAcquisitionLocation.Cz,
                EnumAcquisitionLocation.Pz,
                EnumAcquisitionLocation.AF3,
                EnumAcquisitionLocation.AF4
            )
            this.groundLocations = listOf(EnumAcquisitionLocation.M2)
            this.referenceLocations = listOf(EnumAcquisitionLocation.M1)
        }
    }

    fun getBaseHeader() : KwakHeader {
        return KwakHeader()
    }
}
