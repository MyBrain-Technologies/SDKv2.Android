package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.ScanResult

// TODO: 09/11/2021 : move to separated class later
interface ScanResultListener {
    fun onScanResult(scanResult: ScanResult)
    fun onScanResultError(error: Throwable)
}

/**
 * the equivalent of MBTBLEBluetoothDelegate in ios SDK
 */
interface ConnectionListener {
    fun onDeviceConnectionStateChanged(isConnected: Boolean)
    fun onDeviceBondStateChanged(isBonded: Boolean)
    /**
     * Called when the headset has been connected after the services and characteristics exploration.
     */
    fun onDeviceReady()
    fun onConnectionError(error: Throwable)
    fun onScanFailed(errorCode: Int)
}