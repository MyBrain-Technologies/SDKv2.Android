package com.mybraintech.sdk.core.listener

import android.bluetooth.BluetoothDevice
import com.mybraintech.sdk.core.model.MBTErrorCode

/**
 * the equivalent of MBTBLEBluetoothDelegate in ios SDK
 */
interface ConnectionListener {
    fun onServiceDiscovered(message:String)

    fun onBondingRequired(device: BluetoothDevice)
    fun onBonded(device: BluetoothDevice)
    fun onBondingFailed(device: BluetoothDevice)

    /**
     * Called when the headset has been connected after the services and characteristics exploration.
     */
    fun onDeviceReady(message:String)// last callback called when all device ready

    fun onDeviceDisconnected()
    fun onAudioDisconnected()

    fun onConnectionError(error: Throwable,errorCode: MBTErrorCode)

}
