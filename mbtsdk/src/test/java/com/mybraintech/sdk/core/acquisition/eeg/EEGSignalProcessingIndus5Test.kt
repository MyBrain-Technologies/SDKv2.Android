package com.mybraintech.sdk.core.acquisition.eeg

import org.junit.Assert.assertEquals
import org.junit.Test

class EEGSignalProcessingIndus5Test {

    private val eegAcquisierIndus5_1 = EEGSignalProcessingIndus5(250, 1, true)
    private val eegAcquisierIndus5_2 = EEGSignalProcessingIndus5(250, 0, false)

    private val correctSize = 43
    private val correctTimePerFrame = 5
    private val correctNbChannels = 4

    @Test
    fun test_isValidFrame() {
        var input = byteArrayOf()
        for (i in 1..correctSize) {
            input += 0x00
        }
        assertEquals(true, eegAcquisierIndus5_1.isValidFrame(input))
        assertEquals(false, eegAcquisierIndus5_2.isValidFrame(input))
    }

    @Test
    fun test_getEEGData() {
        val input: ByteArray = ByteArray(correctSize) {
            (it + 29).toByte() //index bytes are 29 30; trigger byte is 31 = 0001 1111
        }
        val sample = eegAcquisierIndus5_1.getEEGData(input)
        assertEquals(correctTimePerFrame, sample.size)
        assertEquals(correctNbChannels, sample[0].eegData?.size)
    }

}