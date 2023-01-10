package com.mybraintech.sdk.core.recording

import com.mybraintech.sdk.core.acquisition.EnumSignalType
import com.mybraintech.sdk.core.model.PPGPacket

abstract class BasePPGRecording : SignalRecordingInterface<ByteArray, PPGPacket> {
    override fun getSignalType(): EnumSignalType {
        return EnumSignalType.PPG
    }

    override fun getSampleRate(): Int {
        throw UnsupportedOperationException()
    }
}