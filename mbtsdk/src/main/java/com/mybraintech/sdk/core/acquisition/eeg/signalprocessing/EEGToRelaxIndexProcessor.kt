package com.mybraintech.sdk.core.acquisition.eeg.signalprocessing

import core.eeg.storage.MbtEEGPacket

object EEGToRelaxIndexProcessor {
  fun computeRelaxIndex(packets: Array<MbtEEGPacket>,
                        sampleRate: Int,
                        channelCount: Int): Float {



    // TODO: Anh Tuan No idea what I'm doing. I don't know where is the
    // modifiedChannelData
    //    val dataArray = packets.flattenModifiedChannelData() // Swift code
    val dataArray =  packets.map {
      val channelsData = it.channelsData
      channelsData.flatten()
    }.flatten()



    val lastPacket = packets.last()
    val qualities = lastPacket.qualities

    // TODO: Anh Tuan use BrainBox here
    return 0
//    val relaxIndex =
//    MBTRelaxIndexBridge.computeRelaxIndex(dataArray,
//                                          sampRate: sampRate,
//                                          nbChannels: channelCount,
//                                          lastPacketQualities: qualities)
//    return relaxIndex
  }
}