package com.mybraintech.sdk.core.bluetooth.deviceinformation

data class DeviceAcquisitionInformation(val indusVersion: IndusVersion) {
  val channelCount: Int

  /// The rate at which EEG data is being sent by the headset.
  val sampleRate: Int

  /// An EEG Packet length.
  val eegPacketSize: Int

  val eegPacketMaxSize: Int

  /// Sample rate of the ims
  val imsSampleRate: Int

  val imsPacketMaxSize: Int

  val imsAxisCount: Int

//  val electrodes: Electrodes

  init {
    val sampleByteSize = 2

    when(indusVersion) {
      IndusVersion.Indus2, IndusVersion.Indus3 -> {
        channelCount = 2
        sampleRate = 250
        eegPacketSize = 250
        eegPacketMaxSize = eegPacketSize * channelCount * sampleByteSize
        imsSampleRate = 100
        imsAxisCount = 3
        imsPacketMaxSize = imsSampleRate * imsAxisCount
//        electrodes = Electrodes(acquisitions: [.p3, .p4],
//        references: [.m1],
//        grounds: [.m2])
      }
      IndusVersion.Indus5 -> {
        channelCount = 4
        sampleRate = 250
        eegPacketSize = 250
        eegPacketMaxSize = eegPacketSize * channelCount * sampleByteSize
        imsSampleRate = 100
        imsAxisCount = 3
        imsPacketMaxSize = imsSampleRate * imsAxisCount

        // TODO: Check channel order
//        self.electrodes = Electrodes(acquisitions: [.p3, .p4, .af3, .af4],
//        references: [.m1],
//        grounds: [.m2])
        // TODO: Use this instead?")
        // channelCount = electrodes.acquisitions.count
      }
    }
  }
}
