package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.android.jnibrainbox.BrainBoxVersion
import com.mybraintech.android.jnibrainbox.RelaxIndexSessionOutputData
import com.mybraintech.sdk.core.acquisition.eeg.signalprocessing.EEGQualityProcessor
import com.mybraintech.sdk.core.acquisition.eeg.signalprocessing.EEGToRelaxIndexProcessor
import com.mybraintech.sdk.core.listener.EEGListener
import com.mybraintech.sdk.core.model.EnumMBTDevice
import core.eeg.storage.MbtEEGPacket


// TODO: Anh Tuan MBTQualityCheckerBridge is the BrainBox

class SignalProcessingManager(deviceType: EnumMBTDevice) {
    private val hasComputedCalibrationDefaultValue: Boolean = false
    private val sampleRate: Int = 250
    private val eegQualityProcessor: EEGQualityProcessor =
        EEGQualityProcessor(sampleRate)
    private val eegRelaxIndexProcessor: EEGToRelaxIndexProcessor = EEGToRelaxIndexProcessor()

    var eegAcquisier: EEGAcquisier
    var eegListener: EEGListener? = null

    init {
        if (deviceType == EnumMBTDevice.Q_PLUS) {
            eegAcquisier = EEGAcquisierIndus5()
        } else {
            TODO("not qplus, to implement")
        }
    }

    //----------------------------------------------------------------------------
    // MARK: - Properties
    //----------------------------------------------------------------------------

    /// Dictionnary to store calibration results.
    var hasComputedCalibration: Boolean = hasComputedCalibrationDefaultValue
        private set

    ///
    internal var eegPacketLength: Int = 0


    /******************** Versioning ********************/

    val brainBoxVersion: String
        get() {
            return BrainBoxVersion.getVersion()
        }

    val qualityCheckerVersion: String
        get() {
            return eegQualityProcessor.qualityCheckerVersion
        }

    //----------------------------------------------------------------------------
    // MARK: - Initialization
    //----------------------------------------------------------------------------

    //----------------------------------------------------------------------------
    // MARK: - Quality
    //----------------------------------------------------------------------------

    /// Compute datas in the *Quality Checker* and returns an array of *Quality*
    /// values for a data matrix of an acquisition packet.
    /// - parameter data: The data matrix of the packet. Each row is a channel
    /// (no GPIOs)
    /// - returns: The array of computed "quality" values. Each value is the
    /// quality for a channel, in the same order as the row order in data.
    fun computeQualityValue(data: ArrayList<ArrayList<Float>>): FloatArray {
        return eegQualityProcessor.computeQualityValue(data)
    }

    //----------------------------------------------------------------------------
    // MARK: - EEG
    //----------------------------------------------------------------------------

    /// Get an array of the modified EEG datas by the *Quality Checker*, and
    /// return it.
    /// - returns: The matrix of EEG datas (modified) by channel.
    fun getModifiedEEGValues(): Array<Array<Float>> {
        TODO("Add BrainBox here and todo")
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

    // TODO: Anh Tuan to check
//  fun computeCalibration(
//    packets: Array<MbtEEGPacket>,
//  sampleRate: Int,
//  channelCount: Int,
//  packetLength: Int
//  ) -> CalibrationOutput? {
//    let calibrationResult =
//    EEGCalibrationProcessor.computeCalibrationV2(lastPackets: packets,
//                                                 packetLength: packetLength,
//                                                 sampleRate: sampleRate,
//                                                 channelCount: channelCount)
//    hasComputedCalibration = calibrationResult != nil ? true : false
//    return calibrationResult
//  }


//==============================================================================
// MARK: - MBTRelaxIndexComputer
//==============================================================================

    fun computeRelaxIndex(eegPackets: Array<MbtEEGPacket>): Float? {
        TODO("Implement")
//    if (!hasComputedCalibration) { return 0.0.toFloat() }
//    eegRelaxIndexProcessor.computeRelaxIndex(eegPackets)
    }

//==============================================================================
// MARK: - MBTSessionAnalysisComputer
//==============================================================================

    //Implementing MBT_SessionAnalysisComputer
    fun analyseSession(
        snrValues: Array<Float>,
        threshold: Float
    ): Map<String, Float> {
        if (snrValues.size <= 3) {
            return emptyMap()
        }

        // TODO: Anh Tuan Use the brainbox here
        //Perform the computation
//    val sessionAnalysisValues =
//    MBTSNRStatisticsBridge.computeSessionStatistics(snrValues,  threshold)
//    val sessionAnalysis = sessionAnalysisValues as? [String: Float] ?? [:]
//    return sessionAnalysis
        return emptyMap()
    }

    fun endSessionAndGenerateSessionStatistic(): RelaxIndexSessionOutputData {
        return eegRelaxIndexProcessor.endSessionAndGenerateSessionStatistic()
    }

//==============================================================================
// MARK: - MBTMelomindAnalysis
//==============================================================================

    fun resetSession() {
        TODO("Anh Tuan Use the brainbox here")
//    MBTMelomindAnalysis.resetSession()
    }

}