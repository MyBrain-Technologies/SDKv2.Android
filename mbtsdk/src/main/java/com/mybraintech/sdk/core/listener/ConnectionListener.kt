package com.mybraintech.sdk.core.listener

/**
 * the equivalent of MBTBLEBluetoothDelegate in ios SDK
 */
interface ConnectionListener {
    fun onServiceDiscovered()
    fun onDeviceDisconnected()
    fun onDeviceBondStateChanged(isBonded: Boolean)

    /**
     * Called when the headset has been connected after the services and characteristics exploration.
     */
    fun onDeviceReady()
    fun onConnectionError(error: Throwable)
}