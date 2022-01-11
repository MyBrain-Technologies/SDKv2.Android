package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.sdk.core.model.RawEEGSample2
import com.mybraintech.sdk.util.NumericalUtils
import timber.log.Timber

class EEGSignalProcessingIndus5(
    sampleRate: Int,
    statusAllocationSize: Int,
    isQualityCheckerEnabled: Boolean
) :
    EEGSignalProcessing(
        sampleRate,
        EnumBluetoothProtocol.BLE,
        statusAllocationSize,
        isQualityCheckerEnabled
    ) {

    override fun getFrameIndex(eegFrame: ByteArray): Long {
        return Indus5Utils.getIndex(eegFrame)
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
    override fun getNumberOfChannels(): Int = 4

    override fun getEEGData(eegFrame: ByteArray): List<RawEEGSample2> {
        val list = mutableListOf<RawEEGSample2>()
        val triggerBytes = if (hasStatus) {
            eegFrame.copyOfRange(indexAlloc, headerAlloc)
        } else {
            ByteArray(0)
        }
        for (i in 0 until getNumberOfTimes(eegFrame)) {
            val status = if (hasStatus) {
                getStatus(i, triggerBytes)
            } else {
                Float.NaN
            }
            list.add(RawEEGSample2(getEEGSample(i, eegFrame), status))
        }
        return list
    }

    private fun getStatus(pos: Int, statusBytes: ByteArray): Float {
        try {
            val byte = statusBytes[pos / 8] //one byte has 8 bits
            val bitPos = pos % 8
            return NumericalUtils.isBitSet(byte, bitPos)
        } catch (e: Exception) {
            Timber.e(e)
            Timber.e("pos = $pos, statusBytes = ${NumericalUtils.bytesToShortString(statusBytes)}")
            return Float.NaN
        }
    }

    private fun getEEGSample(pos: Int, eegFrame: ByteArray): List<ByteArray>? {
        try {
            val list = mutableListOf<ByteArray>()
            val startIndex = headerAlloc + pos * SIGNAL_ALLOC * getNumberOfChannels()
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