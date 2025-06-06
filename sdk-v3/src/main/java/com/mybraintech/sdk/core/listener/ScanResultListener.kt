package com.mybraintech.sdk.core.listener

import android.bluetooth.BluetoothDevice
import com.mybraintech.sdk.core.model.MbtDevice
import com.mybraintech.sdk.core.model.ScanResult

interface ScanResultListener {

    /**
     * contains
     */
    fun onMbtDevices(mbtDevices: List<MbtDevice>)

    /**
     *
     */
    fun onOtherDevices(otherDevices: List<BluetoothDevice>)

    fun onScanError(error: Throwable)
}
