package com.mybraintech.sdk

import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.MbtDevice

interface MbtClient {
    fun getDeviceType() : EnumMBTDevice
    fun getBleConnectionStatus(): BleConnectionStatus
    fun startScan(scanResultListener: ScanResultListener)
    fun stopScan()
    fun connect(mbtDevice: MbtDevice, connectionListener: ConnectionListener)
    fun disconnect()
    fun getBatteryLevel(batteryLevelListener: BatteryLevelListener)
    fun getDeviceInformation(deviceInformationListener: DeviceInformationListener)
    fun startEEG(eegListener: EEGListener)
    fun stopEEG()
}