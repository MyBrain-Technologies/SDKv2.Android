package com.mybraintech.sdk.core.bluetooth

import com.mybraintech.sdk.core.acquisition.MbtDeviceStatusCallback
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.MbtDevice
import com.mybraintech.sdk.core.model.StreamingParams


interface MbtDeviceInterface {

    //----------------------------------------------------------------------------
    // scanning + connection
    //----------------------------------------------------------------------------
    fun startScan(scanResultListener: ScanResultListener)
    fun stopScan()
    fun connectMbt(mbtDevice: MbtDevice, connectionListener: ConnectionListener)
    fun disconnectMbt()

    //----------------------------------------------------------------------------
    // device
    //----------------------------------------------------------------------------
    fun getBleConnectionStatus(): BleConnectionStatus
    fun hasA2dpConnectedDevice(): Boolean

    fun setCurrentDeviceInformationListener(listener: DeviceInformationListener?)
    fun getCurrentDeviceInformation()

    fun getCurrentDeviceA2DPName(): String?
    fun isEEGEnabled(): Boolean
    fun isIMSEnabled(): Boolean
    fun isListeningToHeadsetStatus(): Boolean

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

    //----------------------------------------------------------------------------
    // MARK: only for Test Bench
    //----------------------------------------------------------------------------
    fun setSerialNumber(serialNumber: String, listener: SerialNumberChangedListener?)
}