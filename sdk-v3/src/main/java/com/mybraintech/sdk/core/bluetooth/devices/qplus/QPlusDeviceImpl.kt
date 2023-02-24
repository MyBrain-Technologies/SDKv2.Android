package com.mybraintech.sdk.core.bluetooth.devices.qplus

import android.bluetooth.le.ScanResult
import android.content.Context
import com.mybraintech.sdk.core.bluetooth.MbtBleUtils
import com.mybraintech.sdk.core.model.MbtDevice
import timber.log.Timber


class QPlusDeviceImpl(ctx: Context) : Indus5DeviceImpl(ctx) {

    override fun handleScanResults(results: List<ScanResult>) {
        val devices = results.map { it.device }
        val qplusDevices = devices.filter(MbtBleUtils::isQPlus)
        if (qplusDevices.isNotEmpty()) {
            Timber.d("found QPlus devices : number = ${qplusDevices.size}")
            scanResultListener.onMbtDevices(qplusDevices.map { MbtDevice(it) })
        }
        val otherDevices = devices.filter { !MbtBleUtils.isQPlus(it) }
        if (otherDevices.isNotEmpty()) {
            Timber.d("found other devices : number = ${otherDevices.size}")
            scanResultListener.onOtherDevices(otherDevices)
        }
    }
}
