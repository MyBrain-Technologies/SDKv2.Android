package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.sdk.core.bluetooth.deviceinformation.DeviceInformation
import com.mybraintech.sdk.core.shared.MBTRelaxIndexAlgorithm
import core.eeg.signalprocessing.ContextSP
import core.eeg.storage.MbtEEGPacket


// TODO: 09/11/2021 : implement
class EEGAcquisier(
  private val signalProcessor: SignalProcessingManager,
  private val acquisitionProcessor: EEGAcquisitionProcessor,
  private val  eegPacketManager: EEGPacketManager = EEGPacketManager(),
  private val acquisisitonSaver: EEGAcquisitionSaver = EEGAcquisitionSaver()
  ) {

  //----------------------------------------------------------------------------
  // MARK: - Properties
  //----------------------------------------------------------------------------

  /********************  Parameters ********************/

  /// Bool to know if developer wants to use QC or not.
  var hasQualityChecker: Bool = false
  private set

  /// if the sdk record in DB EEGPacket
  var isRecording: Boolean = false
  set(value) {
    field = value
    if (value) {
      eegPacketManager.removeAllEegPackets()
    }
  }

  //----------------------------------------------------------------------------
  // Initialization
  //----------------------------------------------------------------------------

  // TODO: Anh Tuan add to init :
  // init() { signalProcessor.resetSession() }

  //==============================================================================
  // MARK: - Packets
  //==============================================================================

  func getLastPackets(count: Int) -> [MBTEEGPacket]? {
    return eegPacketManager.getLastPackets(count)
  }

  //----------------------------------------------------------------------------
  // MARK: - Manage streaming datas methods.
  //----------------------------------------------------------------------------

  /// Method called by MelomindEngine when a new EEG streaming
  /// session has began. Method will make everything ready, acquisition side
  /// for the new session.
  fun startStream(isUsingQualityChecker: Boolean, sampleRate: Int) {
    // Start mainQualityChecker.
    if (!isUsingQualityChecker) { return }

    signalProcessor.initializeQualityChecker(sampleRate.toFloat())
    hasQualityChecker = true
  }


  /// Method called by MelomindEngine when the current EEG streaming
  /// session has finished.
  fun stopStream() {
    // Dealloc mainQC.
    if (!isUsingQualityChecker) { return }

    hasQualityChecker = false
    signalProcessor.deinitQualityChecker()
  }

//  /// Save the EEGPackets recorded
//  ///
//  /// - Parameters:
//  ///   - idUser: A *Int* id of the connected user
//  ///   - comments: An array of *String* contains Optional Comments
//  ///   - completion: A block which execute after create the file or fail to
//  ///   create
//  fun saveRecording(userId: Int,
//                    algorithm: MBTRelaxIndexAlgorithm?,
//                    comments: Array<String> = arrayOf(),
//                    device: DeviceInformation,
//                    recordingInformation: MBTRecordInfo,
//                    recordFileSaver: RecordFileSaver,
//                    completion: @escaping (Result<URL, Error>) -> Void) {
//
////    let packets = eegPacketManager.eegPackets
////      let qualities = eegPacketManager.qualities
////      let channelData = eegPacketManager.eegData
////      acquisisitonSaver.saveRecording(packets: packets,
////                                      qualities: qualities,
////                                      channelData: channelData,
////                                      idUser: idUser,
////                                      algorithm: algorithm,
////                                      comments: comments,
////                                      deviceInformation: device,
////                                      recordingInformation: recordingInformation,
////                                      recordFileSaver: recordFileSaver) {
////        [weak self] result in
////        switch result {
////          case .success(let url):
////          self?.eegPacketManager.removeAllEegPackets()
////          completion(.success(url))
////
////          case .failure(let error):
////          if (error as? EEGAcquisitionSaverV2.EEGAcquisitionSaverError)
////          == .unableToWriteFile {
////          self?.eegPacketManager.removeAllEegPackets()
////        }
////          completion(.failure(error))
////        }
////      }
//  }

  //----------------------------------------------------------------------------
  // MARK: - Process Received data Methods.
  //----------------------------------------------------------------------------

  /// Process the brain activty measurement received and return the processed
  /// data.
  /// - Parameters:
  ///     - data: *Data* received from MBT Headset EEGs.
  /// - Returns: *Dictionnary* with the packet Index (key: "packetIndex") and
  /// array of P3 and P4 samples arrays ( key: "packet" )
  fun generateEegPacket(eegData: ByteArray): MbtEEGPacket? {
    val eegPacket =
      acquisitionProcessor.getEEGPacket(eegData, hasQualityChecker)
      ?: return null


    if (isRecording) {
      eegPacketManager.saveEEGPacket(eegPacket)
    }

    return eegPacket
  }



}