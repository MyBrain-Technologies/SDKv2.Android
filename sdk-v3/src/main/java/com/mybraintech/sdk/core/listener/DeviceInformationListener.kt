package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.DeviceInformation

interface DeviceInformationListener {
    fun onDeviceInformation(deviceInformation: DeviceInformation)
    fun onDeviceInformationError(error: Throwable)
}