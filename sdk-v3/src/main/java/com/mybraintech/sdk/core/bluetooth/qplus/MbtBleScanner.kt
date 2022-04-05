package com.mybraintech.sdk.core.bluetooth.qplus

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback

class MbtBleScanner {
    private val scanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner

    lateinit var scanCallback: ScanCallback

    fun startScan(scanCallback: ScanCallback) {
        this.scanCallback = scanCallback
        scanner.startScan(scanCallback)
    }

    fun stopScan() {
        scanner.stopScan(scanCallback)
    }
}