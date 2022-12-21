package com.mybraintech.sdk.core.acquisition

import com.mybraintech.sdk.core.listener.EEGFrameDecodeInterface
import com.mybraintech.sdk.core.listener.EEGRealtimeListener
import com.mybraintech.sdk.core.model.EEGSignalPack
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.MbtDataConversion2
import com.mybraintech.sdk.core.model.TimedBLEFrame
import com.mybraintech.sdk.util.MatrixUtils2
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

internal class RealtimeEEGExecutorImpl(private val eegFrameDecodeInterface: EEGFrameDecodeInterface) :
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
            .subscribe(
                { bleFrame -> consumeFrame(bleFrame) },
                Timber::e
            )
            .addTo(disposable)
    }

    override fun setListener(eegRealtimeListener: EEGRealtimeListener) {
        this.listener = eegRealtimeListener
    }

    override fun terminate() {
        disposable.dispose()
    }

    private fun consumeFrame(timedBLEFrame: TimedBLEFrame) {
        val eegSignals = eegFrameDecodeInterface.decodeEEGData(timedBLEFrame.data)
        val statuses: List<Float> = eegSignals.map { it.statusData }
        val index = IndexReader.decodeIndex(timedBLEFrame.data)
        val invertedEEGs = dataConversion.convertRawDataToEEG(eegSignals)
        val standardEEGs = MatrixUtils2.invertFloatMatrix(invertedEEGs)
        listener?.onEEGFrame(
            EEGSignalPack(
                timestamp = timedBLEFrame.timestamp,
                index = index,
                eegSignals = standardEEGs,
                triggers = statuses
            )
        )
    }

    override fun onEEGFrame(eegFrame: TimedBLEFrame) {
        publishSubject.onNext(eegFrame)
    }
}