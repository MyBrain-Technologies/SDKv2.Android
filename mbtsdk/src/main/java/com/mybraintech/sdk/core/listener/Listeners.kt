package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.ScanResult

// TODO: 09/11/2021 : move to separated class later
interface ScanResultListener {
    fun onScanResult(scanResult: ScanResult)
    fun onScanResultError(error: Throwable)
}

interface ConnectionListener {
    fun onDeviceConnectionStateChanged(isConnected: Boolean)
    fun onDeviceBondStateChanged(isBonded: Boolean)
    fun onConnectionError(error: Throwable)
}