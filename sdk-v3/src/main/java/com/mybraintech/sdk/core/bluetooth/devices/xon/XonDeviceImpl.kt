package com.mybraintech.sdk.core.bluetooth.devices.xon

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.SystemClock
import com.mybraintech.sdk.core.acquisition.MbtDeviceStatusCallback
import com.mybraintech.sdk.core.bluetooth.BatteryLevelConversion
import com.mybraintech.sdk.core.bluetooth.MbtBleUtils
import com.mybraintech.sdk.core.bluetooth.devices.BaseMbtDevice
import com.mybraintech.sdk.core.bluetooth.devices.EnumBluetoothConnection
import com.mybraintech.sdk.core.bluetooth.devices.melomind.MelomindCharacteristic
import com.mybraintech.sdk.core.listener.AccelerometerConfigListener
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener
import com.mybraintech.sdk.core.listener.DeviceSystemStatusListener
import com.mybraintech.sdk.core.listener.EEGFilterConfigListener
import com.mybraintech.sdk.core.listener.MbtDataReceiver
import com.mybraintech.sdk.core.listener.SensorStatusListener
import com.mybraintech.sdk.core.model.DeviceInformation
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.MbtDevice
import com.mybraintech.sdk.core.model.StreamingParams
import com.mybraintech.sdk.core.model.TimedBLEFrame
import com.mybraintech.sdk.util.BLE_CONNECTED_STATUS
import timber.log.Timber

class XonDeviceImpl(val ctx: Context) : BaseMbtDevice(ctx) {

    // required services
    private var xonService: BluetoothGattService? = null

    private  val OPTION_24Bit: Byte = 0x01 // 24-Bit transfer supported by amplifier
    private  val OPTION_Bip2Aux: Byte = 0x02

    private var dataReceiver: MbtDataReceiver? = null
    private var deviceStatusCallback: MbtDeviceStatusCallback? = null

    private var _isEEGEnabled: Boolean = false

    var connectionMode = EnumBluetoothConnection.BLE
    //classic Bluetooth

    //----------------------------------------------------------------------------
    // MARK: ble manager
    //----------------------------------------------------------------------------
    override fun getGattCallback(): BleManagerGattCallback = XonGattCallback()
    override fun connectAudio(mbtDevice: MbtDevice, connectionListener: ConnectionListener) {
        //TODO: do nothing
    }


    override fun disconnectAudio(mbtDevice: MbtDevice) {
        //TODO: do nothing
    }

    override fun log(priority: Int, message: String) {
        if (message.contains("Notification received from 0000b2a5")) {
//            Timber.v(message)
        } else {
            Timber.log(priority, message)
        }
    }

    override fun getBatteryLevel(batteryLevelListener: BatteryLevelListener) {
        this.batteryLevelListener = batteryLevelListener

        val batteryLevelChar =
            xonService!!.getCharacteristic(XonCharacteristic.BATTERY.uuid)

        readCharacteristic(batteryLevelChar)
            .done {
                this.batteryLevelListener?.onBatteryLevel(
                    BatteryLevelConversion().parseForMelomind(batteryLevelChar.value[0])
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
    override fun getDeviceType() = EnumMBTDevice.XON

    override fun getDeviceInformation(deviceInformationListener: DeviceInformationListener) {

        this.deviceInformationListener = deviceInformationListener

        this.deviceInformation = DeviceInformation().apply {
            this.bleAddress = targetMbtDevice?.bluetoothDevice?.address ?: ""
        }
        val enable24bit = byteArrayOf( OPTION_24Bit)
        val mailbox =
           xonService!!.getCharacteristic(XonCharacteristic.AMPLIFIER_OPTIONS.uuid)
        val triggerOp =
            writeCharacteristic(mailbox,enable24bit, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        this.deviceInformation.audioName = ""
        this.deviceInformation.firmwareVersion = ""
        this.deviceInformation.hardwareVersion = ""
        val snChar =
            xonService?.getCharacteristic(XonCharacteristic.SERIALNUMBER.uuid)

        val fwChar =
            xonService?.getCharacteristic(XonCharacteristic.VERSION_NUMBER.uuid)
        beginAtomicRequestQueue()
            .add(triggerOp)
            .add(
                readCharacteristic(fwChar)
                    .done {
                        val fwVErsion = fwChar?.value?: ByteArray(4)
                        val hardwareVersion = "${fwVErsion[0].toInt()}.${fwVErsion[1].toInt()}"
                        val fwVersion = "${fwVErsion[2].toInt()}.${fwVErsion[3].toInt()}"
                        this.deviceInformation.hardwareVersion = hardwareVersion
                        this.deviceInformation.firmwareVersion = fwVersion
                    }
            )
            .add(
                readCharacteristic(snChar)
                    .done {
                        val sn = snChar?.getStringValue(0)?:""
                        this.deviceInformation.serialNumber = sn
                        this.deviceInformation.bleName = XON_BLE_PREFIX + sn
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
        Timber.d("Dev_debug enableSensors streamingParams:${streamingParams.isEEGEnabled}")
        if (!streamingParams.isEEGEnabled) {
            deviceStatusCallback.onEEGStatusError(Throwable("EEG can not be disabled for Melomind device!"))
            return
        }
        if (xonService == null) {
            deviceStatusCallback.onEEGStatusError(Throwable("Required services not found!"))
            return
        }
        this.dataReceiver = dataReceiver
        this.deviceStatusCallback = deviceStatusCallback
        // disable/enable status trigger operation
//        val enableTriggerOperation = streamingParams.isTriggerStatusEnabled
//        Timber.w("Dev_debug trigger status enableTriggerOperation  = $enableTriggerOperation")
//        val mailbox =
//            measurementService!!.getCharacteristic(MelomindCharacteristic.MAIL_BOX.uuid)
//        Timber.w("Dev_debug trigger status mailbox  = $mailbox")
//        val data = if (enableTriggerOperation) {
//            EnumMelomindMailBoxCommand.TRIGGER_STATUS.bytes + 0x01
//        } else {
//            EnumMelomindMailBoxCommand.TRIGGER_STATUS.bytes + 0x00
//        }
//        val triggerOp =
//            writeCharacteristic(mailbox, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
//                .done {
//                    Timber.d("Dev_debug Trigger status command sent successfully")
//                    val response = mailbox.value
//                    try {
//                        if (response[0] == EnumMelomindMailBoxCommand.TRIGGER_STATUS.bytes[0]) {
//                            val size = response[1].toInt()
//                            Timber.w("Dev_debug trigger status allocation size = $size")
//                            if (size > 0) {
//                                if (mtu == 47) {
//                                    // bug in firmware v1.7.26 : jira ticket = FM-486
//                                    this.dataReceiver?.onTriggerStatusConfiguration(
//                                        2
//                                    )
//                                } else {
//                                    this.dataReceiver?.onTriggerStatusConfiguration(
//                                        size
//                                    )
//                                }
//                            } else {
//                                this.dataReceiver?.onTriggerStatusConfiguration(0)
//                            }
//                        }
//                    } catch (e: Exception) {
//                        Timber.w("Dev_debug on write character exception:${e.message}")
//                        connectionListener?.onConnectionError(
//                            Throwable(e.message),
//                            MBTErrorCode.BLE_SIGNAL_COULD_NOT_BE_READY
//                        )
//
//                    }
//                }
//                .fail { btdevice, errorCode ->
//                    Timber.e("Dev_debug Fail to write trigger status command:${btdevice?.name} error:$errorCode")
//                    connectionListener?.onConnectionError(
//                        Throwable("Fail to write trigger status command error $errorCode"),
//                        MBTErrorCode.BLE_SIGNAL_COULD_NOT_BE_READY
//                    )
//
//                }

        // eeg characteristic

        Timber.d("Dev_debug eeg characteristic eegChar")
        val eegChar =
            xonService!!.getCharacteristic(XonCharacteristic.DATA.uuid)

        // setup eeg callback

        Timber.d("Dev_debug eeg characteristic eegChar result:$eegChar")
        setNotificationCallback(eegChar).with { _, eegFrame ->
            if (eegFrame.value != null) {

                Timber.d("Dev_debug eeg receive data size:${eegFrame.size()}")
                Timber.d("Dev_debug eeg receive data:${eegFrame.value!!.encodeToHex()}")
                this.dataReceiver?.onEEGFrame(
                    TimedBLEFrame(
                        SystemClock.elapsedRealtime(),
                        eegFrame.value!!
                    )
                )
            } else {
                Timber.w("Dev_debug eeg onEEGDataError  received empty eeg frame!")
                this.dataReceiver?.onEEGDataError(Throwable("received empty eeg frame!"))
            }
        }

        // configure headset trigger status then enable EEG
        beginAtomicRequestQueue()
//            .add(triggerOp)
            .add(
                enableNotifications(eegChar)
                    .done {
                        Timber.d("Dev_debug EEG_ACQUISITION enabled")
                        _isEEGEnabled = true
                        this.deviceStatusCallback?.onEEGStatusChange(true)
                    }
                    .fail { device, errorCode ->
                        Timber.e("Dev_debug Could not enable EEG_ACQUISITION error:$errorCode")
                        this.deviceStatusCallback?.onEEGStatusError(Throwable("could not start EEG"))
                    }
            )
            .enqueue()
    }

    override fun disableSensors() {
        if (xonService == null ) {
            deviceStatusCallback?.onEEGStatusError(Throwable("required services not found!"))
            return
        }

        // eeg characteristic
        val eegChar =
            xonService!!.getCharacteristic(XonCharacteristic.DATA.uuid)

        disableNotifications(eegChar)
            .done {
                Timber.d("Dev_debug EEG_ACQUISITION disabled")
                _isEEGEnabled = false
                deviceStatusCallback?.onEEGStatusChange(false)
            }
            .fail { _, _ ->
                Timber.e("Dev_debug Could not disable EEG_ACQUISITION")
                deviceStatusCallback?.onEEGStatusError(Throwable("could not stop EEG"))
            }
            .enqueue()
    }

    override fun isEEGEnabled(): Boolean = _isEEGEnabled

    override fun handleScanResults(targetName: String, results: List<ScanResult>) {
        val xonDevices = mutableListOf<BluetoothDevice>()
        val otherDevices = mutableListOf<BluetoothDevice>()
        val hasFilter = targetName.isNotEmpty()

        Timber.i("Dev_debug handleScanResults hasFilter:$hasFilter targetName:$targetName")

        for (result in results) {
            val deviceName = result?.device?.name
            Timber.i("Dev_debug handleScanResults deviceName:$deviceName")

            var isXON = false

            if (deviceName!=null && deviceName.contains("X.on")) {
                isXON = true
            } else {
                isXON = false
            }

            if (hasFilter && isXON) {
                if (result.device.name.contains(targetName)) {
                    isXON = true
                } else {
                    //if has the filter. device name doesnt contain. treat it as other device
                    isXON = false
                }
            }
            if (isXON) {
                xonDevices.add(result.device)
            } else {
                otherDevices.add(result.device)
            }
        }

        //if not found could show the scan result with QPLUS



        if (xonDevices.isNotEmpty()) {
            Timber.d("Dev_debug found xon devices : number = ${xonDevices.size}")
            //de scan audio ble device

            scanResultListener.onMbtDevices(xonDevices.map { MbtDevice(it) })
        }
        if (otherDevices.isNotEmpty()) {
            scanResultListener.onOtherDevices(otherDevices)
        }
    }

    //----------------------------------------------------------------------------
    // MARK: private inner class
    //----------------------------------------------------------------------------
    private inner class XonGattCallback : BleManagerGattCallback() {

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val isSupported = true
            Timber.i("Dev_debug isRequiredServiceSupported = $isSupported")
            connectionListener?.onServiceDiscovered("BLE device(MeloMind) discovered")
            xonService = gatt.getService(XonService.XON_SERVICE.uuid)
            Timber.d("Dev_debug xonService = $xonService")
            return isSupported
        }

        override fun initialize() {
            Timber.d("Dev_debug start initialize Melomind")

            if (bluetoothDevice.bondState != BluetoothDevice.BOND_BONDED) {
                // read battery to create bond (an old firmware engineer told me to do this)
                val batteryLevelChar =
                    xonService?.getCharacteristic(XonCharacteristic.BATTERY.uuid)
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
            val bondStatus = bluetoothDevice.bondState
            Timber.i("Dev_debug BleManagerGattCallback onDeviceReady of BLE device with status:$bondStatus")
            if (bondStatus == BluetoothDevice.BOND_BONDED) {

                Timber.i("Dev_debug onDeviceReady in XonGattCallback  BLE_CONNECTED_STATUS called")
                connectionListener?.onDeviceReady(BLE_CONNECTED_STATUS)
            }
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int) {
            Timber.i("Dev_debug onMtuChanged : mtu = $mtu")
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onDeviceDisconnected() {
            Timber.e("Dev_debug onDeviceDisconnected")
            _isEEGEnabled = false
            connectionListener?.onDeviceDisconnected()
        }
    }

    override fun getDeviceSystemStatus(deviceSystemStatusListener: DeviceSystemStatusListener) {
        deviceSystemStatusListener.onDeviceSystemStatusError("not supported yet")
    }

    override fun getSensorStatuses(sensorStatusListener: SensorStatusListener) {
        sensorStatusListener.onSensorStatusError("not supported yet")
    }

    override fun getAccelerometerConfig(accelerometerConfigListener: AccelerometerConfigListener) {
        accelerometerConfigListener.onAccelerometerConfigError("not supported yet")
    }

    override fun getEEGFilterConfig(eegFilterConfigListener: EEGFilterConfigListener) {
        Timber.e("Not yet implemented")
        eegFilterConfigListener.onEEGFilterConfigError("Not yet implemented")
    }
}

fun ByteArray.encodeToHex(): String {
    val hexArray = "0123456789ABCDEF".toCharArray()

    val hexChars = CharArray(this.size * 2)
    for (j in this.indices) {
        val v = this[j].toInt() and 0xFF

        hexChars[j * 2] = hexArray[v ushr 4]
        hexChars[j * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
}