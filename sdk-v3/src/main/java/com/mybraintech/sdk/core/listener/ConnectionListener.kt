package com.mybraintech.sdk.core.listener

/**
 * the equivalent of MBTBLEBluetoothDelegate in ios SDK
 */
interface ConnectionListener {

    /**
     * BLE services are discovered
     */
    fun onServiceDiscovered()

    /**
     * bonding process is required, a pairing request may be popped up
     */
    fun onBondingRequired()

    /**
     * a pairing request has been popped, user must validate this request to continue.
     * A timeout will be triggered if user does not validate in time
     */
    fun onParingRequest()

    fun onBonded()

    fun onBondingFailed()

    /**
     * the connection process has finished
     */
    fun onDeviceReady()

    fun onDeviceDisconnected()

    fun onConnectionError(error: Throwable)
}