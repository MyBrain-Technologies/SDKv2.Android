package com.mybraintech.sdk.core.listener

interface SerialNumberChangedListener {
    fun onSerialNumberChanged(newSerialNumber: String)
    fun onSerialNumberError(errorMessage: String)
}