package com.mybraintech.sdk.core.bluetooth.devices.hyperion

import android.bluetooth.le.ScanResult
import android.content.Context
import com.mybraintech.sdk.core.bluetooth.MbtBleUtils
import com.mybraintech.sdk.core.bluetooth.devices.qplus.Indus5DeviceImpl
import com.mybraintech.sdk.core.model.MbtDevice
import timber.log.Timber

class HyperionDeviceImpl(ctx: Context) : Indus5DeviceImpl(ctx) {

    override fun handleScanResults(results: List<ScanResult>) {
        val devices = results.map { it.device }
        val hyperions = devices.filter(MbtBleUtils::isHyperion)
        if (hyperions.isNotEmpty()) {
            Timber.d("found Hyperion devices : number = ${hyperions.size}")
            scanResultListener.onMbtDevices(hyperions.map { MbtDevice(it) })
        }
        val otherDevices = devices.filter { !MbtBleUtils.isHyperion(it) }
        if (otherDevices.isNotEmpty()) {
            Timber.d("found other devices : number = ${otherDevices.size}")
            scanResultListener.onOtherDevices(otherDevices)
        }
    }
}