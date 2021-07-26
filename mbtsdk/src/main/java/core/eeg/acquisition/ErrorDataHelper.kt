package core.eeg.acquisition

import timber.log.Timber
import java.lang.Exception

object ErrorDataHelper {

    private const val ZERO_BYTE: Byte = 0x00

    /**
     * data do not contains OP code for Indus5 case
     */
    fun countZeroSample(data: ByteArray, channelNb: Int): Pair<Int, Int> {
        var zeroSample = 0;
        var zeroTime = 0;

        try {
            val sampleNb = (data.size - 2) / 2
            if (sampleNb % channelNb != 0) {
                Timber.e("invalid format frame: data size in bytes = %s", data.size)
            }
            val times = sampleNb / channelNb
            for (i in 0 until times) {
                var newZero = 0
                for (j in 0 until channelNb) {
                    val pos = (i * channelNb + j) * 2 + 2
                    if ((data[pos] == ZERO_BYTE) && (data[pos + 1] == ZERO_BYTE)) {
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