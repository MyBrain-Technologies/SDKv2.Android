package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.sdk.core.acquisition.EnumBluetoothProtocol
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.RawEEGSample2
import com.mybraintech.sdk.core.model.StreamingParams
import io.reactivex.Scheduler

class EEGSignalProcessingXon(
    streamingParams : StreamingParams,
    bleCallback: EEGCallback?,
    bleEEGFrameScheduler: Scheduler
) : EEGSignalProcessing(
    protocol = EnumBluetoothProtocol.BLE,
    isTriggerStatusEnabled = streamingParams.isTriggerStatusEnabled,
    isQualityCheckerEnabled = streamingParams.isQualityCheckerEnabled,
    callback = bleCallback,
    eegFrameScheduler = bleEEGFrameScheduler
) {
    override fun getDeviceType() = EnumMBTDevice.XON

    override fun getFrameIndex(eegFrame: ByteArray): Long {
        //        TODO("Not yet implemented")
        return -1
    }

    override fun isValidFrame(eegFrame: ByteArray): Boolean {
//        TODO("Not yet implemented")
        return true
    }

    override fun getNumberOfChannels(): Int {
        return 8
    }

    override fun decodeEEGData(eegFrame: ByteArray): List<RawEEGSample2> {
//        TODO("Not yet implemented")
        return emptyList()
    }
}