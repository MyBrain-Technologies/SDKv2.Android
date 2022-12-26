package com.mybraintech.sdk.core.bluetooth.devices.melomind

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.SystemClock
import com.mybraintech.sdk.core.acquisition.MbtDeviceStatusCallback
import com.mybraintech.sdk.core.bluetooth.DataConversionUtils
import com.mybraintech.sdk.core.bluetooth.devices.BaseMbtDevice
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.*
import no.nordicsemi.android.ble.BleManager
import timber.log.Timber

class MelomindDeviceImpl(ctx: Context) : BaseMbtDevice(ctx) {

    // required services
    private var deviceInformationService: BluetoothGattService? = null
    private var measurementService: BluetoothGattService? = null

    private var dataReceiver: MbtDataReceiver? = null
    private var deviceStatusCallback: MbtDeviceStatusCallback? = null

    //----------------------------------------------------------------------------
    // MARK: ble manager
    //----------------------------------------------------------------------------
    override fun getGattCallback(): BleManagerGattCallback = MelomindGattCallback()

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
    override fun getDeviceType() = EnumMBTDevice.MELOMIND

    override fun getDeviceInformation(deviceInformationListener: DeviceInformationListener) {
        this.deviceInformationListener = deviceInformationListener

        this.deviceInformation = DeviceInformation().apply {
            this.bleAddress = targetMbtDevice?.bluetoothDevice?.address ?: ""
        }

        val audioNameChar =
            deviceInformationService!!.getCharacteristic(MelomindCharacteristic.AUDIO_NAME.uuid)
        val fwVersionChar =
            deviceInformationService!!.getCharacteristic(MelomindCharacteristic.FIRMWARE_VERSION.uuid)
        val hwVersionChar =
            deviceInformationService!!.getCharacteristic(MelomindCharacteristic.HARDWARE_VERSION.uuid)
        val snChar =
            deviceInformationService!!.getCharacteristic(MelomindCharacteristic.SERIAL_NUMBER.uuid)
        beginAtomicRequestQueue()
            .add(
                readCharacteristic(fwVersionChar)
                    .done {
                        this.deviceInformation.firmwareVersion = fwVersionChar.getStringValue(0)
                    }
            )
            .add(
                readCharacteristic(hwVersionChar)
                    .done {
                        this.deviceInformation.hardwareVersion = hwVersionChar.getStringValue(0)
                    }
            )
            .add(
                readCharacteristic(audioNameChar)
                    .done {
                        this.deviceInformation.audioName =
                            MELOMIND_AUDIO_PREFIX + audioNameChar.getStringValue(0)
                    }
            )
            .add(
                readCharacteristic(snChar)
                    .done {
                        val sn = snChar.getStringValue(0)
                        this.deviceInformation.serialNumber = sn
                        this.deviceInformation.bleName = MELOMIND_BLE_PREFIX + sn
                        this.deviceInformationListener?.onDeviceInformation(deviceInformation)
                    }
                    .fail { _, _ ->
                        this.deviceInformationListener?.onDeviceInformation(deviceInformation)
                    }
            )
            .enqueue()
    }

    override fun enableSensors(
        streamingParams: StreamingParams,
        dataReceiver: MbtDataReceiver,
        deviceStatusCallback: MbtDeviceStatusCallback
    ) {
        if (!streamingParams.isEEGEnabled) {
            deviceStatusCallback.onEEGStatusError(Throwable("EEG can not be disabled for Melomind device!"))
            return
        }
        if (deviceInformationService == null || measurementService == null) {
            deviceStatusCallback.onEEGStatusError(Throwable("Required services not found!"))
            return
        }
        this.dataReceiver = dataReceiver
        this.deviceStatusCallback = deviceStatusCallback
        // disable/enable status trigger operation
        val mailbox =
            measurementService!!.getCharacteristic(MelomindCharacteristic.MAIL_BOX.uuid)
        val data = if (streamingParams.isTriggerStatusEnabled) {
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
                                    this.dataReceiver?.onTriggerStatusConfiguration(
                                        2
                                    )
                                } else {
                                    this.dataReceiver?.onTriggerStatusConfiguration(
                                        size
                                    )
                                }
                            } else {
                                this.dataReceiver?.onTriggerStatusConfiguration(0)
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
                this.dataReceiver?.onEEGFrame(
                    TimedBLEFrame(
                        SystemClock.elapsedRealtime(),
                        eegFrame.value!!
                    )
                )
            } else {
                this.dataReceiver?.onEEGDataError(Throwable("received empty eeg frame!"))
            }
        }

        // configure headset trigger status then enable EEG
        beginAtomicRequestQueue()
            .add(triggerOp)
            .add(
                enableNotifications(eegChar)
                    .done {
                        Timber.d("EEG_ACQUISITION enabled")
                        this.deviceStatusCallback?.onEEGStatusChange(true)
                    }
                    .fail { _, _ ->
                        Timber.e("Could not enable EEG_ACQUISITION")
                        this.deviceStatusCallback?.onEEGStatusError(Throwable("could not start EEG"))
                    }
            )
            .enqueue()
    }

    override fun disableSensors() {
        if (deviceInformationService == null || measurementService == null) {
            deviceStatusCallback?.onEEGStatusError(Throwable("required services not found!"))
            return
        }

        // eeg characteristic
        val eegChar =
            measurementService!!.getCharacteristic(MelomindCharacteristic.EEG_ACQUISITION.uuid)

        disableNotifications(eegChar)
            .done {
                Timber.d("EEG_ACQUISITION disabled")
                deviceStatusCallback?.onEEGStatusChange(false)
            }
            .fail { _, _ ->
                Timber.e("Could not disable EEG_ACQUISITION")
                deviceStatusCallback?.onEEGStatusError(Throwable("could not stop EEG"))
            }
            .enqueue()
    }

    override fun hasA2dpConnectedDevice(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEEGEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isIMSEnabled(): Boolean {
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
    private inner class MelomindGattCallback : BleManagerGattCallback() {

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

    override fun getDeviceSystemStatus(deviceSystemStatusListener: DeviceSystemStatusListener) {
        deviceSystemStatusListener.onDeviceSystemStatusError("not supported yet")
    }

    override fun getSensorStatuses(sensorStatusListener: SensorStatusListener) {
        sensorStatusListener.onSensorStatusError("not supported yet")
    }
}
