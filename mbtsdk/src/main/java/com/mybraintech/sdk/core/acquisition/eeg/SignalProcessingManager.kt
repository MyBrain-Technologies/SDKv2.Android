package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.sdk.core.acquisition.eeg.signalprocessing.EEGQualityProcessor
import com.mybraintech.sdk.core.shared.MBTRelaxIndexAlgorithm




// TODO: Anh Tuan MBTQualityCheckerBridge is the BrainBox





class SignalProcessingManager(
  val hasComputedCalibrationDefaultValue: Boolean = false
) {


  //----------------------------------------------------------------------------
  // MARK: - Properties
  //----------------------------------------------------------------------------

  /******************** Singleton ********************/

  /// Dictionnary to store calibration results.
  var hasComputedCalibration: Boolean = hasComputedCalibrationDefaultValue
  private set

  ///
  var sampleRate: Int = 0

  ///
  internal var eegPacketLength: Int = 0

  // TODO: Anh Tuan is it necessary?
//  internal var relaxIndexAlgorithm = MBTRelaxIndexAlgorithm.algorithm(
//    fromSDKVersion: MBTQualityCheckerBridge.getVersion()!
//  )

  //----------------------------------------------------------------------------
  // MARK: - Initialization
  //----------------------------------------------------------------------------


  /// Initalize MBT_MainQC to enable MBT_QualityChecker methods.
  fun initializeQualityChecker(sampleRate: Float,
                               accuracy: Float = 0.85.toFloat()) {
    // TODO: Anh Tuan Add BrainBox here
//    MBTQualityCheckerBridge.initializeMainQualityChecker(sampleRate,
//                                                         accuracy)
  }

  /// Delete MBT_MainQC instance once acquisition phase is over.
  fun deinitQualityChecker() {
    // TODO: Anh Tuan Add BrainBox here
//    MBTQualityCheckerBridge.deInitializeMainQualityChecker()
  }

  //----------------------------------------------------------------------------
  // MARK: - Quality
  //----------------------------------------------------------------------------

  /// Compute datas in the *Quality Checker* and returns an array of *Quality*
  /// values for a data matrix of an acquisition packet.
  /// - parameter data: The data matrix of the packet. Each row is a channel
  /// (no GPIOs)
  /// - returns: The array of computed "quality" values. Each value is the
  /// quality for a channel, in the same order as the row order in data.
  fun computeQualityValue(data: Array<Array<Float>>): FloatArray {
    val packetLength = data.firstOrNull()?.size ?: return FloatArray(0)

    return EEGQualityProcessor.computeQualityValue(data,
                                                   sampleRate,
                                                   packetLength)
  }

  fun computeQualityValue(data: Array<Array<Float>>,
                          sampleRate: Int,
                          eegPacketLength: Int): FloatArray {
    // FIXME: Why is it passed as parameter and the property being init here?
    this.sampleRate = sampleRate
    this.eegPacketLength = eegPacketLength
    return computeQualityValue(data)
  }

  //----------------------------------------------------------------------------
  // MARK: - EEG
  //----------------------------------------------------------------------------

  /// Get an array of the modified EEG datas by the *Quality Checker*, and
  /// return it.
  /// - returns: The matrix of EEG datas (modified) by channel.
  fun getModifiedEEGValues(): Array<Array<Float>> {
    // TODO: Anh Tuan Add BrainBox here and todo
//    val newEEGValues = MBTQualityCheckerBridge.getModifiedEEGData()
//    val newEEGValuesSwift = newEEGValues as? [[Float]] ?? [[]]
//
//    return newEEGValuesSwift
  }


//==============================================================================
// MARK: - MBTCalibrationComputer
//==============================================================================

  /// Compute calibration from modified EEG Data and qualities,
  /// from the last complete packet until the *n* last packet.
  /// - Parameters:
  ///     - packetsCount: Number of packets to get, from the last one.
  /// - Returns: A dictionnary with calibration datas from the CPP Signal
  /// Processing.
  fun computeCalibration(
    packets: Array<MbtEEGPacket>,
  sampleRate: Int,
  channelCount: Int,
  packetLength: Int
  ) -> CalibrationOutput? {
    let calibrationResult =
    EEGCalibrationProcessor.computeCalibrationV2(lastPackets: packets,
                                                 packetLength: packetLength,
                                                 sampleRate: sampleRate,
                                                 channelCount: channelCount)
    hasComputedCalibration = calibrationResult != nil ? true : false
    return calibrationResult
  }

}

//==============================================================================
// MARK: - MBTRelaxIndexComputer
//==============================================================================

extension SignalProcessingManager {

  func computeRelaxIndex(eegPackets: [MBTEEGPacket],
  sampleRate: Int,
  channelCount: Int) -> Float? {
    guard hasComputedCalibration else { return 0 }
    return EEGToRelaxIndexProcessor.computeRelaxIndex(from: eegPackets,
                                                      sampRate: sampleRate,
                                                      nbChannels: channelCount)
  }

}

//==============================================================================
// MARK: - MBTSessionAnalysisComputer
//==============================================================================

extension SignalProcessingManager {

  //Implementing MBT_SessionAnalysisComputer
  func analyseSession(snrValues: [Float],
  threshold: Float) -> [String: Float] {
    guard snrValues.count > 3 else { return [:] }

    //Perform the computation
    let sessionAnalysisValues =
    MBTSNRStatisticsBridge.computeSessionStatistics(snrValues,
                                                    threshold: threshold)
    let sessionAnalysis = sessionAnalysisValues as? [String: Float] ?? [:]
    return sessionAnalysis
  }

}

//==============================================================================
// MARK: - MBTMelomindAnalysis
//==============================================================================

extension SignalProcessingManager {

  func resetSession() {
    MBTMelomindAnalysis.resetSession()
  }

}
}