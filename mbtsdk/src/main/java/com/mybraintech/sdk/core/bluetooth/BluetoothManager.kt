package com.mybraintech.sdk.core.bluetooth

import android.content.Context
import com.mybraintech.sdk.core.bluetooth.central.BluetoothCentral
import com.mybraintech.sdk.core.bluetooth.central.IBluetoothCentral
import com.mybraintech.sdk.core.bluetooth.central.MBTScanOption
import com.mybraintech.sdk.core.bluetooth.peripheral.Peripheral
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener

interface IBluetoothManager {

    //----------------------------------------------------------------------------
    // device
    //----------------------------------------------------------------------------
    fun hasConnectedDevice(): Boolean
    fun hasA2dpConnectedDevice(): Boolean

    fun setCurrentDeviceInformationListener(listener: DeviceInformationListener?)
    fun getCurrentDeviceInformation(listener: DeviceInformationListener)

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
    fun connect(scanOption: MBTScanOption)

}

// TODO: 09/11/2021 : implement
abstract class BluetoothManager(context: Context) : IBluetoothManager {

    private lateinit var peripheral: Peripheral
    private var bluetoothCentral: IBluetoothCentral = BluetoothCentral(context)

    override fun connect(scanOption: MBTScanOption) {
        if (scanOption.isIndus5) {
            bluetoothCentral.connect(scanOption)
        } else {
            TODO("not yet implemented")
        }
    }

    override fun setConnectionListener(connectionListener: ConnectionListener) {
        bluetoothCentral.setConnectionListener(connectionListener)
    }

    override fun getBatteryLevel() {
        peripheral.requestBatteryLevel()
    }

}