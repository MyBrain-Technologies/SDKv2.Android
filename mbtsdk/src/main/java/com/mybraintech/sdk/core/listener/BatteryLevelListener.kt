package com.mybraintech.sdk.core.listener

import androidx.annotation.FloatRange

interface BatteryLevelListener {
    fun onBatteryLevel(@FloatRange(from = 0.0, to = 100.0) float: Float)
    fun onBatteryLevelError(error: Throwable)
}