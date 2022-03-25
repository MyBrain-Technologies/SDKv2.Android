package com.mybraintech.sdk.core.bluetooth.devices

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanSettings

class MbtBleScanner {
    private val scanner: BluetoothLeScannerCompat = BluetoothLeScannerCompat.getScanner()
    lateinit var scanCallback: ScanCallback

    fun startScan(filters: List<ScanFilter>?, scanCallback: ScanCallback) {
        this.scanCallback = scanCallback
        val settings: ScanSettings = ScanSettings.Builder()
            .setLegacy(false)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(150)
            .setUseHardwareBatchingIfSupported(true)
            .build()
        scanner.startScan(filters, settings, scanCallback)
    }

    fun stopScan() {
        scanner.stopScan(scanCallback)
    }
}