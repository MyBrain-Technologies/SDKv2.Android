package com.mybraintech.sdk.core.acquisition.eeg

import core.eeg.storage.MbtEEGPacket

class EEGPacketManager {

  //----------------------------------------------------------------------------
  // MARK: - Properties
  //----------------------------------------------------------------------------

  var eegPackets: MutableList<MbtEEGPacket> = MutableList(0)
  private set

  /******************** Array getters ********************/

  // TODO: Anh Tuan. It was supposed to be a computed property but kotlin force
  // to have an initialized value

  val eegData: Array<Array<Float?>>
  get() {
    return emptyArray()
////    var eegDatas = [[Float?]]()
////    for eegPacket in eegPackets {
////      for channelNumber in 0 ..< eegPacket.channelsData.count {
////        let channelData = eegPacket.channelsData[channelNumber]
////
////        if eegDatas.count < channelNumber + 1 {
////          eegDatas.append([Float?]())
////        }
////
////        for packetIndex in 0 ..< channelData.count {
////          if channelData[packetIndex].isNaN {
////            eegDatas[channelNumber].append(nil)
////            log.info("Get JSON EEG data", context: Float.nan)
////          } else {
////            let value = channelData[packetIndex]
////            eegDatas[channelNumber].append(value)
////          }
////        }
////      }
//      }
//    let hasData = eegDatas.compactMap({
//      $0.contains(Float.nan) || $0.contains(Float.signalingNaN)
//    })
//
//    log.info("Get JSON EEG data", context: hasData)
//
//    return eegDatas
  }

  // TODO: Anh Tuan don't know how to generate an Array<Array<Float>> from this.
  val qualities: ArrayList<ArrayList<Float>>
  get() {
//    return emptyArray() //arrayOf(0.0.toFloat()))

    var qualities = ArrayList<ArrayList<Float>>(0)
    for (eegPacket in eegPackets) {
      for (indexQuality in 0 until eegPacket.qualities.size) {
        if (qualities.size < indexQuality + 1) {
          qualities.add(ArrayList(0))
        }
        val quality = eegPacket.qualities[indexQuality]
        qualities[indexQuality].add(quality)
      }
    }
    return qualities
    //    var qualities = [[Float]]()
//    for eegPacket in eegPackets {
//      for indexQuality in 0 ..< eegPacket.qualities.count {
//        if qualities.count < indexQuality + 1 {
//          qualities.append([Float]())
//        }
//        qualities[indexQuality].append(eegPacket.qualities[indexQuality])
//      }
//    }
//
//    return qualities
  }

  //----------------------------------------------------------------------------
  // MARK: - Create
  //----------------------------------------------------------------------------

  /// Method to persist EEGPacket received in the Realm database.
  /// - Parameters:
  ///     - eegPacket: *MBTEEGPacket* freshly created, soon db-saved.
  /// - Returns: The *MBTEEGPacket* saved in Realm-db.
  fun saveEEGPacket(eegPacket: MbtEEGPacket) {
    eegPackets.add(eegPacket)
  }

  //----------------------------------------------------------------------------
  // MARK: - Read
  //----------------------------------------------------------------------------

  /// Get last n *MBTEEGPackets* from the Realm DB.
  /// - Parameters:
  ///     - n: Number of *MBTEEGPackets* wanted.
  /// - Returns: The last n *MBTEEGPacket*.
  fun getLastPackets(n: Int): Array<MbtEEGPacket>? {
    if (eegPackets.size < n) { return null }
    return eegPackets.subList(0, n).toTypedArray()
  }

  //----------------------------------------------------------------------------
  // MARK: - Delete
  //----------------------------------------------------------------------------

  /// Delete all EEGPacket saved in Realm DB.
  fun removeAllEegPackets() {
    eegPackets.clear()
  }


}