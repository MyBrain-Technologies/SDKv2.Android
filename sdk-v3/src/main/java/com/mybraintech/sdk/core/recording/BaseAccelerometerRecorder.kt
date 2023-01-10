package com.mybraintech.sdk.core.recording

import com.mybraintech.sdk.core.acquisition.EnumSignalType
import com.mybraintech.sdk.core.model.AccelerometerConfig
import com.mybraintech.sdk.core.model.EnumAccelerometerSampleRate
import com.mybraintech.sdk.core.model.ThreeDimensionalPosition
import io.reactivex.disposables.CompositeDisposable

abstract class BaseAccelerometerRecorder : SignalRecordingInterface<ByteArray, ThreeDimensionalPosition> {

    private var _sampleRate = EnumAccelerometerSampleRate.F_100_HZ
    protected var disposable = CompositeDisposable()

    override fun getSignalType(): EnumSignalType {
        return EnumSignalType.ACCELEROMETER
    }

    override fun getSampleRate(): Int {
        return _sampleRate.sampleRate
    }

    fun setSampleRate(sampleRate: EnumAccelerometerSampleRate) {
        this._sampleRate = sampleRate
    }

    abstract fun onAccelerometerConfiguration(accelerometerConfig: AccelerometerConfig)

    override fun dispose() {
        disposable.dispose()
    }
}