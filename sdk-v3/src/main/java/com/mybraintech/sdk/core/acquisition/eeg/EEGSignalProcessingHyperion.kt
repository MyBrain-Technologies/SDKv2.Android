package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.android.jnibrainbox.BrainBoxVersion
import com.mybraintech.sdk.core.listener.EEGListener
import com.mybraintech.sdk.core.model.*

class EEGSignalProcessingHyperion(streamingParams: StreamingParams, eegListener: EEGListener) :
    EEGSignalProcessingIndus5(streamingParams, eegListener) {

    override fun getDeviceType(): EnumMBTDevice {
        return EnumMBTDevice.HYPERION
    }

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