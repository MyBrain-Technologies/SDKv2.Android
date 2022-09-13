package com.mybraintech.sdk.core.acquisition

import com.mybraintech.sdk.core.listener.EEGFrameConversionInterface
import com.mybraintech.sdk.core.listener.EEGRealtimeListener
import com.mybraintech.sdk.core.model.EEGSignalPack
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.MbtDataConversion2
import com.mybraintech.sdk.core.model.TimedBLEFrame
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject

internal class RealtimeEEGExecutorImpl(private val eegFrameConversionInterface: EEGFrameConversionInterface) :
    RealtimeEEGExecutor {

    private var workerScheduler =
        RxJavaPlugins.createSingleScheduler(AcquisierThreadFactory) // realtime EEG
    private var listener: EEGRealtimeListener? = null // realtime EEG
    private var publishSubject = PublishSubject.create<TimedBLEFrame>()
    lateinit var dataConversion: MbtDataConversion2
    private val disposable = CompositeDisposable()

    override fun init(deviceType: EnumMBTDevice) {
        dataConversion = MbtDataConversion2.generateInstance(deviceType)

        publishSubject.observeOn(workerScheduler)
            .subscribeOn(workerScheduler)
            .subscribe {
                consumeFrame(it)
            }
            .addTo(disposable)
    }

    override fun setListener(eegRealtimeListener: EEGRealtimeListener) {
        this.listener = eegRealtimeListener
    }

    override fun terminate() {
        disposable.dispose()
    }

    private fun consumeFrame(timedBLEFrame: TimedBLEFrame) {
        val eegSignals = eegFrameConversionInterface.getEEGData(timedBLEFrame.data)
        val index = IndexReader.decodeIndex(timedBLEFrame.data)
        val standardEEGs = dataConversion.convertRawDataToEEG(eegSignals)
        listener?.onEEGFrame(
            EEGSignalPack(
                timestamp = timedBLEFrame.timestamp,
                index = index,
                signals = standardEEGs
            )
        )
    }

    override fun onEEGFrame(eegFrame: TimedBLEFrame) {
        publishSubject.onNext(eegFrame)
    }
}