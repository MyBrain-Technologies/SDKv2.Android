package com.mybraintech.sdk.core.bluetooth.devices.melomind

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.SystemClock
import com.mybraintech.sdk.core.acquisition.MBTSession
import com.mybraintech.sdk.core.acquisition.MbtDeviceStatusCallback
import com.mybraintech.sdk.core.bluetooth.BatteryLevelConversion
import com.mybraintech.sdk.core.bluetooth.MbtAudioDeviceInterface
import com.mybraintech.sdk.core.bluetooth.devices.BaseMbtDevice
import com.mybraintech.sdk.core.bluetooth.devices.EnumBluetoothConnection
import com.mybraintech.sdk.core.bluetooth.devices.melomind.DeviceCommandEvent.Companion.CMD_CODE_CONNECT_IN_A2DP_FAILED_ALREADY_CONNECTED
import com.mybraintech.sdk.core.bluetooth.devices.melomind.DeviceCommandEvent.Companion.CMD_CODE_CONNECT_IN_A2DP_FAILED_BAD_BDADDR
import com.mybraintech.sdk.core.bluetooth.devices.melomind.DeviceCommandEvent.Companion.CMD_CODE_CONNECT_IN_A2DP_FAILED_TIMEOUT
import com.mybraintech.sdk.core.bluetooth.devices.melomind.DeviceCommandEvent.Companion.CMD_CODE_CONNECT_IN_A2DP_IN_PROGRESS
import com.mybraintech.sdk.core.bluetooth.devices.melomind.DeviceCommandEvent.Companion.CMD_CODE_CONNECT_IN_A2DP_LINKKEY_INVALID
import com.mybraintech.sdk.core.bluetooth.devices.melomind.DeviceCommandEvent.Companion.CMD_CODE_CONNECT_IN_A2DP_SUCCESS
import com.mybraintech.sdk.core.encodeToHex
import com.mybraintech.sdk.core.listener.AccelerometerConfigListener
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener
import com.mybraintech.sdk.core.listener.DeviceSystemStatusListener
import com.mybraintech.sdk.core.listener.EEGFilterConfigListener
import com.mybraintech.sdk.core.listener.MbtDataReceiver
import com.mybraintech.sdk.core.listener.ScanResultListener
import com.mybraintech.sdk.core.listener.SensorStatusListener
import com.mybraintech.sdk.core.model.DeviceInformation
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.MBTErrorCode
import com.mybraintech.sdk.core.model.MbtDevice
import com.mybraintech.sdk.core.model.StreamingParams
import com.mybraintech.sdk.core.model.TimedBLEFrame
import com.mybraintech.sdk.util.AUDIO_CONNECTED_STATUS
import com.mybraintech.sdk.util.BLE_CONNECTED_STATUS
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data
import timber.log.Timber

class MelomindDeviceImpl(val ctx: Context) : BaseMbtDevice(ctx) {

    // required services
    private var deviceInformationService: BluetoothGattService? = null
    private var measurementService: BluetoothGattService? = null

    private var dataReceiver: MbtDataReceiver? = null
    private var deviceStatusCallback: MbtDeviceStatusCallback? = null

    private var _isEEGEnabled: Boolean = false

    var connectionMode = EnumBluetoothConnection.BLE
    //classic Bluetooth

    //----------------------------------------------------------------------------
    // MARK: ble manager
    //----------------------------------------------------------------------------
    override fun getGattCallback(): BleManagerGattCallback = MelomindGattCallback()
    override fun connectAudio(mbtDevice: MbtDevice, connectionListener: ConnectionListener) {
        val currentThread = Thread.currentThread()
        Timber.d("Dev_debug connectAudio:${mbtDevice.bluetoothDevice.name} connectionListener:$connectionListener Current thread: ${currentThread.name}")
        this.connectionListener = connectionListener
        val pairedDevices: MutableSet<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        var found = false
        if (pairedDevices != null) {
            for (device in pairedDevices) {
                val deviceName = device.name
                val macAddress = device.address
                // Do something with the device (e.g., display in a list)
                Timber.d("Dev_debug connectAudio Paired device: $deviceName at $macAddress")
                var targetAudioname = mbtDevice.bluetoothDevice.name
                Timber.d("Dev_debug tobe connectAudio o Paired targetAudioname:$targetAudioname")
                if (deviceName.equals(targetAudioname)) {
                    found = true
                    break
                }
            }

        }
        Timber.d("Dev_debug tobe connectAudio found:$found")
        if (found) {
            //if device already bond (paired device list). do the connection again
            connectUsingBluetoothA2dpBinder(mbtDevice.bluetoothDevice)
        } else {
            //request pair new device
            mbtDevice.bluetoothDevice.createBond()
        }
    }


    override fun disconnectAudio(bluetoothDevice: BluetoothDevice?) {
        disConnectUsingBluetoothA2dpBinder(bluetoothDevice)

    }
    override fun startScanAudio(targetName: String, scanResultListener: ScanResultListener?) {
        if (MBTSession.forceScanAudioOnly) {
            Timber.d("Dev_debug startScanAudio called(SDK) forceScanAudioOnly")
            startDiscovery()
        } else {
            Timber.d("%s%s", "Dev_debug startScanAudio called(SDK) targetName:", targetName)
            val pairedDevices: MutableSet<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            var found = false
            var foundedDevice: BluetoothDevice? = null
            var hasFilter =
                targetName.isNotEmpty() && targetName.length > 2
            Timber.d("%s%s", "Dev_debug startScanAudio called(SDK) hasFilter:", hasFilter)
            if (pairedDevices != null) {
                for (device in pairedDevices) {
                    val deviceName = device.name
                    val macAddress = device.address
                    // Do something with the device (e.g., display in a list)
                    Timber.d("Dev_debug Paired device: $deviceName at $macAddress")
                    if (deviceName.contains("MM")) {
                        if (hasFilter) {
                            if (deviceName.equals(targetName)) {
                                found = true
                                foundedDevice = device
                                break
                            }
                        } else {
                            found = true
                            foundedDevice = device
                            break
                        }
                    }
                }
            }
            if (foundedDevice != null && found) {
                Timber.d("Dev_debug found paired device")
                scanResultListener?.onMbtDevices(listOf(MbtDevice(foundedDevice)))

            } else {
                setupAudioBTBroadcast(context, object : MbtAudioDeviceInterface {
                    override fun onMbtAudioDeviceFound(
                        device: BluetoothDevice,
                        action: String,
                        state: Int
                    ) {
                        val deviceName = device.name
                        if (deviceName.startsWith("MM")) {
                            if (hasFilter) {
                                if (deviceName.equals(targetName)) {
                                    scanResultListener?.onMbtDevices(listOf(MbtDevice(device)))
                                }
                            } else {
                                scanResultListener?.onMbtDevices(listOf(MbtDevice(device)))
                            }
                        } else {
                            scanResultListener?.onOtherDevices(listOf(device))
                        }
                    }

                })
                startDiscovery()
            }
        }
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
            measurementService!!.getCharacteristic(MelomindCharacteristic.BATTERY_LEVEL.uuid)

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

    override fun scanConnectedA2DP() {
        var connectedDevices = a2dp?.connectedDevices ?: arrayListOf()
        val connectedDeviceSize = connectedDevices.size
        var founded = false

        val bleName = deviceInformation.bleName
        val audioName = deviceInformation.audioName

        Timber.d("Dev_debug connectAudioViaBle check connect a2dp list size is:${connectedDeviceSize}")
        Timber.d("Dev_debug connectAudioViaBle bleName:${bleName} audioName:${audioName}")
        if (connectedDeviceSize > 0) {
            for (device in connectedDevices) {
                val deviceName = device.name
                if (deviceName.equals(bleName) || deviceName.equals(audioName)) {
                    Timber.d("Dev_debug connectAudioViaBle  a2dp connectedDevice name:${device.name}")
                    audioDevice = device
                    founded = true
                    break
                } else if (deviceName.startsWith("ml") || deviceName.startsWith("MM")) {
                    audioDevice = device
                    founded = true
                    break
                }
            }
        }

        if (founded) {
            audioBleConnected = true
        } else {
            audioBleConnected = false
            audioDevice = null
        }
    }

    //----------------------------------------------------------------------------
    // MARK: internal ble manager
    //----------------------------------------------------------------------------
    override fun getDeviceType() = EnumMBTDevice.MELOMIND
    fun rebootHeadset() {
        Timber.i("Dev_debug  rebootHeadset called")
        val mailbox =
            measurementService!!.getCharacteristic(MelomindCharacteristic.MAIL_BOX.uuid)
        val rebootData = DeviceCommandEvent.MBX_SYS_REBOOT_EVT.assembledCodes

        val triggerOp =
            writeCharacteristic(mailbox, rebootData, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                .done {
                    Timber.d("Dev_debug Trigger rebootHeadset command sent successfully")
                    audioBleConnected = false
                }
                .fail { btdevice, errorCode ->
                    connectionListener?.let { handleCharacteriseActionFailed(errorCode, it) }
                    Timber.d("Dev_debug Fail to write trigger rebootHeadset command:${btdevice?.name} error:$errorCode")
                }
//        beginAtomicRequestQueue()
//                .add(triggerOp) .enqueue()
    }

    fun disconnectAudioViaBLE() {
        val isConnected = audioBleConnected
        Timber.i("Dev_debug  disconnectAudioViaBLE : isConnected = $isConnected")
        if (isConnected) {

//            disConnectUsingBluetoothA2dpBinder(targetMbtDevice?.bluetoothDevice)
            val mailbox =
                measurementService!!.getCharacteristic(MelomindCharacteristic.MAIL_BOX.uuid)
            val disconnectData = DeviceCommandEvent.MBX_DISCONNECT_IN_A2DP.assembledCodes

            val triggerOp =
                writeCharacteristic(
                    mailbox,
                    disconnectData,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                    .done {
                        Timber.d("Dev_debug Trigger disconnectAudioViaBLE command sent successfully")
                        audioBleConnected = false
                        connectionListener?.onAudioDisconnected()
                    }
                    .fail { btdevice, errorCode ->
                        Timber.d("Dev_debug Fail to write trigger disconnectAudioViaBLE command:${btdevice?.name} error:$errorCode")
                        if (errorCode == 3) {
                            //scan audio instance
                            //disconnect using classic method
                            disconnectAudio(audioDevice)
//                            asdasdasd
                        } else {
                            connectionListener?.let {
                                handleCharacteriseActionFailed(
                                    errorCode,
                                    it
                                )
                            }
                        }

                    }
            beginAtomicRequestQueue()
                .add(triggerOp).enqueue()
        } else {
            Timber.e("Dev_debug  disconnectAudioViaBLE no device to connect")
        }
    }

    fun connectAudioViaBle() {


        scanConnectedA2DP()
        if (audioBleConnected) {
            connectionListener?.onDeviceReady(AUDIO_CONNECTED_STATUS)
        } else {


            val mailbox =
                measurementService!!.getCharacteristic(MelomindCharacteristic.MAIL_BOX.uuid)
            Timber.d("Dev_debug connectAudioViaBle status mailbox  = $mailbox")
//        val data = byteArrayOf(0x11.toByte(),0x25.toByte(), 0xA2.toByte())
            val data = DeviceCommandEvent.MBX_CONNECT_IN_A2DP.assembledCodes

            val triggerOp =
                writeCharacteristic(mailbox, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    .done {
                        Timber.d("Dev_debug Trigger connectAudioViaBle command sent successfully")

                    }
                    .fail { btdevice, errorCode ->
                        Timber.d("Dev_debug Fail to write trigger connectAudioViaBle command:${btdevice?.name} error:$errorCode")


                    }

            setNotificationCallback(mailbox).with(object : DataReceivedCallback {
                override fun onDataReceived(p0: BluetoothDevice, p1: Data) {
                    Timber.i("Dev_debug connectAudioViaBle onDataReceived data p1  = ${p1.value?.encodeToHex()}")
                    //remove the firstbyte since it's the identifer byte
                    val rawData = p1.value
                    rawData?.let { it ->
                        val identifier = it.first().toUInt()
                        val a2dpIdentifier =
                            DeviceCommandEvent.MBX_CONNECT_IN_A2DP.identifierCode.toUInt()
                        Timber.d("Dev_debug connectAudioViaBle onDataReceived identifier  = ${identifier} a2dpIdentifier:$a2dpIdentifier")
                        if (identifier == a2dpIdentifier) {
                            val response = it.copyOfRange(1, it.size)
                            val connectedByte =
                                DeviceCommandEvent.MBX_CONNECT_IN_A2DP.getResponseCodeForKey(
                                    CMD_CODE_CONNECT_IN_A2DP_SUCCESS
                                )

                            val connectionInProgressByte =
                                DeviceCommandEvent.MBX_CONNECT_IN_A2DP.getResponseCodeForKey(
                                    CMD_CODE_CONNECT_IN_A2DP_IN_PROGRESS
                                )

                            val connectionBadBoardByte =
                                DeviceCommandEvent.MBX_CONNECT_IN_A2DP.getResponseCodeForKey(
                                    CMD_CODE_CONNECT_IN_A2DP_FAILED_BAD_BDADDR
                                )
                            val connectionLinkInvalidKeydByte =
                                DeviceCommandEvent.MBX_CONNECT_IN_A2DP.getResponseCodeForKey(
                                    CMD_CODE_CONNECT_IN_A2DP_LINKKEY_INVALID
                                )

                            val connectionTimeoutByte =
                                DeviceCommandEvent.MBX_CONNECT_IN_A2DP.getResponseCodeForKey(
                                    CMD_CODE_CONNECT_IN_A2DP_FAILED_TIMEOUT
                                )
                            val alreadyConnectedByte =
                                DeviceCommandEvent.MBX_CONNECT_IN_A2DP.getResponseCodeForKey(
                                    CMD_CODE_CONNECT_IN_A2DP_FAILED_ALREADY_CONNECTED
                                )

                            Timber.d("Dev_debug connectAudioViaBle response  = ${response.encodeToHex()}")
                            Timber.d(
                                "Dev_debug connectAudioViaBle responseStats= ${
                                    byteArrayOf(
                                        connectedByte
                                    ).encodeToHex()
                                }"
                            )

                            val isSuccess = response.contentEquals(byteArrayOf(connectedByte))
                            val isBadBoard = response.contentEquals(byteArrayOf(connectionBadBoardByte))
                            val isLinkInvalidKey = response.contentEquals(byteArrayOf(connectionLinkInvalidKeydByte))
                            val alreadyConnected =
                                response.contentEquals(byteArrayOf(alreadyConnectedByte))
                            val isInprogress =
                                response.contentEquals(byteArrayOf(connectionInProgressByte))
                            val isInTimeOut =
                                response.contentEquals(byteArrayOf(connectionTimeoutByte))

                            Timber.d("Dev_debug connectAudioViaBle isSuccess  = ${isSuccess}")
                            if (isSuccess || alreadyConnected) {
                                //AudioConnected
                                audioBleConnected = true
                                connectionListener?.onDeviceReady(AUDIO_CONNECTED_STATUS)

                            } else if (!isInprogress && !isInTimeOut && !isBadBoard && !isLinkInvalidKey) {
                                //audio connected failed
                                connectionListener?.onConnectionError(
                                    Throwable("connectAudioViaBle failed"),
                                    MBTErrorCode.FAILED_TO_CONNECT_TO_AUDIO_DEVICE
                                )
                            } else {
                                //connection is in progress
                                Handler().postDelayed({
                                    connectAudioViaBle()
                                }, 2000)
                                Timber.d("Dev_debug connectAudioViaBle is in progress")

                            }
                        }
                    }


                }

            })

            beginAtomicRequestQueue()
                .add(triggerOp).add(
                    enableNotifications(mailbox)
                        .done {
                            Timber.d("Dev_debug enableNotifications mailbox")
                        }
                        .fail { device, errorCode ->
                            Timber.e("Dev_debug Could not enable enableNotifications mailbox error:$errorCode")
                            connectionListener?.let {
                                handleCharacteriseActionFailed(
                                    errorCode,
                                    it
                                )
                            }
                        }
                ).enqueue()
        }
    }

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
                        val orgAudioName = audioNameChar.getStringValue(0)
                        if (!orgAudioName.uppercase().contains("MM")) {
                            this.deviceInformation.audioName =
                                MELOMIND_AUDIO_PREFIX + orgAudioName
                        } else {
                            this.deviceInformation.audioName = orgAudioName
                        }
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

    fun handleCharacteriseActionFailed(errorCode: Int, connectionListener: ConnectionListener) {
        if (errorCode == 3) {
            scanConnectedA2DP()

            disconnect().enqueue()
            disconnectAudio(audioDevice)
            audioBleConnected = false
            audioDevice = null
            connectionListener.onConnectionError(
                Throwable("Write permission error please restart headset"),
                MBTErrorCode.DEVICE_WRITE_PERMISSION_ERROR
            )
        } else if (errorCode == 2) {
            connectionListener.onConnectionError(
                Throwable("read permission error"),
                MBTErrorCode.DEVICE_READ_PERMISSION_ERROR
            )
        } else {
            connectionListener.onConnectionError(
                Throwable("Fail to write trigger status command error $errorCode"),
                MBTErrorCode.BLE_SIGNAL_COULD_NOT_BE_READY
            )
        }
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
        if (deviceInformationService == null || measurementService == null) {
            deviceStatusCallback.onEEGStatusError(Throwable("Required services not found!"))
            return
        }
        this.dataReceiver = dataReceiver
        this.deviceStatusCallback = deviceStatusCallback
        // disable/enable status trigger operation
        val enableTriggerOperation = streamingParams.isTriggerStatusEnabled
        Timber.w("Dev_debug trigger status enableTriggerOperation  = $enableTriggerOperation")
        val mailbox =
            measurementService!!.getCharacteristic(MelomindCharacteristic.MAIL_BOX.uuid)
        Timber.w("Dev_debug trigger status mailbox  = $mailbox")
        val data = if (enableTriggerOperation) {
            EnumMelomindMailBoxCommand.TRIGGER_STATUS.bytes + 0x01
        } else {
            EnumMelomindMailBoxCommand.TRIGGER_STATUS.bytes + 0x00
        }
        val triggerOp =
            writeCharacteristic(mailbox, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                .done {
                    Timber.d("Dev_debug Trigger status command sent successfully")
                    val response = mailbox.value
                    try {
                        if (response[0] == EnumMelomindMailBoxCommand.TRIGGER_STATUS.bytes[0]) {
                            val size = response[1].toInt()
                            Timber.w("Dev_debug trigger status allocation size = $size")
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
                        Timber.w("Dev_debug on write character exception:${e.message}")
                        connectionListener?.onConnectionError(
                            Throwable(e.message),
                            MBTErrorCode.BLE_SIGNAL_COULD_NOT_BE_READY
                        )

                    }
                }
                .fail { btdevice, errorCode ->
                    Timber.e("Dev_debug Fail to write trigger status command:${btdevice?.name} error:$errorCode")
                    connectionListener?.let { handleCharacteriseActionFailed(errorCode, it) }


                }

        // eeg characteristic

        Timber.d("Dev_debug eeg characteristic eegChar")
        val eegChar =
            measurementService!!.getCharacteristic(MelomindCharacteristic.EEG_ACQUISITION.uuid)

        // setup eeg callback

        Timber.d("Dev_debug eeg characteristic eegChar result:$eegChar")
        setNotificationCallback(eegChar).with { _, eegFrame ->
            if (eegFrame.value != null) {
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
            .add(triggerOp)
            .add(
                enableNotifications(eegChar)
                    .done {
                        Timber.d("Dev_debug EEG_ACQUISITION enabled")
                        _isEEGEnabled = true
                        this.deviceStatusCallback?.onEEGStatusChange(true)
                    }
                    .fail { device, errorCode ->
                        Timber.e("Dev_debug Could not enable EEG_ACQUISITION error:$errorCode")
                        connectionListener?.let { handleCharacteriseActionFailed(errorCode, it) }
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
        val melomindDevices = mutableListOf<BluetoothDevice>()
        val otherDevices = mutableListOf<BluetoothDevice>()
        val hasFilter = targetName.isNotEmpty()

        Timber.i("Dev_debug handleScanResults hasFilter:$hasFilter targetName:$targetName")

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
            if (hasFilter && isMelomind) {
                if (result.device.name.contains(targetName)) {
                    isMelomind = true
                } else {
                    //if has the filter. device name doesnt contain. treat it as other device
                    isMelomind = false
                }
            }
            if (isMelomind) {
                melomindDevices.add(result.device)
            } else {
                otherDevices.add(result.device)
            }
        }
        if (melomindDevices.isNotEmpty()) {
            Timber.d("Dev_debug found melomind devices : number = ${melomindDevices.size}")
            //de scan audio ble device

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
            connectionListener?.onServiceDiscovered("BLE device(MeloMind) discovered")
            deviceInformationService = gatt.getService(MelomindService.DEVICE_INFORMATION.uuid)
            measurementService = gatt.getService(MelomindService.MEASUREMENT.uuid)
            val isSupported = (deviceInformationService != null && measurementService != null)
            Timber.i("Dev_debug isRequiredServiceSupported = $isSupported")

//            val audioNameChar =
//                deviceInformationService!!.getCharacteristic(MelomindCharacteristic.AUDIO_NAME.uuid)
//
//            beginAtomicRequestQueue()
//
//                .add(
//                    readCharacteristic(audioNameChar)
//                        .done {
//                            val targetDeviceAudio = audioNameChar.getStringValue(0)
//                            Timber.i("isRequiredServiceSupported readCharacteristic MelomindGattCallback audioNameChar 2 = ${targetDeviceAudio}")
//                            if (connectionMode == EnumBluetoothConnection.BLE_AUDIO) {
//                                startBluetoothScanning(
//                                    targetDeviceAudio,
//                                    "in MelomindGattCallback isRequiredServiceSupported audioname callback"
//                                )
//                            }
//                        }
//                )
//                .enqueue()


            return isSupported
        }

        override fun initialize() {
            Timber.d("Dev_debug start initialize Melomind")

            if (bluetoothDevice.bondState != BluetoothDevice.BOND_BONDED) {
                // read battery to create bond (an old firmware engineer told me to do this)

                Timber.i("Dev_debug read battery to create bond work around")
                val batteryLevelChar =
                    measurementService!!.getCharacteristic(MelomindCharacteristic.BATTERY_LEVEL.uuid)
                readCharacteristic(batteryLevelChar)
                    .enqueue()
            }

            beginAtomicRequestQueue()
                .add(
                    requestMtu(MTU_SIZE)
                        .done { Timber.d("requestMtu done") }
                        .fail { _, errorCode ->
                            Timber.e("Could not requestMtu errorCode:$errorCode")
                            connectionListener?.let {
                                handleCharacteriseActionFailed(
                                    errorCode,
                                    it
                                )
                            }
                        }
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

                Timber.i("Dev_debug onDeviceReady in MelomindGattCallback  BLE_CONNECTED_STATUS called")
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
