package com.mybraintech.sdk.core.acquisition

import com.mybraintech.sdk.core.listener.EEGRealtimeListener
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.TimedBLEFrame

/**
 * how to use : call [init] at start and call [terminate] to release occupied memory.
 */
interface RealtimeEEGExecutor {
    fun init(deviceType : EnumMBTDevice)
    fun onEEGFrame(eegFrame: TimedBLEFrame)
    fun setListener(eegRealtimeListener: EEGRealtimeListener)
    fun terminate()
}