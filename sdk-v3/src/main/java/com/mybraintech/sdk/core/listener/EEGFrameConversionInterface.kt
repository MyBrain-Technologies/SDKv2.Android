package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.RawEEGSample2

interface EEGFrameConversionInterface {
    /**
     * @param eegFrame eeg frame starting with index frame number
     */
    fun getEEGData(eegFrame: ByteArray): List<RawEEGSample2>
}