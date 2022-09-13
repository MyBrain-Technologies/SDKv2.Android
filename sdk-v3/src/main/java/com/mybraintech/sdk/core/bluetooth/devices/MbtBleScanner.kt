package com.mybraintech.sdk.core.bluetooth.devices

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback

class MbtBleScanner {
    private val scanner: BluetoothLeScanner =
        BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner

    lateinit var scanCallback: ScanCallback

    /**
     * Important : after few days of work with Melomind, we find out that the function <pre>startScan(scanCallback)}</pre>
     * perform so much better in compare with function startScan(scanFilters, scanSettings, scanCallback).
     *
     * If you need to filter result, please do it in function handleScanResults(scanResults)
     *
     * @see com.mybraintech.sdk.core.bluetooth.devices.BaseMbtDevice.handleScanResults
     * @see android.bluetooth.le.BluetoothLeScanner.startScan(android.bluetooth.le.ScanCallback)
     * @see android.bluetooth.le.BluetoothLeScanner.startScan(java.util.List<android.bluetooth.le.ScanFilter>, android.bluetooth.le.ScanSettings, android.bluetooth.le.ScanCallback)
     */
    fun startScan(scanCallback: ScanCallback) {
        this.scanCallback = scanCallback
        scanner.startScan(scanCallback)
    }

    fun stopScan() {
        scanner.stopScan(scanCallback)
    }
}