package com.mybraintech.sdk.core.model

import android.bluetooth.BluetoothDevice

// TODO: 09/11/2021 : move to separated class later
class DeviceInformation {
    var name: String? = null
}

class ScanResult(val devices: List<MbtDevice>) {
}

class MbtDevice(val bluetoothDevice: BluetoothDevice)

data class BleConnectionStatus(
    /**
     * is not null if there is a connected MbtDevice.
     */
    val mbtDevice: MbtDevice?,

    /**
     * true if there is a connected MbtDevice and connection process is finished.
     *
     * false if there is no connected MbtDevice or connection process is not finished.
     */
    val isConnectionEstablished: Boolean
)