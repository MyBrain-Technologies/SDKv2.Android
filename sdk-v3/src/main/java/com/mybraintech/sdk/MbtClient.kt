package com.mybraintech.sdk

import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.*

interface MbtClient {
    fun getDeviceType() : EnumMBTDevice
    fun getBleConnectionStatus(): BleConnectionStatus
    fun startScan(scanResultListener: ScanResultListener)
    fun stopScan()
    fun connect(mbtDevice: MbtDevice, connectionListener: ConnectionListener)
    fun disconnect()
    fun getBatteryLevel(batteryLevelListener: BatteryLevelListener)
    fun getDeviceInformation(deviceInformationListener: DeviceInformationListener)
    fun startEEG(eegParams: EEGParams, eegListener: EEGListener)
    fun stopEEG()
    fun startEEGRecording(recordingOption: RecordingOption, recordingListener: RecordingListener)
    fun stopEEGRecording()
    fun isEEGEnabled(): Boolean
    fun isRecordingEnabled(): Boolean
    fun getRecordingBufferSize(): Int
    fun getDataLossPercent(): Float
}