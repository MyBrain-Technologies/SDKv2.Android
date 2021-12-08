package com.mybraintech.sdk.core.acquisition.eeg.packet


object EEGDeserializer {

  //----------------------------------------------------------------------------
  // MARK: - Methods
  //----------------------------------------------------------------------------

  fun deserializeToRelaxIndex(
    bytes: ByteArray,
    numberOfElectrodes: Int = 2
  ): Array<Array<Float>> {

    TODO("Find input type")
//    val protocol = BluetoothProtocol.LOW_ENERGY
//    val eegMatrix = MbtDataConversion.convertRawDataToEEG(bytes,
//                                                          protocol,
//                                                          numberOfElectrodes)
//    return eegMatrix
  }

}