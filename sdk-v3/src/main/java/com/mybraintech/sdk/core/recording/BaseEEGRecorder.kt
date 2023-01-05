package com.mybraintech.sdk.core.recording

import com.mybraintech.sdk.core.acquisition.EnumSignalType
import com.mybraintech.sdk.core.listener.EEGRealtimeListener
import com.mybraintech.sdk.core.model.EEGSignalPack
import com.mybraintech.sdk.core.model.EEGStreamingErrorCounter
import com.mybraintech.sdk.core.model.MbtEEGPacket
import com.mybraintech.sdk.core.model.TimedBLEFrame
import io.reactivex.disposables.CompositeDisposable

/**
 * [BaseEEGRecorder] class receives data from caller and accumulate these data to generate [MbtEEGPacket]
 */
abstract class BaseEEGRecorder(private var eegCallback: EEGCallback?) :
    SignalRecordingInterface<TimedBLEFrame, MbtEEGPacket> {

    protected val _sampleRate = 250
    protected val disposable = CompositeDisposable()
    private var realtimeListener: EEGRealtimeListener? = null

    override fun getSignalType(): EnumSignalType {
        return EnumSignalType.EEG
    }

    override fun getSampleRate() : Int {
        return _sampleRate
    }

    fun setRealtimeListener(eegRealtimeListener: EEGRealtimeListener?) {
        this.realtimeListener = eegRealtimeListener
    }

    fun hasRealtimeListener(): Boolean {
        return this.realtimeListener != null
    }

    protected fun notifyRealtime(pack: EEGSignalPack) {
        realtimeListener?.onEEGFrame(pack)
    }

    abstract fun onTriggerStatusConfiguration(statusAllocationSize: Int)

    abstract fun getRecordingErrorData(): EEGStreamingErrorCounter

    protected fun notifyPacket(mbtEEGPacket: MbtEEGPacket) {
        eegCallback?.onNewEEG(mbtEEGPacket)
    }

    override fun dispose() {
        disposable.dispose()
    }

    /**
     * notify caller when a new packet has been created
     */
    interface EEGCallback {
        fun onNewEEG(eegPacket: MbtEEGPacket)
    }
}