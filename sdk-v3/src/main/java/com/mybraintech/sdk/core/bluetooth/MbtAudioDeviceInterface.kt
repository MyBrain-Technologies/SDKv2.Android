package com.mybraintech.sdk.core.bluetooth

import android.bluetooth.BluetoothDevice

interface MbtAudioDeviceInterface {
    fun onMbtAudioDeviceFound(device: BluetoothDevice, action: String, state: Int)
}