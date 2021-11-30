package com.mybraintech.sdk.core.acquisition.eeg.packet

import java.lang.Math.pow

// TODO: Anh Tuan
object EEGDeserializer {
  private val shiftTo32Bytes: Int = 8 + 4
  private val checkSign: Int = (0x80 << shiftTo32Bytes)
  private val negativeMask: Int = (0xFFFFFFF << (32 - shiftTo32Bytes))
  private val positiveMask: Int = (~negativeMask)
  private val divider = 2

  /// Constants to decode eeg values to float relax indexes values
  private val voltageADS1299: Float = (0.286 * pow(10, -6)) / 8)

  //----------------------------------------------------------------------------
  // MARK: - Methods
  //----------------------------------------------------------------------------

  /// Deserialize uint8 values received from headset to relax indexes (float) values
  fun deserializeToRelaxIndex(
    bytes: ByteArray,
    numberOfElectrodes: Int = 2
  ): Array<Array<Float>> {
//    let bytesConvertedTo32 = convert24to32Bit(bytes: bytes)
//
//    let desamplifiedValues = removeAmplification(values: bytesConvertedTo32)
//
//    let electrodesArray =
//    spreadBetweenElectrodes(values: desamplifiedValues,
//                            numberOfElectrodes: numberOfElectrodes)
//
//    return electrodesArray

    return Array<Array<Float>>(0)
  }

  //----------------------------------------------------------------------------
  // MARK: - Tools
  //----------------------------------------------------------------------------

  /// Spread eeg values between number of electrodes.
  fun spreadBetweenElectrodes(values: Array<Float>,
                              numberOfElectrodes: Int): Array<Array<Float>> {
//    let electrodesArray = values.spread(numberOfArrays: numberOfElectrodes)
//    { index, _ in index % numberOfElectrodes }
//    return electrodesArray
    return Array<Array<Float>>(0)
  }

  /// Convert a list of bytes on 24 bit to 32 bit values
  fun convert24to32Bit(bytes: ByteArray): IntArray {
//    var values = [Int32]()
//
//    for i in 0 ..< bytes.count / divider  {
//      let temp = convert24to32Bit(bytes: bytes, at: divider * i)
//      values.append(temp)
//    }
//
//    return values

    return IntArray(0)
  }

  /// Convert a 24 bit values to 32 bit value. Uses two uint8 value to create a 32 bit value.
  fun convert24to32Bit(bytes: ByteArray, index: Int): Int {
//    var temp: Int32 = 0x00000000
//
//    temp = (Int32(bytes[index] & 0xFF) << shiftTo32Bytes)
//    | Int32(bytes[index + 1] & 0xFF) << (shiftTo32Bytes - 8)
//
//    let isNegative = (temp & checkSign) > 0
//    temp = isNegative ? Int32(temp | negativeMask) : Int32(temp & positiveMask)
//    return temp

    return 0
  }

  /// Decode eeg value by removing amplification
  fun removeAmplification(values: IntArray): FloatArray {
//    return values.map() { Float($0) * voltageADS1299 }
    return FloatArray(0)
  }
}