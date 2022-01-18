package com.mybraintech.sdk.core.model

import com.mybraintech.sdk.core.acquisition.eeg.MbtEEGPacket2

class RecordingData2 {
    var nbPackets: Int = -1
    lateinit var eegData: ArrayList<ArrayList<Float>>
    lateinit var statusData: ArrayList<Float>
    lateinit var qualities: ArrayList<ArrayList<Float>>
    var recordingErrorData: RecordingErrorData2? = null

    constructor(
        hasStatus: Boolean,
        nbChannels: Int,
        eegPackets: List<MbtEEGPacket2>,
        recordingErrorData: RecordingErrorData2
    ) {
        init(nbChannels)
        nbPackets = eegPackets.size
        this.recordingErrorData = recordingErrorData

        for (eegPacket in eegPackets) {

            //RAW EEG
            for (ch in 0 until nbChannels) {
                eegData[ch].addAll(eegPacket.channelsData[ch])
            }
//            Timber.d("recordingData.eegData size = ${eegData.size} * ${eegData[0].size}")

            //QUALITIES
            if (eegPacket.qualities != null) {
                for (ch in 0 until nbChannels) {
                    qualities[ch].add(eegPacket.qualities[ch])
                }
            }

            //STATUS
            if (eegPacket.statusData != null && hasStatus) {
                statusData.addAll(eegPacket.statusData)
            }
        }
    }

    private fun init(nbChannels: Int) {
        eegData = arrayListOf()
        statusData = arrayListOf()
        qualities = arrayListOf()
        for (i in 1..nbChannels) {
            eegData.add(arrayListOf())
            qualities.add(arrayListOf())
        }
    }
}