package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.sdk.core.model.*
import io.reactivex.Scheduler

class EEGSignalProcessingHyperion(
    streamingParams: StreamingParams,
    eegCallback: EEGCallback,
    bleFrameScheduler: Scheduler
) :
    EEGSignalProcessingIndus5(streamingParams, eegCallback, bleFrameScheduler) {

    override fun getDeviceType(): EnumMBTDevice {
        return EnumMBTDevice.HYPERION
    }
}