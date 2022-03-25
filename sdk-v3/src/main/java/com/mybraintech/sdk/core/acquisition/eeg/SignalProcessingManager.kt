package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.android.jnibrainbox.BrainBoxVersion
import com.mybraintech.sdk.core.model.EEGParams
import com.mybraintech.sdk.core.model.EnumMBTDevice
import java.lang.UnsupportedOperationException


class SignalProcessingManager(deviceType: EnumMBTDevice, eegParams: EEGParams) {
    private val hasComputedCalibrationDefaultValue: Boolean = false
//    private val eegQualityProcessor: EEGQualityProcessor =
//        EEGQualityProcessor(sampleRate)
//    private val eegRelaxIndexProcessor: EEGToRelaxIndexProcessor = EEGToRelaxIndexProcessor()

    var eegSignalProcessing: EEGSignalProcessing = when (deviceType) {
        EnumMBTDevice.Q_PLUS -> {
            EEGSignalProcessingIndus5(eegParams)
        }
        EnumMBTDevice.MELOMIND -> {
            EEGSignalProcessingMelomind(eegParams)
        }
        else -> {
            throw UnsupportedOperationException("device type ")
        }
    }

}