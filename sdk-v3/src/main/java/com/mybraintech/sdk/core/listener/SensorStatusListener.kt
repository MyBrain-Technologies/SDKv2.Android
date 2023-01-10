package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.Indus5SensorStatus

interface SensorStatusListener {
    fun onSensorStatusFetched(sensorStatus: Indus5SensorStatus)
    fun onSensorStatusError(error: String)
}