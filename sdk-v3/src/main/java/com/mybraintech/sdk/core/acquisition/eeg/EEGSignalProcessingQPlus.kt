package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.android.jnibrainbox.BrainBoxVersion
import com.mybraintech.sdk.core.acquisition.EnumBluetoothProtocol
import com.mybraintech.sdk.core.acquisition.Indus5Utils
import com.mybraintech.sdk.core.listener.EEGListener
import com.mybraintech.sdk.core.model.*
import com.mybraintech.sdk.util.NumericalUtils
import timber.log.Timber

class EEGSignalProcessingQPlus(streamingParams: StreamingParams, eegListener: EEGListener) :
    EEGSignalProcessing(
        sampleRate = streamingParams.eegSampleRate,
        protocol = EnumBluetoothProtocol.BLE,
        isTriggerStatusEnabled = streamingParams.isTriggerStatusEnabled,
        isQualityCheckerEnabled = streamingParams.isQualityCheckerEnabled,
        eegListener = eegListener
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
        val hasStatus = (statusAlloc != 0)
        val triggerBytes = if (hasStatus) {
            eegFrame.copyOfRange(indexAlloc, headerAlloc)
        } else {
            ByteArray(0)
        }
        for (i in 0 until getNumberOfTimes(eegFrame)) {
            val status = if (hasStatus) {
                getTriggerStatus(i, triggerBytes)
            } else {
                Float.NaN
            }
            list.add(RawEEGSample2(getEEGSample(i, eegFrame), status))
        }
        return list
    }

    private fun getTriggerStatus(pos: Int, triggerStatusBytes: ByteArray): Float {
        try {
            val byte = triggerStatusBytes[pos / 8] //one byte has 8 bits
            val bitPos = pos % 8
            return NumericalUtils.isBitSet(byte, bitPos)
        } catch (e: Exception) {
            Timber.e(e)
            Timber.e("pos = $pos, statusBytes = ${NumericalUtils.bytesToShortString(triggerStatusBytes)}")
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

    override fun createKwak(recordingOption: RecordingOption): Kwak {
        return Kwak().apply {
            context = recordingOption.context
            header = KwakHeader.getQPlusHeader().apply {
                deviceInfo = recordingOption.deviceInformation
                setRecordingNb(recordingOption.recordingNb)
            }
            recording.recordID = recordingOption.recordId
            recording.recordingType.recordType = recordingOption.recordingType
            if (isQualityCheckerEnabled) {
                recording.recordingType.spVersion = BrainBoxVersion.getVersion()
            } else {
                recording.recordingType.spVersion = "0.0.0"
            }
        }
    }
}