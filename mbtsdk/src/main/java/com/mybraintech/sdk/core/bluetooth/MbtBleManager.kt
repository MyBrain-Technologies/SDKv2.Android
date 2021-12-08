package com.mybraintech.sdk.core.bluetooth

import android.content.Context
import com.mybraintech.sdk.core.bluetooth.central.Indus5BleManager
import com.mybraintech.sdk.core.bluetooth.central.MBTScanOption
import com.mybraintech.sdk.core.bluetooth.interfaces.IInternalBleManager
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener


interface IBleManager {
    //----------------------------------------------------------------------------
    // device
    //----------------------------------------------------------------------------
    fun init(isIndus5: Boolean)
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
    fun setConnectionListener(connectionListener: ConnectionListener)
    fun connect(scanOption: MBTScanOption? = null)
    fun disconnect()
}

class MbtBleManager(private val context: Context) : IBleManager {

    private var isIndus5: Boolean = true
    private lateinit var internalBleManager: IInternalBleManager

    override fun init(isIndus5: Boolean) {
        this.isIndus5 = isIndus5
        if (isIndus5) {
            internalBleManager = Indus5BleManager(context)
        } else {
            TODO("not yet implemented")
        }
    }

    override fun connect(scanOption: MBTScanOption?) {
        return internalBleManager.connectMbt(scanOption)
    }

    override fun disconnect() {
        return internalBleManager.disconnectMbt()
    }

    override fun setConnectionListener(connectionListener: ConnectionListener) {
        return internalBleManager.setConnectionListener(connectionListener)
    }

    override fun hasConnectedDevice(): Boolean {
        return internalBleManager.hasConnectedDevice()
    }

    override fun hasA2dpConnectedDevice(): Boolean {
        return internalBleManager.hasA2dpConnectedDevice()
    }

    override fun setCurrentDeviceInformationListener(listener: DeviceInformationListener?) {
        return internalBleManager.setCurrentDeviceInformationListener(listener)
    }

    override fun getCurrentDeviceInformation() {
        return internalBleManager.getCurrentDeviceInformation()
    }

    override fun getCurrentDeviceA2DPName(): String? {
        return internalBleManager.getCurrentDeviceA2DPName()
    }

    override fun isListeningToEEG(): Boolean {
        return internalBleManager.isListeningToEEG()
    }

    override fun isListeningToIMS(): Boolean {
        return internalBleManager.isListeningToIMS()
    }

    override fun isListeningToHeadsetStatus(): Boolean {
        return internalBleManager.isListeningToHeadsetStatus()
    }

    override fun setBatteryLevelListener(batteryLevelListener: BatteryLevelListener?) {
        return internalBleManager.setBatteryLevelListener(batteryLevelListener)
    }

    override fun getBatteryLevel() {
        return internalBleManager.getBatteryLevel()
    }
}