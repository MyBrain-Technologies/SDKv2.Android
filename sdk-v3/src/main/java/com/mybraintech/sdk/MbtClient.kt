package com.mybraintech.sdk

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import com.mybraintech.android.jnibrainbox.RelaxIndexSessionOutputData
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
    fun startScan(targetName:String,scanResultListener: ScanResultListener)
    fun stopScan()

    fun startScanAudio(targetName:String,scanResultListener: ScanResultListener?)
    fun stopScanAudio()

    fun connect(mbtDevice: MbtDevice, connectionListener: ConnectionListener, connectionMode: EnumBluetoothConnection = EnumBluetoothConnection.BLE)
    fun connectAudio(mbtDevice:MbtDevice,connectionListener: ConnectionListener)
    fun connectAudioViaBLE(connectionListener: ConnectionListener)
    fun disconnectAudio(mbtDevice: BluetoothDevice?)
    fun disconnectAudioViaBLE()
    fun rebootHeadset()
    fun getA2DP(): BluetoothA2dp?
    fun initA2DP()
    fun disconnect()
    fun removeBondBle()
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
    fun eegRelaxingIndex(eegs:EEGRecordedDatas): Float
    fun eggStartRelaxingIndexSession(data: EEGCalibrateResult)
    fun eggEngRelaxingIndexSession():RelaxIndexSessionOutputData?

    @TestBench
    fun setSerialNumber(serialNumber: String, listener: SerialNumberChangedListener?)
    @TestBench
    fun setAudioName(audioName: String, listener: AudioNameListener?)
    @TestBench
    fun getDeviceSystemStatus(deviceSystemStatusListener: DeviceSystemStatusListener)
    @TestBench
    fun getAccelerometerConfig(accelerometerConfigListener : AccelerometerConfigListener)
    fun computeStatistics(threshold:Float, snrValues:Array<Float>): HashMap<String, Float>?
}