package com.mybraintech.sdk

import com.mybraintech.sdk.core.LabStreamingLayer
import com.mybraintech.sdk.core.TestBench
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.*
import java.io.File
import java.io.InputStream

interface MbtClient {
    fun getDeviceType() : EnumMBTDevice
    fun getBleConnectionStatus(): BleConnectionStatus
    fun startScan(scanResultListener: ScanResultListener)
    fun stopScan()
    fun connect(mbtDevice: MbtDevice, connectionListener: ConnectionListener)
    fun disconnect()
    fun getBatteryLevel(batteryLevelListener: BatteryLevelListener)
    fun getDeviceInformation(deviceInformationListener: DeviceInformationListener)

    fun getStreamingState(sensorStatusListener: SensorStatusListener)
    fun startStreaming(streamingParams: StreamingParams)
    fun stopStreaming()
    fun isEEGEnabled(): Boolean

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

    @TestBench
    fun setSerialNumber(serialNumber: String, listener: SerialNumberChangedListener?)
    @TestBench
    fun setAudioName(audioName: String, listener: AudioNameListener?)
    @TestBench
    fun getDeviceSystemStatus(deviceSystemStatusListener: DeviceSystemStatusListener)
    @TestBench
    fun getAccelerometerConfig(accelerometerConfigListener : AccelerometerConfigListener)

    /**
     * @return true if firmware input stream is valid and preparation phase is ok
     */
    fun prepareDFU(firmware : InputStream) : Boolean

    /**
     * @return true if DFU is started successfully
     */
    fun startDFU(listener: DriverFirmwareUpgradeListener) : Boolean
}