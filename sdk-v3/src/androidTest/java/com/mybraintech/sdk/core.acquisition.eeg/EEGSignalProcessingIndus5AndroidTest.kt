package com.mybraintech.sdk.core.acquisition.eeg

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class EEGSignalProcessingIndus5AndroidTest {

    private val eegAcquisierIndus5_1 = EEGSignalProcessingIndus5(250, 1, true)
    private val eegAcquisierIndus5_2 = EEGSignalProcessingIndus5(250, 0, false)

    private val correctSize = 43
    private val correctTimePerFrame = 5
    private val correctNbChannels = 4

    @Test
    fun test_onEEGFrame() {
        println("test_onEEGFrame")
        val eegFrame: ByteArray = ByteArray(correctSize) {
            (it + 29).toByte() //trigger byte is 31 = 0001 1111
        }
        eegAcquisierIndus5_1.onEEGStatusChange(true)
        for (i in 1..100) {
            eegAcquisierIndus5_1.onEEGFrame(eegFrame)
            println("go sleep 10ms")
            Thread.sleep(10)
        }
        Thread.sleep(1000)
        println("done")
    }

}