package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.android.jnibrainbox.BrainBoxVersion
import com.mybraintech.sdk.core.model.EEGParams
import com.mybraintech.sdk.core.model.EnumMBTDevice


class SignalProcessingManager(deviceType: EnumMBTDevice, eegParams: EEGParams) {
    private val hasComputedCalibrationDefaultValue: Boolean = false
    private val sampleRate: Int = 250
//    private val eegQualityProcessor: EEGQualityProcessor =
//        EEGQualityProcessor(sampleRate)
//    private val eegRelaxIndexProcessor: EEGToRelaxIndexProcessor = EEGToRelaxIndexProcessor()

    var eegSignalProcessing: EEGSignalProcessing

    init {
        if (deviceType == EnumMBTDevice.Q_PLUS) {
            val statusAlloc = if (eegParams.isStatusEnabled) {
                deviceType.getStatusAllocationSize()
            } else {
                0
            }
            eegSignalProcessing = EEGSignalProcessingIndus5(
                sampleRate,
                statusAlloc,
                eegParams.isQualityCheckerEnabled
            )
        } else {
            TODO("not qplus, to implement")
        }
    }

    val brainBoxVersion: String
        get() {
            return BrainBoxVersion.getVersion()
        }

}