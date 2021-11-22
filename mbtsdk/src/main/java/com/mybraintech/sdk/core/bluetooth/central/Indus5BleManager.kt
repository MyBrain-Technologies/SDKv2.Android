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

class Indus5BleManager(ctx: Context, val connectionListener: ConnectionListener?, val rxDataReceivedCallback: DataReceivedCallback?) : BleManager(ctx) {

    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    override fun getGattCallback(): BleManagerGattCallback = GattCallback()

    override fun log(priority: Int, message: String) {
        Timber.log(priority, message)
    }

//    fun readBattery() {
//        if (txCharacteristic != null) {
//            writeCharacteristic(
//                txCharacteristic,
//                BleCommand.ReadBattery.command,
//                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
//            ).await(object : DataSentCallback {
//                override fun onDataSent(device: BluetoothDevice, data: Data) {
//
//                }
//            })
//        }
//    }

    fun getMtuMailboxOperation() : Operation {
        val CMD_READ_BATTERY : ByteArray = byteArrayOf(0x20.toByte())
       return writeCharacteristic(txCharacteristic,
            CMD_READ_BATTERY,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)

    }

    private inner class GattCallback : BleManagerGattCallback() {

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(PostIndus5Service.Transparent.uuid)
            rxCharacteristic =
                service?.getCharacteristic(PostIndus5Characteristic.Rx.uuid)
            txCharacteristic =
                service?.getCharacteristic(PostIndus5Characteristic.Tx.uuid)
            val myCharacteristicProperties = rxCharacteristic?.properties ?: 0
            return (myCharacteristicProperties and BluetoothGattCharacteristic.PROPERTY_READ != 0) &&
                    (myCharacteristicProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0)
        }

        override fun initialize() {
            rxDataReceivedCallback?.let {
                setNotificationCallback(rxCharacteristic).with(it)
            }

            beginAtomicRequestQueue()
                .add(enableNotifications(rxCharacteristic)
                    .fail { _: BluetoothDevice?, status: Int ->
                        log(Log.ERROR, "Could not subscribe: $status")
                        disconnect().enqueue()
                    }
                )
                .add(requestMtu(47))
                .add(getMtuMailboxOperation())
                .done {
                    log(Log.INFO, "Target initialized")
                    // TODO: 22/11/2021 change mtu by mailbox
                    requestMtu(47).enqueue()
                }
                .fail { device, status ->

                }
                .enqueue()
        }

        override fun onServicesInvalidated() {
            rxCharacteristic = null
            txCharacteristic = null
        }

        override fun onDeviceReady() {

        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int) {

        }

        override fun onDeviceDisconnected() {

        }
    }
}
