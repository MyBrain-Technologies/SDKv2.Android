package com.mybraintech.sdk.core.recording

import com.mybraintech.sdk.core.acquisition.EnumSignalType
import com.mybraintech.sdk.core.model.MbtEEGPacket
import com.mybraintech.sdk.core.model.TimedBLEFrame

abstract class BaseEEGRecording : SignalRecordingInterface<TimedBLEFrame, MbtEEGPacket> {

    override fun getSignalType(): EnumSignalType {
        return EnumSignalType.EEG
    }
}