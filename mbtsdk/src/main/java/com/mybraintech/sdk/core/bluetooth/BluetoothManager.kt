package com.mybraintech.sdk.core.bluetooth

import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener
import com.mybraintech.sdk.core.listener.ScanResultListener
import com.mybraintech.sdk.core.model.MBTDevice

interface IBluetoothManager {

    //----------------------------------------------------------------------------
    // device
    //----------------------------------------------------------------------------
    fun hasConnectedDevice(): Boolean
    fun hasA2dpConnectedDevice(): Boolean
    fun getCurrentDeviceInformation(listener: DeviceInformationListener)
    fun getCurrentDeviceA2DPName(): String?
    fun isListeningToEEG(): Boolean
    fun isListeningToIMS(): Boolean
    fun isListeningToHeadsetStatus(): Boolean

    //----------------------------------------------------------------------------
    // battery
    //----------------------------------------------------------------------------
    fun getBatteryLevel(batteryLevelListener: BatteryLevelListener)

    //----------------------------------------------------------------------------
    // scanning + connection
    //----------------------------------------------------------------------------
    fun startScan(scanResultListener: ScanResultListener)
    fun stopScan()
    fun connect(mbtDevice: MBTDevice, connectionListener: ConnectionListener)
}

// TODO: 09/11/2021 : implement
abstract class BluetoothManager : IBluetoothManager {
}