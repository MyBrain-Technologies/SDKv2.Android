package com.mybraintech.sdk.core.bluetooth

import com.mybraintech.sdk.core.acquisition.MbtDeviceStatusCallback
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.MbtDevice
import com.mybraintech.sdk.core.model.StreamingParams
import java.io.InputStream


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
    fun getSensorStatuses(sensorStatusListener: SensorStatusListener)

    //----------------------------------------------------------------------------
    // battery
    //----------------------------------------------------------------------------
    fun getBatteryLevel(batteryLevelListener: BatteryLevelListener)

    fun getDeviceInformation(deviceInformationListener: DeviceInformationListener)

    //----------------------------------------------------------------------------
    // MARK: streaming
    //----------------------------------------------------------------------------
    fun enableSensors(
        streamingParams: StreamingParams,
        dataReceiver: MbtDataReceiver,
        deviceStatusCallback: MbtDeviceStatusCallback
    )

    fun disableSensors()
    fun isEEGEnabled(): Boolean

    //----------------------------------------------------------------------------
    // MARK: only for Test Bench
    //----------------------------------------------------------------------------
    fun setSerialNumber(serialNumber: String, listener: SerialNumberChangedListener?)
    fun setAudioName(audioName: String, listener: AudioNameListener?)
    fun getDeviceSystemStatus(deviceSystemStatusListener: DeviceSystemStatusListener)
    fun getAccelerometerConfig(accelerometerConfigListener: AccelerometerConfigListener)

    /**
     * setup/valid the OAD file before executing an [startDFU]
     * @param firmware OAD file
     * @see startDFU
     */
    fun prepareDFU(firmware: InputStream): Boolean

    /**
     * @return `true` if BLE command is sent successfully, then later
     * [DriverFirmwareUpgradeListener.onDFUInitialized] will be triggered
     * if the headset accept the DFU request
     * @see prepareDFU
     */
    fun startDFU(listener: DriverFirmwareUpgradeListener): Boolean
}