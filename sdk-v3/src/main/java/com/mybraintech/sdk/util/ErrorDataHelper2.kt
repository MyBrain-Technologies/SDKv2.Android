package com.mybraintech.sdk.util

import timber.log.Timber
import java.lang.Exception

object ErrorDataHelper2 {

    private const val ZERO_BYTE: Byte = 0x00

    /**
     * data do not contains OP code for Indus5 case
     */
    fun countZeroSample(eegData: ByteArray, channelNb: Int): Pair<Int, Int> {
        var zeroSample = 0;
        var zeroTime = 0;

        try {
            val sampleNb = eegData.size / 2
            if (sampleNb % channelNb != 0) {
                Timber.e("invalid format frame: data size in bytes = %s", eegData.size)
            }
            val times = sampleNb / channelNb
            for (i in 0 until times) {
                var newZero = 0
                for (j in 0 until channelNb) {
                    val pos = (i * channelNb + j) * 2
                    if ((eegData[pos] == ZERO_BYTE) && (eegData[pos + 1] == ZERO_BYTE)) {
                        newZero++
                    }
                }
                zeroSample += newZero
                if (newZero == channelNb) { //all the channels are 0
                    zeroTime++
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

        return Pair(zeroTime, zeroSample);
    }
}