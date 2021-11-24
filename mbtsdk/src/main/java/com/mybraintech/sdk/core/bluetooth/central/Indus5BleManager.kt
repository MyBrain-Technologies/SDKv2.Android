package com.mybraintech.sdk.core.bluetooth.central

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.characteristics.PostIndus5Characteristic
import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.services.PostIndus5Service
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.WriteRequest
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.callback.FailCallback
import no.nordicsemi.android.ble.callback.SuccessCallback
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
            assert(rxCharacteristic != null)

            rxDataReceivedCallback?.let {
                //todo: parse indus5 response here to battery listener...
                setNotificationCallback(rxCharacteristic).with(it)
            }

            beginAtomicRequestQueue()
                .add(
                    enableNotifications(rxCharacteristic)
                        .done("rx enableNotifications done".getSuccessCallback())
                        .fail("Could not subscribe".getFailCallback())
                )
                .add(
                    requestMtu(47)
                        .done("requestMtu done".getSuccessCallback())
                        .fail("Could not requestMtu".getFailCallback())
                )
                .add(
                    getMtuMailboxRequest()
                        .done("MtuMailboxRequest done".getSuccessCallback())
                        .fail("Could not MtuMailboxRequest".getFailCallback())
                )
                .done("Target initialized".getSuccessCallback())
                .fail("Could not initialize".getFailCallback())
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
            Timber.i("onDeviceDisconnected")
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

    //----------------------------------------------------------------------------
    // MARK: gatt callback
    //----------------------------------------------------------------------------
    private fun String.getFailCallback(): FailCallback {
        return Indus5FailCallback(this)
    }

    private fun String.getSuccessCallback(): SuccessCallback {
        return Indus5SuccessCallback(this)
    }

    private class Indus5SuccessCallback(private val message: String) : SuccessCallback {
        override fun onRequestCompleted(device: BluetoothDevice) {
            Timber.i(message)
        }
    }

    private class Indus5FailCallback(private val message: String) : FailCallback {
        override fun onRequestFailed(device: BluetoothDevice, status: Int) {
            Timber.e("$message : status = $status")
        }
    }
}
