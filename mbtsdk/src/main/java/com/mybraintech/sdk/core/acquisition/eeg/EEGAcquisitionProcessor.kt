package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.sdk.core.acquisition.eeg.packet.EEGAcquisitionBuffer
import com.mybraintech.sdk.core.acquisition.eeg.packet.EEGDeserializer
import core.eeg.storage.MbtEEGPacket
import features.MbtAcquisitionLocations

class EEGAcquisitionProcessor(
  private val acquisitionBuffer: EEGAcquisitionBuffer,
  private val eegPacketLength: Int,
  private val channelCount: Int,
  private val sampleRate: Int,
  private val electrodeToChannelIndex: Map<MbtAcquisitionLocations, Int>,
  private val signalProcessor: SignalProcessingManager
) {

  fun getEEGPacket(data: ByteArray, hasQualityChecker: Boolean): MbtEEGPacket? {
    acquisitionBuffer.add(data)
    val packet = acquisitionBuffer.getUsablePackets() ?: return null

    val relaxIndexes =
    EEGDeserializer.deserializeToRelaxIndex(packet, channelCount)

    val eegPacket = convertToEEGPacket(relaxIndexes, hasQualityChecker)
    return eegPacket
  }

  /// Convert values from the acquisition to EEG Packets
  private fun convertToEEGPacket(values: Array<Array<Float>>,
                                 hasQualityChecker: Boolean): MbtEEGPacket? {
    // TODO: Anh Tuan Update MBTEEGPacket to use this
    // Start temporary
    val eegPacket = MbtEEGPacket()
    // End temporary
//    guard let eegPacket = MBTEEGPacket(
//      buffer: values,
//      electrodeToChannelIndex: electrodeToChannelIndex
//    ) else {
//      return nil
//    }

    if (hasQualityChecker) {
      val qualities = generateQualities(eegPacket)
      eegPacket.addQualities(qualities)

      val modifiedValues = generateModifiedValues(eegPacket)
      eegPacket.setModifiedChannelsData(modifiedValues, sampleRate)
    }
    return eegPacket
  }

  /// Get qualities from signal processing
  private fun generateQualities(eegPacket: MbtEEGPacket): FloatArray {
    // TODO: To check
    val buffer = eegPacket.channelsData

    val qualities =
    signalProcessor.computeQualityValue(buffer, sampleRate, eegPacketLength)
    return qualities
  }

  /// Get Eeg modified values from signal progression
  private fun generateModifiedValues(
    eegPacket: MbtEEGPacket
  ): Array<Array<Float>> {
    val correctedValues = signalProcessor.getModifiedEEGValues()
    return correctedValues
  }
}