package com.mybraintech.sdk.core.bluetooth.devices.qplus

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Handler
import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessing
import com.mybraintech.sdk.core.bluetooth.MbtBleUtils
import com.mybraintech.sdk.core.bluetooth.devices.MbtBaseBleManager
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.DeviceInformation
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.MbtDevice
import no.nordicsemi.android.ble.WriteRequest
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data
import timber.log.Timber


class QPlusBleManager(ctx: Context) :
    MbtBaseBleManager(ctx), DataReceivedCallback {

    private var eegSignalProcessing: EEGSignalProcessing? = null

    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    //----------------------------------------------------------------------------
    // MARK: ble manager
    //----------------------------------------------------------------------------
    override fun getGattCallback(): BleManagerGattCallback = Indus5GattCallback(this)

    override fun log(priority: Int, message: String) {
        if (message.contains("value: (0x) 40")) {
            Timber.v(message)
        } else {
            Timber.log(priority, message)
        }
    }

    override fun getBatteryLevel(batteryLevelListener: BatteryLevelListener) {
        this.batteryLevelListener = batteryLevelListener
        getBatteryLevelMailboxRequest()
            .fail { _, _ ->
                batteryLevelListener.onBatteryLevelError(Throwable())
            }
            .enqueue()
    }

    //----------------------------------------------------------------------------
    // MARK: rx data receive callback
    //----------------------------------------------------------------------------
    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        if (data.value != null) {
//            Timber.i("onDataReceived : ${NumericalUtils.bytesToHex(data.value)}")
            when (val indus5Response = QPlusMailboxHelper.parseRawIndus5Response(data.value!!)) {
                is QPlusResponse.MtuChange -> {
                    Timber.i("Mailbox MTU changed successfully")
                }
                is QPlusResponse.BatteryLevel -> {
                    batteryLevelListener?.onBatteryLevel(indus5Response.percent)
                }
                is QPlusResponse.FirmwareVersion -> {
                    deviceInformation.firmwareVersion = indus5Response.version
                }
                is QPlusResponse.HardwareVersion -> {
                    deviceInformation.hardwareVersion = indus5Response.version
                }
                is QPlusResponse.SerialNumber -> {
                    deviceInformation.uniqueDeviceIdentifier = indus5Response.serialNumber
                }
                is QPlusResponse.DeviceName -> {
                    deviceInformation.productName = indus5Response.name
                }
                is QPlusResponse.EEGStatus -> {
                    eegSignalProcessing?.onEEGStatusChange(indus5Response.isEnabled)
                }
                is QPlusResponse.EEGFrame -> {
                    eegSignalProcessing?.onEEGFrame(indus5Response.data)
                }
                is QPlusResponse.TriggerStatusConfiguration -> {
                    eegSignalProcessing?.onTriggerStatusConfiguration(indus5Response.triggerStatusAllocationSize)
                }
                else -> {
                    Timber.e("this type is not supported : ${indus5Response.javaClass.simpleName}")
                }
            }
        } else {
            Timber.e("data value is null")
        }
    }

    //----------------------------------------------------------------------------
    // MARK: internal ble manager
    //----------------------------------------------------------------------------
    override fun connectMbt(mbtDevice: MbtDevice, connectionListener: ConnectionListener) {
        /**
         * remove bond to fix data loss bug on Android 9 (SDK-415).
         * We do not apply this for Android 10 and later since this will show a popup to ask
         * user permission each time.
         */
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            removeBond(mbtDevice.bluetoothDevice)
        }

        super.connectMbt(mbtDevice, connectionListener)
    }

    override fun getBleConnectionStatus(): BleConnectionStatus {
        val gattConnectedDevices = MbtBleUtils.getGattConnectedDevices(context)
        var connectedIndus5: BluetoothDevice? = null
        for (device in gattConnectedDevices) {
            if (MbtBleUtils.isQPlus(device)) {
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

    override fun getDeviceType() = EnumMBTDevice.Q_PLUS

    override fun handleScanResults(results: List<ScanResult>) {
        val devices = results.map { it.device }
        val indus5Devices = devices.filter { MbtBleUtils.isQPlus(it) }
        if (indus5Devices.isNotEmpty()) {
            Timber.d("found indus5 devices : number = ${indus5Devices.size}")
            scanResultListener.onMbtDevices(indus5Devices.map { MbtDevice(it) })
        }
        val otherDevices = devices.filter { !MbtBleUtils.isQPlus(it) }
        if (otherDevices.isNotEmpty()) {
            Timber.d("found other devices : number = ${otherDevices.size}")
            scanResultListener.onOtherDevices(otherDevices)
        }
    }

    override fun getDeviceInformation(deviceInformationListener: DeviceInformationListener) {
        this.deviceInformation = DeviceInformation().also {
            it.productName = targetMbtDevice?.bluetoothDevice?.name ?: ""
        }
        this.deviceInformationListener = deviceInformationListener
        beginAtomicRequestQueue()
//            .add(getDeviceNameMailboxRequest()) //do not need to call this, we have already device name
            .add(getFirmwareVersionMailboxRequest())
            .add(getHardwareVersionMailboxRequest())
            .add(getSerialNumberMailboxRequest())
            .done {
                //emit result immediately if completed, otherwise emit result (may be not completed) with 100 ms delay
                if (deviceInformation.isCompleted()) {
                    Timber.d("device information is completed, trigger listener...")
                    deviceInformationListener.onDeviceInformation(deviceInformation)
                } else {
                    Handler(context.mainLooper).postDelayed({
                        Timber.d("Delayed callback : device information is completed = ${this.deviceInformation.isCompleted()}, trigger listener...")
                        deviceInformationListener.onDeviceInformation(this.deviceInformation)
                    }, 100)
                }
            }
            .fail { _, status ->
                deviceInformationListener.onDeviceInformationError(Throwable("fail to retrieve device information : status = $status"))
            }
            .enqueue()
    }

    override fun startEeg(eegSignalProcessing: EEGSignalProcessing) {
        this.eegSignalProcessing = eegSignalProcessing

        // create status operation
        val statusCommand = EnumQPlusFrameSuffix.MBX_P300_ENABLE.bytes.toMutableList()
        statusCommand.add(
            if (eegSignalProcessing.isTriggerStatusEnabled) {
                0x01
            } else {
                0x00
            }
        )
        val statusOperation = writeCharacteristic(
            txCharacteristic,
            statusCommand.toByteArray(),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )

        // eeg streaming operation
        val eegOperation = writeCharacteristic(
            txCharacteristic,
            EnumQPlusFrameSuffix.MBX_START_EEG_ACQUISITION.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )

        // operation queue
        beginAtomicRequestQueue()
            .add(statusOperation)
            .add(eegOperation)
            .enqueue()
    }

    override fun stopEeg() {
        writeCharacteristic(
            txCharacteristic,
            EnumQPlusFrameSuffix.MBX_STOP_EEG_ACQUISITION.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
            .enqueue()
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

    private fun getMtuMailboxRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            QPlusMailboxHelper.generateMtuChangeBytes(MTU_SIZE),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getBatteryLevelMailboxRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            EnumQPlusFrameSuffix.MBX_GET_BATTERY_VALUE.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getDeviceNameMailboxRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            EnumQPlusFrameSuffix.MBX_GET_DEVICE_NAME.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getFirmwareVersionMailboxRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            EnumQPlusFrameSuffix.MBX_GET_FIRMWARE_VERSION.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getHardwareVersionMailboxRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            EnumQPlusFrameSuffix.MBX_GET_HARDWARE_VERSION.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getSerialNumberMailboxRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            EnumQPlusFrameSuffix.MBX_GET_SERIAL_NUMBER.bytes,
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
            val service = gatt.getService(QPlusService.Transparent.uuid)
            rxCharacteristic =
                service?.getCharacteristic(QPlusCharacteristic.Rx.uuid)
            txCharacteristic =
                service?.getCharacteristic(QPlusCharacteristic.Tx.uuid)
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
                        .done { Timber.d("rx enableNotifications done") }
                        .fail { _, _ -> Timber.e("Could not subscribe") }
                )
                .add(
                    requestMtu(MTU_SIZE)
                        .done { Timber.d("requestMtu done") }
                        .fail { _, _ -> Timber.e("Could not requestMtu") }
                )
                .add(
                    getMtuMailboxRequest()
                        .done { Timber.d("MtuMailboxRequest done") }
                        .fail { _, _ -> Timber.e("Could not MtuMailboxRequest") }
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

    private fun removeBond(device: BluetoothDevice) {
        try {
            device::class.java.getMethod("removeBond").invoke(device)
        } catch (e: Exception) {
            Timber.i("Removing bond has been failed: ${e.message}")
        }
    }
}
