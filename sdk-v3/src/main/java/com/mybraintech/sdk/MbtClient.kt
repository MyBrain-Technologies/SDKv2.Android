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
    fun startStreaming(streamingParams: StreamingParams)
    fun stopStreaming()
    fun setEEGRealtimeListener(eegRealtimeListener: EEGRealtimeListener)
    fun setEEGListener(eegListener: EEGListener)
    fun setAccelerometerListener(accelerometerListener: AccelerometerListener)
    fun startRecording(recordingOption: RecordingOption, recordingListener: RecordingListener)
    fun stopRecording()
    fun isEEGEnabled(): Boolean
    fun isRecordingEnabled(): Boolean
    fun getRecordingBufferSize(): Int
    fun getDataLossPercent(): Float
}