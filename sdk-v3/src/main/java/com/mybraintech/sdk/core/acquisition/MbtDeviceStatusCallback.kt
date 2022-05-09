package com.mybraintech.sdk.core.acquisition

interface MbtDeviceStatusCallback {
    fun onEEGStatusChange(isEnabled: Boolean)
    fun onIMSStatusChange(isEnabled: Boolean)
    fun onDeviceStatusError(error: Throwable)
}