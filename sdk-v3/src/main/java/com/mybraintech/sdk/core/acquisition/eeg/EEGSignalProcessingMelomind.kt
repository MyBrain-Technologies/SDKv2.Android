package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.sdk.core.acquisition.EnumBluetoothProtocol
import com.mybraintech.sdk.core.acquisition.IndexReader
import com.mybraintech.sdk.core.model.*
import com.mybraintech.sdk.util.NumericalUtils
import io.reactivex.Scheduler
import timber.log.Timber

class EEGSignalProcessingMelomind(
    streamingParams: StreamingParams,
    eegCallback: EEGCallback,
    bleFrameScheduler: Scheduler
) :
    EEGSignalProcessing(
        protocol = EnumBluetoothProtocol.BLE,
        isTriggerStatusEnabled = streamingParams.isTriggerStatusEnabled,
        isQualityCheckerEnabled = streamingParams.isQualityCheckerEnabled,
        callback = eegCallback,
        eegFrameScheduler = bleFrameScheduler
    ) {

    override fun getDeviceType(): EnumMBTDevice {
        return EnumMBTDevice.MELOMIND
    }

    override fun getFrameIndex(eegFrame: ByteArray): Long {
        return IndexReader.decodeIndex(eegFrame)
    }

    override fun isValidFrame(eegFrame: ByteArray): Boolean {
        /**
         * eg: indus 5 eeg frame can be 43 bytes = 2 bytes index + 1 byte trigger + 5 times * 4 channels * 2 bytes
         */
        val oneTime = SIGNAL_ALLOC * getNumberOfChannels()
        return ((eegFrame.size - headerAlloc) % oneTime == 0)
    }

    /**
     * F3 F4 AF3 AF4
     */
    override fun getNumberOfChannels(): Int = 2

    override fun decodeEEGData(eegFrame: ByteArray): List<RawEEGSample2> {
        val list = mutableListOf<RawEEGSample2>()
        val hasStatus = (statusAlloc != 0)

//        Timber.v("decodeEEGData hasStatus:$hasStatus indexAlloc:$indexAlloc headerAlloc:$headerAlloc")
        val triggerBytes = if (hasStatus) {
            eegFrame.copyOfRange(indexAlloc, headerAlloc)
        } else {
            ByteArray(0)
        }
//        Timber.v("decodeEEGData triggerBytes:${triggerBytes.size}")
        val numberOfTimes = getNumberOfTimes(eegFrame)
//        Timber.v("decodeEEGData numberOfTimes:${numberOfTimes}")
        for (i in 0 until getNumberOfTimes(eegFrame)) {
            val status = if (hasStatus) {
                getTriggerStatus(i, triggerBytes)
            } else {
                Float.NaN
            }
            list.add(RawEEGSample2(getEEGSample(i, eegFrame), status))
        }
//        Timber.v("decodeEEGData final list:${list}")
        return list
    }

    private fun getTriggerStatus(pos: Int, triggerStatusBytes: ByteArray): Float {
        try {
            val byte = triggerStatusBytes[pos / 8] //one byte has 8 bits
            val bitPos = pos % 8
            return NumericalUtils.isBitSet(byte, bitPos)
        } catch (e: Exception) {
            Timber.e(e)
            Timber.e(
                "pos = $pos, statusBytes = ${
                    NumericalUtils.bytesToShortString(
                        triggerStatusBytes
                    )
                }"
            )
            return Float.NaN
        }
    }

    private fun getEEGSample(pos: Int, eegFrame: ByteArray): List<ByteArray>? {

//        Timber.v("getEEGSample pos:${pos} eegFrame:${eegFrame.size}")
        try {
            val list = mutableListOf<ByteArray>()
            val startIndex = headerAlloc + pos * SIGNAL_ALLOC * getNumberOfChannels()
//            Timber.v("getEEGSample startIndex:${startIndex} headerAlloc:${headerAlloc} SIGNAL_ALLOC:$SIGNAL_ALLOC ")
            for (i in 0 until getNumberOfChannels()) {
                list.add(
                    eegFrame.copyOfRange(
                        startIndex + i * 2,
                        startIndex + i * 2 + SIGNAL_ALLOC
                    )
                )
            }
            return list
        } catch (e: Exception) {
            Timber.e(e)
            return null
        }
    }

}
fun Byte.toString():String {
    return this.toString(16)
}