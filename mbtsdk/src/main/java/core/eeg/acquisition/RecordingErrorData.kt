package core.eeg.acquisition

import timber.log.Timber

/**
 * this class is to debug data loss bug, it may be removed in release version
 */
class RecordingErrorData {

    companion object {
        @JvmStatic
        var default: RecordingErrorData = RecordingErrorData()
    }

    var startingIndex: Long = -1
    var currentIndex: Long = -1

    /**
     * counter of missing samples
     */
    var missingFrame: Long = 0

    /**
     * all channels signal of a time are zero counter
     */
    var zeroTimeNumber: Long = 0

    /**
     * counter of zero value of all channels
     */
    var zeroSampleNumber: Long = 0

    fun increaseMissingEegFrame(value: Int) {
        missingFrame += value
    }

    fun increaseZeroTimeCounter(value: Int) {
        zeroTimeNumber += value
    }

    fun increaseZeroSampleCounter(value: Int) {
        zeroSampleNumber += value
    }

    fun getMissingPercent(): Float {
        if (startingIndex == -1L || currentIndex == -1L) {
            return 0f
        }
        return missingFrame.toFloat() * 100 / (currentIndex - startingIndex + 1)
    }

    @JvmOverloads
    fun resetData() {
        Timber.d("resetData")
        missingFrame = 0
        zeroSampleNumber = 0
        zeroTimeNumber = 0
        startingIndex = -1
        currentIndex = -1
    }

    fun clone(): RecordingErrorData {
        val clone = RecordingErrorData()
        clone.missingFrame = missingFrame
        clone.zeroTimeNumber = zeroTimeNumber
        clone.zeroSampleNumber = zeroSampleNumber
        clone.startingIndex = startingIndex
        clone.currentIndex = currentIndex
        return clone
    }
}