package com.mybraintech.sdk.core.acquisition.eeg.packet

class EEGRawPacket(
  val rawValue: ByteArray
) {

  //----------------------------------------------------------------------------
  // MARK: - Properties
  //----------------------------------------------------------------------------

  /// Index of a packet is stored in the two first value
  val packetIndex: Short
  get() {
    //TODO: Anh Tuan
    return 0
//    return Int16(rawValue[0] & 0xff) << 8 | Int16(rawValue[1] & 0xff)
  }

  val packetIndexValues: ByteArray
  get() {
    //TODO: Anh Tuan
    return ByteArray(0)
//    return Array(rawValue.prefix(2))
  }

  /// Value of a packet is stored after the two first value (wich are
  // `packetIndex` property)
  val packetValues: ByteArray
  get() {
    //TODO: Anh Tuan
    return ByteArray(0)
//    return rawValue.suffix(rawValue.count - 2)
  }

  val packetValuesLength: Int
  get() {
    return packetValues.size
  }

}