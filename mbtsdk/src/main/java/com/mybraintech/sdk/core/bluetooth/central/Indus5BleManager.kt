package com.mybraintech.sdk.core.bluetooth.central

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.IntentFilter
import com.mybraintech.sdk.core.bluetooth.IMbtBleManager
import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.characteristics.PostIndus5Characteristic
import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.services.PostIndus5Service
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener
import com.mybraintech.sdk.core.listener.ScanResultListener
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.MbtDevice
import com.mybraintech.sdk.util.getString
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.WriteRequest
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.callback.FailCallback
import no.nordicsemi.android.ble.callback.SuccessCallback
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanResult
import timber.log.Timber

class Indus5BleManager(ctx: Context) :
    BleManager(ctx), IMbtBleManager, DataReceivedCallback {

    private var isScanning: Boolean = false
    private var broadcastReceiver = MbtBleBroadcastReceiver()
    private val mbtBleScanner = MbtBleScanner()

    private var targetMbtDevice: MbtDevice? = null
    private lateinit var scanResultListener: ScanResultListener
    private var connectionListener: ConnectionListener? = null
    private var batteryLevelListener: BatteryLevelListener? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    //----------------------------------------------------------------------------
    // MARK: ble manager
    //----------------------------------------------------------------------------
    override fun getGattCallback(): BleManagerGattCallback = Indus5GattCallback(this)

    override fun log(priority: Int, message: String) {
        Timber.log(priority, message)
    }

    //----------------------------------------------------------------------------
    // MARK: IbluetoothUsage
    //----------------------------------------------------------------------------

    override fun setBatteryLevelListener(batteryLevelListener: BatteryLevelListener?) {
        this.batteryLevelListener = batteryLevelListener
    }

    //----------------------------------------------------------------------------
    // MARK: rx data receive callback
    //----------------------------------------------------------------------------
    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        Timber.i("onDataReceived : ${data.value}")
    }

    //----------------------------------------------------------------------------
    // MARK: internal ble manager
    //----------------------------------------------------------------------------
    private fun isBluetoothEnabled(): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isEnabled
    }

    private fun isConnectedAndReady(): Boolean {
        return (super.isConnected() && super.isReady())
    }

    override fun startScan(scanResultListener: ScanResultListener) {
        if (!isBluetoothEnabled()) {
            scanResultListener.onScanError(Throwable("Bluetooth is not enabled"))
            return
        }
        this.scanResultListener = scanResultListener

        if (!isScanning) {
            isScanning = true
            mbtBleScanner.startScan(getScanCallback())
        } else {
            scanResultListener.onScanError(Throwable("a scan process is in progress already, can not launch another!"))
        }
    }

    override fun stopScan() {
        isScanning = false
        mbtBleScanner.stopScan()
    }

    override fun connectMbt(mbtDevice: MbtDevice, connectionListener: ConnectionListener) {
        if (!isBluetoothEnabled()) {
            connectionListener.onConnectionError(Throwable("Bluetooth is not enabled"))
            return
        }
        if (isConnectedAndReady()) {
            connectionListener.onConnectionError(Throwable("Device is connected already : ${bluetoothDevice.getString()}"))
            return
        }
        this.connectionListener = connectionListener
        this.targetMbtDevice = mbtDevice
        broadcastReceiver.register(context, mbtDevice.bluetoothDevice, connectionListener)
        connect(mbtDevice.bluetoothDevice)
            .useAutoConnect(false)
            .timeout(5000)
            .done {
                Timber.i("ble connect done")
            }
            .fail { device, status ->
                connectionListener.onConnectionError(Throwable("fail to connect to MbtDevice : name = ${device?.name} | status = $status"))
            }
            .enqueue()
    }

    override fun disconnectMbt() {
        if (super.isConnected()) {
            targetMbtDevice = null
            this.broadcastReceiver.targetDevice = null
            disconnect().enqueue()
        } else {
            connectionListener?.onConnectionError(Throwable("there is no connected device to disconnect!"))
        }
    }

    override fun getBleConnectionStatus(): BleConnectionStatus {
        val gattConnectedDevices = MbtBleUtils.getGattConnectedDevices(context)
        var connectedIndus5: BluetoothDevice? = null
        for (device in gattConnectedDevices) {
            if (MbtBleUtils.isIndus5(device)) {
                Timber.i("found a connected indus5")
                connectedIndus5 = device
                break
            }
        }
        if (!isConnectedAndReady()) {
            Timber.d("device is NOT ready or is not connected")
            return if (connectedIndus5 != null) {
                BleConnectionStatus(MbtDevice(connectedIndus5), false)
            } else {
                BleConnectionStatus(null, false)
            }
        } else {
            Timber.d("device is ready and is connected")
            if (bluetoothDevice != null) {
                return BleConnectionStatus(MbtDevice(bluetoothDevice!!), true)
            } else {
                // this case should never happen
                Timber.e("fatal error: bluetoothDevice is null")
                return BleConnectionStatus(null, false)
            }
        }
    }

    override fun hasA2dpConnectedDevice(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setCurrentDeviceInformationListener(listener: DeviceInformationListener?) {
        TODO("Not yet implemented")
    }

    override fun getCurrentDeviceInformation() {
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

    override fun getBatteryLevel() {
        getBatteryMailboxRequest()
            .done {
                Timber.i("readBatteryLevelMbt done")
            }
            .enqueue()
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
        override fun onRequestCompleted(device: BluetoothDevice?) {
            Timber.i(message)
        }
    }

    private class Indus5FailCallback(private val message: String) : FailCallback {
        override fun onRequestFailed(device: BluetoothDevice?, status: Int) {
            Timber.e("$message : status = $status")
        }
    }

    private fun handleScanResults(results: List<BluetoothDevice>) {
        val indus5Devices = results.filter { MbtBleUtils.isIndus5(it) }
        if (indus5Devices.isNotEmpty()) {
            Timber.d("found indus5 devices : number = ${indus5Devices.size}")
            scanResultListener.onMbtDevices(indus5Devices.map { MbtDevice(it) })
        }
        val otherDevices = results.filter { !MbtBleUtils.isIndus5(it) }
        if (otherDevices.isNotEmpty()) {
            Timber.d("found other devices : number = ${otherDevices.size}")
            scanResultListener.onOtherDevices(otherDevices)
        }
    }

    private fun getScanCallback(): ScanCallback {
        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Timber.d("onScanResult : name = ${result.device.name} | address = ${result.device.address} ")
                handleScanResults(listOf(result.device))
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                Timber.d("onBatchScanResults : size = ${results.size}")
                handleScanResults(results.map { it.device })
            }

            override fun onScanFailed(errorCode: Int) {
                val msg = "onScanFailed : errorCode=$errorCode"
                Timber.e(msg)
                scanResultListener.onScanError(Throwable(msg))
            }
        }
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

    //----------------------------------------------------------------------------
    // MARK: private inner class
    //----------------------------------------------------------------------------
    private inner class Indus5GattCallback(private val rxDataReceivedCallback: DataReceivedCallback) :
        BleManagerGattCallback() {

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            connectionListener?.onServiceDiscovered()
            val service = gatt.getService(PostIndus5Service.Transparent.uuid)
            rxCharacteristic =
                service?.getCharacteristic(PostIndus5Characteristic.Rx.uuid)
            txCharacteristic =
                service?.getCharacteristic(PostIndus5Characteristic.Tx.uuid)
            val isSupported = (txCharacteristic != null && rxCharacteristic != null)
            Timber.i("isRequiredServiceSupported = $isSupported")
            return isSupported
        }

        override fun initialize() {
            Timber.d("start initialize")
            assert(rxCharacteristic != null)

            setNotificationCallback(rxCharacteristic).with(rxDataReceivedCallback)

            // Try to execute requests, this procedure do not make sure that all requests will be executed successfully.
            // For example in Android 11, user can refuse to bond device, which will cause a fail connection.
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
                .enqueue()
        }

        override fun onServicesInvalidated() {
            rxCharacteristic = null
            txCharacteristic = null
        }

        override fun onDeviceReady() {
            connectionListener?.onDeviceReady()
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int) {
            Timber.i("onMtuChanged : mtu = $mtu")
        }

        override fun onDeviceDisconnected() {
            Timber.i("onDeviceDisconnected")
            connectionListener?.onDeviceDisconnected()
        }
    }
}
