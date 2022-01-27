package com.mybraintech.sdk.core.bluetooth

import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessing
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.EEGParams
import com.mybraintech.sdk.core.model.MbtDevice


interface IMbtBleManager {

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
    fun isListeningToEEG(): Boolean
    fun isListeningToIMS(): Boolean
    fun isListeningToHeadsetStatus(): Boolean

    //----------------------------------------------------------------------------
    // battery
    //----------------------------------------------------------------------------
    fun getBatteryLevel(batteryLevelListener: BatteryLevelListener)

    fun getDeviceInformation(deviceInformationListener: DeviceInformationListener)

    //----------------------------------------------------------------------------
    // MARK: streaming
    //----------------------------------------------------------------------------
    fun startEeg(eegSignalProcessing: EEGSignalProcessing)
    fun stopEeg()
}