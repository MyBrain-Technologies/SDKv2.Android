package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.AccelerometerConfig


interface AccelerometerConfigListener {
    fun onAccelerometerConfigFetched(config: AccelerometerConfig)
    fun onAccelerometerConfigError(error: String)
}