package com.mybraintech.sdk.core.model

import timber.log.Timber

/**
 * copy from old sdk
 * this class is to debug data loss bug, it may be removed in release version
 */
class EEGStreamingErrorCounter {

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

    fun increaseMissingEegFrame(value: Long) {
        missingFrame += value
    }

    fun increaseZeroTimeCounter(value: Long) {
        zeroTimeNumber += value
    }

    fun increaseZeroSampleCounter(value: Long) {
        zeroSampleNumber += value
    }

    fun getMissingPercent(): Float {
        if (startingIndex == -1L || currentIndex == -1L) {
            return 0f
        }
        return missingFrame.toFloat() * 100 / (currentIndex - startingIndex + 1)
    }

    @Suppress("unused")
    fun resetData() {
        Timber.d("resetData")
        missingFrame = 0
        zeroSampleNumber = 0
        zeroTimeNumber = 0
        startingIndex = -1
        currentIndex = -1
    }

    @Suppress("unused")
    fun clone(): EEGStreamingErrorCounter {
        val clone = EEGStreamingErrorCounter()
        clone.missingFrame = missingFrame
        clone.zeroTimeNumber = zeroTimeNumber
        clone.zeroSampleNumber = zeroSampleNumber
        clone.startingIndex = startingIndex
        clone.currentIndex = currentIndex
        return clone
    }
}