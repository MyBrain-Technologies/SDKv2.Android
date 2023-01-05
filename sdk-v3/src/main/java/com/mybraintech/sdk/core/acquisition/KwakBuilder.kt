package com.mybraintech.sdk.core.acquisition

import com.mybraintech.android.jnibrainbox.BrainBoxVersion
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.Kwak
import com.mybraintech.sdk.core.model.KwakHeader
import com.mybraintech.sdk.core.model.RecordingOption

class KwakBuilder {
    fun createKwak(
        deviceType: EnumMBTDevice,
        recordingOption: RecordingOption,
        isQualityCheckerEnabled: Boolean
    ): Kwak {
        val kwakHeader = when (deviceType) {
            EnumMBTDevice.HYPERION -> KwakHeader().getHyperionHeader()
            EnumMBTDevice.MELOMIND -> KwakHeader().getMelomindHeader()
            EnumMBTDevice.Q_PLUS -> KwakHeader().getQPlusHeader()
            EnumMBTDevice.UNDEFINED -> KwakHeader().getBaseHeader()
        }.apply {
            deviceInfo = recordingOption.deviceInformation
            setRecordingNb(recordingOption.recordingNb)
        }

        return Kwak().apply {
            context = recordingOption.context
            header = kwakHeader
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