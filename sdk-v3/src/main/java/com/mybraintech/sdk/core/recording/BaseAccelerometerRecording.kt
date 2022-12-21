package com.mybraintech.sdk.core.recording

import com.mybraintech.sdk.core.acquisition.EnumSignalType
import com.mybraintech.sdk.core.model.ThreeDimensionalPosition

abstract class BaseAccelerometerRecording : SignalRecordingInterface<ByteArray, ThreeDimensionalPosition> {

    override fun getSignalType(): EnumSignalType {
        return EnumSignalType.ACCELEROMETER
    }
}