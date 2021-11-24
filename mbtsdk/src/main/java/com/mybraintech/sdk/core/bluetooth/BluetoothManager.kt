package com.mybraintech.sdk.core.bluetooth

import android.content.Context
import com.mybraintech.sdk.core.bluetooth.central.*
import com.mybraintech.sdk.core.bluetooth.peripheral.Peripheral
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener

interface IBluetoothManager {

    //----------------------------------------------------------------------------
    // device
    //----------------------------------------------------------------------------
    fun hasConnectedDevice(context: Context): Boolean
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
    fun disconnect()

}

// TODO: 09/11/2021 : implement
class BluetoothManager(private val context: Context) : IBluetoothManager {

    private var peripheral: Peripheral? =null
    private lateinit var bluetoothCentral: IBluetoothCentral
    private var connectionListener: ConnectionListener? = null
    private var batteryLevelListener: BatteryLevelListener? = null

    override fun connect(scanOption: MBTScanOption) {
        if (hasConnectedDevice(context)) {
            connectionListener?.onConnectionError(Throwable("A device is connected already, please disconnect first!"))
        } else {
            if (scanOption.isIndus5) {
                configureAndConnectIndus5(scanOption)
            } else {
                TODO("not yet implemented")
            }
        }
    }

    private fun configureAndConnectIndus5(scanOption: MBTScanOption) {
        val bleManager = Indus5BleManager(context, null)

        bleManager.setConnectionListener(connectionListener)
        bluetoothCentral = BluetoothCentral(context, bleManager)

        bleManager.setBatteryLevelListener(batteryLevelListener)
        peripheral = Peripheral(bleManager)

        bluetoothCentral?.connect(scanOption)
    }

    override fun disconnect() {
        if (!::bluetoothCentral.isInitialized) {
            bluetoothCentral = BluetoothCentral(context, Indus5BleManager(context, null))
        }
        bluetoothCentral.disconnect()
    }

    override fun setConnectionListener(connectionListener: ConnectionListener) {
        this.connectionListener = connectionListener
    }

    override fun hasConnectedDevice(context: Context): Boolean {
        return BluetoothCentral.hasConnectedDevice(context)
    }

    override fun hasA2dpConnectedDevice(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setCurrentDeviceInformationListener(listener: DeviceInformationListener?) {
        TODO("Not yet implemented")
    }

    override fun getCurrentDeviceInformation(listener: DeviceInformationListener) {
        TODO("Not yet implemented")
    }

    override fun getCurrentDeviceA2DPName(): String? {
        TODO("Not yet implemented")
    }

    override fun isListeningToEEG(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isListeningToIMS(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isListeningToHeadsetStatus(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setBatteryLevelListener(batteryLevelListener: BatteryLevelListener?) {
        this.batteryLevelListener = batteryLevelListener //todo how to pass this to peripheral ?
    }

    override fun getBatteryLevel() {
        peripheral?.requestBatteryLevel()
    }

}