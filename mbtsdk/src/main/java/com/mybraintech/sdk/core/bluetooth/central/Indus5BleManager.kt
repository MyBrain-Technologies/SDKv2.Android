package com.mybraintech.sdk.core.bluetooth.central

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.characteristics.PostIndus5Characteristic
import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.services.PostIndus5Service
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.WriteRequest
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import timber.log.Timber

class Indus5BleManager(ctx: Context, val rxDataReceivedCallback: DataReceivedCallback?) :
    BleManager(ctx), IBluetoothConnectable, IBluetoothUsage {

    private var connectionListener: ConnectionListener? = null
    private var batteryLevelListener: BatteryLevelListener? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    override fun getGattCallback(): BleManagerGattCallback = GattCallback()

    override fun log(priority: Int, message: String) {
        Timber.log(priority, message)
    }

    private fun getMtuMailboxRequest(): WriteRequest {
        val CMD_CHANGE_MTU: ByteArray = byteArrayOf(0x29.toByte(), 0x2F)
        return writeCharacteristic(
            txCharacteristic,
            CMD_CHANGE_MTU,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getBatteryMailboxRequest(): WriteRequest {
        val CMD_READ_BATTERY_LEVEL: ByteArray = byteArrayOf(0x20.toByte())
        return writeCharacteristic(
            txCharacteristic,
            CMD_READ_BATTERY_LEVEL,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private inner class GattCallback : BleManagerGattCallback() {

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(PostIndus5Service.Transparent.uuid)
            rxCharacteristic =
                service?.getCharacteristic(PostIndus5Characteristic.Rx.uuid)
            txCharacteristic =
                service?.getCharacteristic(PostIndus5Characteristic.Tx.uuid)
            return (txCharacteristic != null && rxCharacteristic != null)
        }

        override fun initialize() {
            rxDataReceivedCallback?.let {
                //todo: parse indus5 response here to battery listener...
                setNotificationCallback(rxCharacteristic).with(it)
            }

            beginAtomicRequestQueue()
                .add(
                    enableNotifications(rxCharacteristic)
                        .done {
                            Timber.i("rx enableNotifications done")
                        }
                        .fail { _: BluetoothDevice?, status: Int ->
                            log(Log.ERROR, "Could not subscribe: $status")
                        }
                )
                .add(
                    requestMtu(47)
                        .done {
                            Timber.i("requestMtu done")
                        }
                        .fail { _, status ->
                            Timber.e("Could not requestMtu: $status")
                        }
                )
                .add(
                    getMtuMailboxRequest()
                        .done {
                            Timber.i("MtuMailboxRequest done")
                        }
                        .fail { _, status ->
                            Timber.e("Could not MtuMailboxRequest: $status")
                        }
                )
                .done {
                    log(Log.INFO, "Target initialized")
                }
                .fail { _, status ->
                    Timber.e("Could not initialize: $status")
                    disconnect().enqueue()
                }
                .enqueue()
        }

        override fun onServicesInvalidated() {
            rxCharacteristic = null
            txCharacteristic = null
        }

        override fun onDeviceReady() {
            connectionListener?.onDeviceConnectionStateChanged(true)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int) {
            Timber.i("onMtuChanged : mtu = $mtu")
        }

        override fun onDeviceDisconnected() {
            connectionListener?.onDeviceConnectionStateChanged(false)
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
        disconnect()
            .enqueue()
    }

    override fun setConnectionListener(connectionListener: ConnectionListener?) {
        this.connectionListener = connectionListener
    }

    //----------------------------------------------------------------------------
    // MARK: IbluetoothUsage
    //----------------------------------------------------------------------------

    override fun readBatteryLevelMbt() {
        getBatteryMailboxRequest()
            .done {
                Timber.i("readBatteryLevelMbt done")
            }
            .enqueue()
    }

    override fun setBatteryLevelListener(batteryLevelListener: BatteryLevelListener?) {
        this.batteryLevelListener = batteryLevelListener
    }
}
