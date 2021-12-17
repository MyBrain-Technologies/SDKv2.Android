package com.mybraintech.sdk.core.listener

import android.bluetooth.BluetoothDevice

/**
 * the equivalent of MBTBLEBluetoothDelegate in ios SDK
 */
interface ConnectionListener {
    fun onServiceDiscovered()

    fun onBondingRequired(device: BluetoothDevice)
    fun onBonded(device: BluetoothDevice)
    fun onBondingFailed(device: BluetoothDevice)

    /**
     * Called when the headset has been connected after the services and characteristics exploration.
     */
    fun onDeviceReady()

    fun onDeviceDisconnected()

    fun onConnectionError(error: Throwable)
}