package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.android.jnibrainbox.BrainBoxVersion
import com.mybraintech.sdk.core.model.EEGParams
import com.mybraintech.sdk.core.model.EnumMBTDevice


class SignalProcessingManager(deviceType: EnumMBTDevice, eegParams: EEGParams) {
    private val hasComputedCalibrationDefaultValue: Boolean = false
//    private val eegQualityProcessor: EEGQualityProcessor =
//        EEGQualityProcessor(sampleRate)
//    private val eegRelaxIndexProcessor: EEGToRelaxIndexProcessor = EEGToRelaxIndexProcessor()

    var eegSignalProcessing: EEGSignalProcessing

    init {
        if (deviceType == EnumMBTDevice.Q_PLUS) {
            eegSignalProcessing = EEGSignalProcessingIndus5(eegParams)
        } else {
            TODO("not qplus, to implement")
        }
    }
}