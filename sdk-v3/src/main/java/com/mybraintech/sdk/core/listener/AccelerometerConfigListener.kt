package com.mybraintech.sdk.core.listener


interface AccelerometerConfigListener {
    fun onAccelerometerConfigFetched(sampleRate: Int)
    fun onAccelerometerConfigError(error: String)
}