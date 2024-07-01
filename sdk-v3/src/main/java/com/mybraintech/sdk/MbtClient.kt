package com.mybraintech.sdk

import com.mybraintech.sdk.core.LabStreamingLayer
import com.mybraintech.sdk.core.ResearchStudy
import com.mybraintech.sdk.core.TestBench
import com.mybraintech.sdk.core.acquisition.EEGCalibrateResult
import com.mybraintech.sdk.core.acquisition.EEGRecordedDatas
import com.mybraintech.sdk.core.bluetooth.devices.EnumBluetoothConnection
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.*

interface MbtClient {
    fun getDeviceType() : EnumMBTDevice
    fun getBleConnectionStatus(): BleConnectionStatus
    fun startScan(scanResultListener: ScanResultListener)
    fun stopScan()
    fun connect(mbtDevice: MbtDevice, connectionListener: ConnectionListener, connectionMode: EnumBluetoothConnection = EnumBluetoothConnection.BLE)
    fun disconnect()
    fun getBatteryLevel(batteryLevelListener: BatteryLevelListener)
    fun getDeviceInformation(deviceInformationListener: DeviceInformationListener)

    fun getStreamingState(sensorStatusListener: SensorStatusListener)
    fun startStreaming(streamingParams: StreamingParams)
    fun stopStreaming()
    fun isEEGEnabled(): Boolean

    @ResearchStudy
    fun getEEGFilterConfig(listener: EEGFilterConfigListener)

    @LabStreamingLayer
    fun setEEGRealtimeListener(eegRealtimeListener: EEGRealtimeListener)

    fun setEEGListener(eegListener: EEGListener)
    fun setAccelerometerListener(accelerometerListener: AccelerometerListener)
    fun startRecording(recordingOption: RecordingOption, recordingListener: RecordingListener)
    fun stopRecording()

    /**
     * @param trim (in seconds) allow to trim the recording size
     */
    fun stopRecording(trim: Int)

    fun isRecordingEnabled(): Boolean
    fun getRecordingBufferSize(): Int
    fun getDataLossPercent(): Float

    fun eegCalibration(data: EEGRecordedDatas): EEGCalibrateResult
    fun eegRelaxingIndex(data: EEGCalibrateResult,eegs:EEGRecordedDatas): Float
    @TestBench
    fun setSerialNumber(serialNumber: String, listener: SerialNumberChangedListener?)
    @TestBench
    fun setAudioName(audioName: String, listener: AudioNameListener?)
    @TestBench
    fun getDeviceSystemStatus(deviceSystemStatusListener: DeviceSystemStatusListener)
    @TestBench
    fun getAccelerometerConfig(accelerometerConfigListener : AccelerometerConfigListener)
}