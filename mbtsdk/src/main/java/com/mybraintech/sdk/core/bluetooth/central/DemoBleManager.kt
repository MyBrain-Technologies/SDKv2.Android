package com.mybraintech.sdk.core.bluetooth.central

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.characteristics.PostIndus5Characteristic
import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.services.PostIndus5Service
import com.mybraintech.sdk.core.listener.ConnectionListener
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.Operation
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import timber.log.Timber

class DemoBleManager(ctx: Context)
    : BleManager(ctx), IBluetoothConnectable {

    private var txCharacteristic: BluetoothGattCharacteristic? = null

    override fun getGattCallback(): BleManagerGattCallback = GattCallback()

    override fun log(priority: Int, message: String) {
        Timber.log(priority, message)
    }

    fun getMtuMailboxOperation() : Operation {
        val CMD_READ_BATTERY : ByteArray = byteArrayOf(0x20.toByte())
       return writeCharacteristic(txCharacteristic,
            CMD_READ_BATTERY,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)

    }

    private inner class GattCallback : BleManagerGattCallback() {

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            //todo: services is discovered
            return true
        }

        override fun initialize() {
            //todo: start to subscribe rx ??
        }

        override fun onServicesInvalidated() {
            //todo: clear
        }

        override fun onDeviceReady() {

        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int) {

        }

        override fun onDeviceDisconnected() {

        }
    }

    //----------------------------------------------------------------------------
    // MARK: IBluetoothConnectable
    //----------------------------------------------------------------------------
    override fun connectMbt(device: BluetoothDevice) {
        connect(device)
                    .useAutoConnect(true)
                    .timeout(5000)
                    .enqueue()
    }

    override fun disconnectMbt() {
        TODO("Not yet implemented")
    }
}
