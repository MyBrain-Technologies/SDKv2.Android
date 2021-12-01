package com.mybraintech.sdk.core.bluetooth.interfaces

import android.content.Context
import com.mybraintech.sdk.core.bluetooth.central.MBTScanOption
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener

interface IInternalBleManager {
    //----------------------------------------------------------------------------
    // device
    //----------------------------------------------------------------------------
    fun hasConnectedDevice(): Boolean
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
    fun setBatteryLevelListener(batteryLevelListener: BatteryLevelListener?)
    fun getBatteryLevel()

    //----------------------------------------------------------------------------
    // scanning + connection
    //----------------------------------------------------------------------------
    fun setConnectionListener(connectionListener: ConnectionListener?)
    fun connectMbt(scanOption: MBTScanOption?)
    fun disconnectMbt()
}
