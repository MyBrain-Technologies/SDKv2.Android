package com.mybraintech.sdk.core.bluetooth.central

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.IntentFilter
import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.characteristics.PostIndus5Characteristic
import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.services.PostIndus5Service
import com.mybraintech.sdk.core.bluetooth.interfaces.IInternalBleManager
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.WriteRequest
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.callback.FailCallback
import no.nordicsemi.android.ble.callback.SuccessCallback
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import timber.log.Timber

class Indus5BleManager(ctx: Context) :
    BleManager(ctx), IInternalBleManager, DataReceivedCallback {

    private var isScanning: Boolean = false
    private var scanOption: MBTScanOption? = null
    private var scanCallback: ScanCallback = getScanCallback()
    private var broadcastReceiver = MbtBleBroadcastReceiver()
    private val scanner: BluetoothLeScannerCompat = BluetoothLeScannerCompat.getScanner()

    private var connectionListener: ConnectionListener? = null
    private var batteryLevelListener: BatteryLevelListener? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    init {
        context.registerReceiver(
            broadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        context.registerReceiver(
            broadcastReceiver,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        )
    }

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
    override fun hasConnectedDevice(): Boolean {
        if (!super.isReady()) {
            Timber.d("device is not ready")
            return false
        }
        val device = super.getBluetoothDevice()
        return if (device == null) {
            Timber.d("device is null")
            false
        } else {
            val result = MbtBleUtils.isIndus5(device)
            Timber.d("result = $result")
            return result
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

    override fun setConnectionListener(connectionListener: ConnectionListener?) {
        this.connectionListener = connectionListener
    }

    override fun connectMbt(scanOption: MBTScanOption?) {
        if (super.isReady()) {
            connectionListener?.onConnectionError(Throwable("A device is connected already : ${bluetoothDevice?.name} |  ${bluetoothDevice?.address}"))
            return
        }

        Timber.i("search indus5 in connected devices")
        val gattConnectedDevices = MbtBleUtils.getGattConnectedDevices(context)
        for (device in gattConnectedDevices) {
            if (MbtBleUtils.isIndus5(device)) {
                Timber.i("search indus5 in connected devices : found a connected indus5, start connection without scan...")
                enqueueConnect(device)
                return
            }
        }
        Timber.i("search indus5 in connected devices : not found")

        if (!isScanning) {
            isScanning = true
            this.scanOption = scanOption
            startScan()
        } else {
            connectionListener?.onScanFailed(Throwable("a scan process is in progress already, can not launch another!"))
        }
    }

    override fun disconnectMbt() {
        if (super.isReady()) {
            disconnect().enqueue()
        } else {
            connectionListener?.onConnectionError(Throwable("there is no connected device to disconnect!"))
        }
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

    //----------------------------------------------------------------------------
    // MARK: private functions
    //----------------------------------------------------------------------------
    private fun startScan() {
        val settings: ScanSettings = ScanSettings.Builder()
            .setLegacy(false)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(5000)
            .setUseHardwareBatchingIfSupported(true)
            .build()
        scanner.startScan(null, settings, scanCallback)
    }

    private fun handleScanResults(results: List<ScanResult>) {
        for (result in results) {
            Timber.d("handleScanResults : name = ${result.device.name} | address = ${result.device.address} ")
            if (MbtBleUtils.isIndus5(result.device)) {
                Timber.d("found indus5 device")
                if ((scanOption == null) || (scanOption?.name == result.device.name)) {
                    Timber.d("stop running scan, start connection process...")
                    scanner.stopScan(scanCallback)
                    isScanning = false
                    enqueueConnect(result.device)
                    break
                }
            }
        }
    }

    private fun enqueueConnect(device: BluetoothDevice) {
        connect(device)
            .useAutoConnect(true)
            .timeout(5000)
            .done {
                Timber.i("enqueueConnect done")
            }
            .fail { device, status ->
                Timber.i("enqueueConnect fail")
            }
            .enqueue()
    }

    private fun getScanCallback(): ScanCallback {
        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Timber.d("onScanResult : name = ${result.device.name} | address = ${result.device.address} ")
                handleScanResults(listOf(result))
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                Timber.d("onBatchScanResults : size = ${results.size}")
                handleScanResults(results)
            }

            override fun onScanFailed(errorCode: Int) {
                connectionListener?.onConnectionError(Throwable("errorCode=$errorCode"))
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
}
