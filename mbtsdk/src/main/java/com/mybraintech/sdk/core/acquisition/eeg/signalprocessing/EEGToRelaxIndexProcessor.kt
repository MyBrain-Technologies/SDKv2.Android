package com.mybraintech.sdk.core.acquisition.eeg.signalprocessing

import com.mybraintech.android.jnibrainbox.RelaxIndex
import com.mybraintech.android.jnibrainbox.RelaxIndexSessionOutputData
import core.eeg.storage.MbtEEGPacket

class EEGToRelaxIndexProcessor(
  private val samplingRate: Int = 250,
  private val smoothedRms: FloatArray = FloatArray(0),
  private val iafMedianLower: Float = 0.0.toFloat(),
  private val iafMedianUpper: Float = 0.0.toFloat(),
  private val relaxIndexEngine: RelaxIndex =
    RelaxIndex(samplingRate, smoothedRms, iafMedianLower, iafMedianUpper)
) {

  fun computeRelaxIndex(packets: Array<MbtEEGPacket>): Float {

    // TODO: Anh Tuan No idea what I'm doing. I don't know where is the
    // modifiedChannelData
    //    val dataArray = packets.flattenModifiedChannelData() // Swift code
    val signal =  packets.map {
      val channelsData = it.channelsData
      channelsData.flatten().toTypedArray()
    }.toTypedArray()

    val lastPacket = packets.last()
    val qualities = lastPacket.qualities

     TODO("Find a way for the input type")
//    return relaxIndexEngine.computeVolume(signal, qualities)
  }

  fun endSessionAndGenerateSessionStatistic(): RelaxIndexSessionOutputData {
    return relaxIndexEngine.endSession()
  }
}