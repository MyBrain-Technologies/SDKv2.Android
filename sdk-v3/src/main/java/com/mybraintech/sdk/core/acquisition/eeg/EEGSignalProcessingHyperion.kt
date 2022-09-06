package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.android.jnibrainbox.BrainBoxVersion
import com.mybraintech.sdk.core.listener.EEGListener
import com.mybraintech.sdk.core.model.Kwak
import com.mybraintech.sdk.core.model.KwakHeader
import com.mybraintech.sdk.core.model.RecordingOption
import com.mybraintech.sdk.core.model.StreamingParams

class EEGSignalProcessingHyperion(streamingParams: StreamingParams, eegListener: EEGListener) :
    EEGSignalProcessingIndus5(streamingParams, eegListener) {

    override fun createKwak(recordingOption: RecordingOption): Kwak {
        return Kwak().apply {
            context = recordingOption.context
            header = KwakHeader.getHyperionHeader().apply {
                deviceInfo = recordingOption.deviceInformation
                setRecordingNb(recordingOption.recordingNb)
            }
            recording.recordID = recordingOption.recordId
            recording.recordingType.recordType = recordingOption.recordingType
            if (isQualityCheckerEnabled) {
                recording.recordingType.spVersion = BrainBoxVersion.getVersion()
            } else {
                recording.recordingType.spVersion = "0.0.0"
            }
        }
    }
}