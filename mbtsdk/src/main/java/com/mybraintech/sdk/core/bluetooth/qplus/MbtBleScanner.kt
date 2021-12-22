package com.mybraintech.sdk.core.bluetooth.qplus

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanSettings

class MbtBleScanner {
    private val scanner: BluetoothLeScannerCompat = BluetoothLeScannerCompat.getScanner()
    lateinit var scanCallback: ScanCallback

    fun startScan(scanCallback: ScanCallback) {
        this.scanCallback = scanCallback
        val settings: ScanSettings = ScanSettings.Builder()
            .setLegacy(false)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(5000)
            .setUseHardwareBatchingIfSupported(true)
            .build()
        scanner.startScan(null, settings, scanCallback)
    }

    fun stopScan() {
        scanner.stopScan(scanCallback)
    }
}