package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.RawEEGSample2

interface EEGFrameDecodeInterface {
    /**
     * @param eegFrame eeg frame starting with index frame number
     */
    fun decodeEEGData(eegFrame: ByteArray): List<RawEEGSample2>
}