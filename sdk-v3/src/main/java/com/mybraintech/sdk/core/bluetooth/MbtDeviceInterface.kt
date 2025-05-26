package com.mybraintech.sdk.core.bluetooth

import android.bluetooth.BluetoothDevice
import com.mybraintech.sdk.core.acquisition.MbtDeviceStatusCallback
import com.mybraintech.sdk.core.bluetooth.devices.EnumBluetoothConnection
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.MbtDevice
import com.mybraintech.sdk.core.model.StreamingParams


interface MbtDeviceInterface {

    //----------------------------------------------------------------------------
    // scanning + connection
    //----------------------------------------------------------------------------
    fun startScan(targetName:String,scanResultListener: ScanResultListener)
    fun stopScan()

    fun startScanAudio(targetName:String,scanResultListener: ScanResultListener?)
    fun stopScanAudio()

    fun connectMbt(mbtDevice: MbtDevice, connectionListener: ConnectionListener, connectionMode: EnumBluetoothConnection)
    fun connectAudio(mbtDevice: MbtDevice,connectionListener: ConnectionListener)
    fun disconnectMbt()
    fun removeBondMbt()
    fun disconnectAudio(mbtDevice: BluetoothDevice?)

    //----------------------------------------------------------------------------
    // device
    //----------------------------------------------------------------------------
    fun getBleConnectionStatus(): BleConnectionStatus
    fun getSensorStatuses(sensorStatusListener: SensorStatusListener)

    //----------------------------------------------------------------------------
    // battery
    //----------------------------------------------------------------------------
    fun getBatteryLevel(batteryLevelListener: BatteryLevelListener)

    fun getDeviceInformation(deviceInformationListener: DeviceInformationListener)

    //----------------------------------------------------------------------------
    // MARK: streaming
    //----------------------------------------------------------------------------
    fun enableSensors(streamingParams: StreamingParams, dataReceiver: MbtDataReceiver, deviceStatusCallback: MbtDeviceStatusCallback)
    fun disableSensors()
    fun isEEGEnabled() : Boolean

    //----------------------------------------------------------------------------
    // MARK: only for Test Bench
    //----------------------------------------------------------------------------
    fun setSerialNumber(serialNumber: String, listener: SerialNumberChangedListener?)
    fun setAudioName(audioName: String, listener: AudioNameListener?)
    fun getDeviceSystemStatus(deviceSystemStatusListener: DeviceSystemStatusListener)
    fun getAccelerometerConfig(accelerometerConfigListener: AccelerometerConfigListener)
    fun getEEGFilterConfig(eegFilterConfigListener: EEGFilterConfigListener)
}