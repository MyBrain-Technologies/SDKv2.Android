package com.mybraintech.sdk.core.model

import timber.log.Timber


class EEGRecordingData(
    hasStatus: Boolean,
    nbChannels: Int,
    eegPackets: List<MbtEEGPacket>,
    recordingErrorData: EEGStreamingErrorCounter
) {
    var nbPackets: Int = -1
    lateinit var eegData: ArrayList<ArrayList<Float>>
    lateinit var statusData: ArrayList<Float>
    lateinit var qualities: ArrayList<ArrayList<Float>>
    var recordingErrorData: EEGStreamingErrorCounter? = recordingErrorData

    init {
        init(nbChannels)
        nbPackets = eegPackets.size
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
        Timber.d("nbChannels = $nbChannels")
        eegData = arrayListOf()
        statusData = arrayListOf()
        qualities = arrayListOf()
        for (i in 1..nbChannels) {
            eegData.add(arrayListOf())
            qualities.add(arrayListOf())
        }
    }
}