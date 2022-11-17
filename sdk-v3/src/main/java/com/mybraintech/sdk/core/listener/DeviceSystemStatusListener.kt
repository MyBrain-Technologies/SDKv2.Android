package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.DeviceSystemStatus

interface DeviceSystemStatusListener {
    fun onDeviceSystemStatusFetched(deviceSystemStatus: DeviceSystemStatus)
    fun onDeviceSystemStatusError(error: String)
}