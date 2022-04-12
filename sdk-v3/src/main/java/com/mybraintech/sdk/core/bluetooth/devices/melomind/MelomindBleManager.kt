package com.mybraintech.sdk.core.bluetooth.devices.melomind

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanResult
import android.content.Context
import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessing
import com.mybraintech.sdk.core.bluetooth.DataConversionUtils
import com.mybraintech.sdk.core.bluetooth.MbtBleUtils
import com.mybraintech.sdk.core.bluetooth.devices.MbtBaseBleManager
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.DeviceInformation
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.MbtDevice
import no.nordicsemi.android.ble.BleManager
import timber.log.Timber

class MelomindBleManager(ctx: Context) : MbtBaseBleManager(ctx) {

    // required services
    private var deviceInformationService: BluetoothGattService? = null
    private var measurementService: BluetoothGattService? = null

    private var eegSignalProcessing: EEGSignalProcessing? = null

    //----------------------------------------------------------------------------
    // MARK: ble manager
    //----------------------------------------------------------------------------
    override fun getGattCallback(): BleManager.BleManagerGattCallback = MelomindGattCallback()

    override fun log(priority: Int, message: String) {
        if (message.contains("Notification received from 0000b2a5")) {
            Timber.v(message)
        } else {
            Timber.log(priority, message)
        }
    }

    override fun getBatteryLevel(batteryLevelListener: BatteryLevelListener) {
        this.batteryLevelListener = batteryLevelListener

        val batteryLevelChar =
            measurementService!!.getCharacteristic(MelomindCharacteristic.BATTERY_LEVEL.uuid)

        readCharacteristic(batteryLevelChar)
            .done {
                this.batteryLevelListener?.onBatteryLevel(
                    DataConversionUtils.getBatteryPercentageFromByteValue(
                        batteryLevelChar.value[0]
                    ).toFloat()
                )
            }
            .fail { _, _ ->
                this.batteryLevelListener?.onBatteryLevelError(Throwable("L57 : Cannot read battery level!"))
            }
            .enqueue()
    }

    //----------------------------------------------------------------------------
    // MARK: internal ble manager
    //----------------------------------------------------------------------------
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

    override fun getDeviceType() = EnumMBTDevice.MELOMIND

    override fun getDeviceInformation(deviceInformationListener: DeviceInformationListener) {
        this.deviceInformationListener = deviceInformationListener

        this.deviceInformation = DeviceInformation().also {
            it.productName = targetMbtDevice?.bluetoothDevice?.name ?: ""
        }

        val fwVersion =
            deviceInformationService!!.getCharacteristic(MelomindCharacteristic.FIRMWARE_VERSION.uuid)
        val hwVersion =
            deviceInformationService!!.getCharacteristic(MelomindCharacteristic.HARDWARE_VERSION.uuid)
        val sn =
            deviceInformationService!!.getCharacteristic(MelomindCharacteristic.SERIAL_NUMBER.uuid)
        beginAtomicRequestQueue()
            .add(
                readCharacteristic(fwVersion)
                    .done {
                        this.deviceInformation.firmwareVersion = fwVersion.getStringValue(0)
                    }
            )
            .add(
                readCharacteristic(hwVersion)
                    .done {
                        this.deviceInformation.hardwareVersion = hwVersion.getStringValue(0)
                    }
            )
            .add(
                readCharacteristic(sn)
                    .done {
                        this.deviceInformation.uniqueDeviceIdentifier = sn.getStringValue(0)
                        this.deviceInformationListener?.onDeviceInformation(deviceInformation)
                    }
                    .fail { _, _ ->
                        this.deviceInformationListener?.onDeviceInformation(deviceInformation)
                    }
            )
            .enqueue()
    }

    override fun startEeg(eegSignalProcessing: EEGSignalProcessing) {
        if (deviceInformationService == null || measurementService == null) {
            eegSignalProcessing.eegListener?.onEegError(Throwable("required services not found!"))
            return
        }
        this.eegSignalProcessing = eegSignalProcessing

        // disable/enable status trigger operation
        val mailbox =
            measurementService!!.getCharacteristic(MelomindCharacteristic.MAIL_BOX.uuid)
        val data = if (eegSignalProcessing.isTriggerStatusEnabled) {
            EnumMelomindMailBoxCommand.TRIGGER_STATUS.bytes + 0x01
        } else {
            EnumMelomindMailBoxCommand.TRIGGER_STATUS.bytes + 0x00
        }
        val triggerOp =
            writeCharacteristic(mailbox, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                .done {
                    Timber.d("Trigger status command sent successfully")
                    val response = mailbox.value
                    try {
                        if (response[0] == EnumMelomindMailBoxCommand.TRIGGER_STATUS.bytes[0]) {
                            val size = response[1].toInt()
                            Timber.w("trigger status allocation size = $size")
                            if (size > 0) {
                                if (mtu == 47) {
                                    // bug in firmware v1.7.26 : jira ticket = FM-486
                                    eegSignalProcessing.onTriggerStatusConfiguration(2)
                                } else {
                                    eegSignalProcessing.onTriggerStatusConfiguration(size)
                                }
                            } else {
                                eegSignalProcessing.onTriggerStatusConfiguration(0)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e)
                    }
                }
                .fail { _, _ -> Timber.e("Fail to write trigger status command") }

        // eeg characteristic
        val eegChar =
            measurementService!!.getCharacteristic(MelomindCharacteristic.EEG_ACQUISITION.uuid)

        // setup eeg callback
        setNotificationCallback(eegChar).with { _, eegFrame ->
            if (eegFrame.value != null) {
                eegSignalProcessing.onEEGFrame(eegFrame.value!!)
            } else {
                eegSignalProcessing.eegListener?.onEegError(Throwable("received empty eeg frame!"))
            }
        }

        // configure headset trigger status then enable EEG
        beginAtomicRequestQueue()
            .add(triggerOp)
            .add(
                enableNotifications(eegChar)
                    .done {
                        Timber.d("EEG_ACQUISITION enabled")
                        eegSignalProcessing.onEEGStatusChange(true)
                    }
                    .fail { _, _ ->
                        Timber.e("Could not enable EEG_ACQUISITION")
                        eegSignalProcessing.eegListener?.onEegError(Throwable("could not start EEG"))
                    }
            )
            .enqueue()
    }

    override fun stopEeg() {
        if (deviceInformationService == null || measurementService == null) {
            eegSignalProcessing?.eegListener?.onEegError(Throwable("required services not found!"))
            return
        }

        // eeg characteristic
        val eegChar =
            measurementService!!.getCharacteristic(MelomindCharacteristic.EEG_ACQUISITION.uuid)

        disableNotifications(eegChar)
            .done {
                Timber.d("EEG_ACQUISITION disabled")
                eegSignalProcessing?.onEEGStatusChange(false)
            }
            .fail { _, _ ->
                Timber.e("Could not disable EEG_ACQUISITION")
                eegSignalProcessing?.eegListener?.onEegError(Throwable("could not stop EEG"))
            }
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

    override fun handleScanResults(results: List<ScanResult>) {
        val melomindDevices = mutableListOf<BluetoothDevice>()
        val otherDevices = mutableListOf<BluetoothDevice>()

        val serviceUuid = MelomindService.MEASUREMENT.uuid.toString()

        for (result in results) {
            var isMelomind = false
            if (result.scanRecord?.serviceUuids != null) {
                for (uuid in result.scanRecord!!.serviceUuids) {
                    if (uuid.uuid.toString() == serviceUuid) {
                        isMelomind = true
                        break
                    }
                }
            } else {
                isMelomind = false
            }
            if (isMelomind) {
                melomindDevices.add(result.device)
            } else {
                otherDevices.add(result.device)
            }
        }
        if (melomindDevices.isNotEmpty()) {
            Timber.d("found melomind devices : number = ${melomindDevices.size}")
            scanResultListener.onMbtDevices(melomindDevices.map { MbtDevice(it) })
        }
        if (otherDevices.isNotEmpty()) {
            scanResultListener.onOtherDevices(otherDevices)
        }
    }

    //----------------------------------------------------------------------------
    // MARK: private inner class
    //----------------------------------------------------------------------------
    private inner class MelomindGattCallback() :
        BleManager.BleManagerGattCallback() {

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            connectionListener?.onServiceDiscovered()
            deviceInformationService = gatt.getService(MelomindService.DEVICE_INFORMATION.uuid)
            measurementService = gatt.getService(MelomindService.MEASUREMENT.uuid)
            val isSupported = (deviceInformationService != null && measurementService != null)
            Timber.i("isRequiredServiceSupported = $isSupported")
            return isSupported
        }

        override fun initialize() {
            Timber.d("start initialize Melomind")

            if (bluetoothDevice.bondState != BluetoothDevice.BOND_BONDED) {
                // read battery to create bond (an old firmware engineer told me to do this)
                val batteryLevelChar =
                    measurementService!!.getCharacteristic(MelomindCharacteristic.BATTERY_LEVEL.uuid)
                readCharacteristic(batteryLevelChar)
                    .enqueue()
            }

            beginAtomicRequestQueue()
                .add(
                    requestMtu(MTU_SIZE)
                        .done { Timber.d("requestMtu done") }
                        .fail { _, _ -> Timber.e("Could not requestMtu") }
                )
                .enqueue()
        }

        override fun onServicesInvalidated() {
            // do nothing
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
