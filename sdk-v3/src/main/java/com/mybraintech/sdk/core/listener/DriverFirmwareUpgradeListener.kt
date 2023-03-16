package com.mybraintech.sdk.core.listener

interface DriverFirmwareUpgradeListener {
    fun onDFUInitialized()
    fun onOTAResult(isOk: Boolean, msg: String = "")
    fun onDFUError(error: String)
    fun onDFUAborted()
}