package com.mybraintech.sdk.core.bluetooth.devices.qplus

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.mybraintech.sdk.core.acquisition.MbtDeviceStatusCallback
import com.mybraintech.sdk.core.bluetooth.devices.BaseMbtDevice
import com.mybraintech.sdk.core.bluetooth.devices.EnumBluetoothConnection
import com.mybraintech.sdk.core.bluetooth.devices.Indus5MailboxDecoder
import com.mybraintech.sdk.core.bluetooth.devices.xon.encodeToHex
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.*
import com.mybraintech.sdk.util.BLE_CONNECTED_STATUS
import no.nordicsemi.android.ble.Operation
import no.nordicsemi.android.ble.WriteRequest
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data
import timber.log.Timber
import java.nio.charset.Charset


abstract class Indus5DeviceImpl(ctx: Context) :
    BaseMbtDevice(ctx), DataReceivedCallback {

    private var streamingParams: StreamingParams? = null
    private var dataReceiver: MbtDataReceiver? = null
    private var deviceStatusCallback: MbtDeviceStatusCallback? = null

    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    private var _isEEGEnabled = false

    //----------------------------------------------------------------------------
    // MARK: ble manager
    //----------------------------------------------------------------------------
    override fun getGattCallback(): BleManagerGattCallback = Indus5GattCallback(this)
    override fun connectAudio(mbtDevice: MbtDevice, connectionListener: ConnectionListener) {
        // TODO("Not yet implemented")
    }

    override fun disconnectAudio(mbtDevice: MbtDevice) {
        //
    }
    override fun log(priority: Int, message: String) {
        if (message.contains("value: (0x) 40")
            || message.contains("value: (0x) 50")
            || message.contains("value: (0x) 60")
        ) {
//            Timber.v(message)
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
            Timber.v("onDataReceived size:${data.value?.size} hex:${data.value?.encodeToHex()}")
            when (val response = Indus5MailboxDecoder.decodeRawIndus5Response(data.value!!)) {
                is Indus5Response.MtuChange -> {
                    Timber.i("Mailbox MTU changed successfully")
                }
                is Indus5Response.BatteryLevel -> {
                    batteryLevelListener?.onBatteryLevel(response.percent)
                }
                is Indus5Response.FirmwareVersion -> {
                    deviceInformation.firmwareVersion = response.version
                }
                is Indus5Response.HardwareVersion -> {
                    deviceInformation.hardwareVersion = response.version
                }
                is Indus5Response.GetSerialNumber -> {
                    deviceInformation.serialNumber = response.serialNumber
                    deviceInformation.bleName = INDUS5_BLE_PREFIX + response.serialNumber
                }
                is Indus5Response.AudioNameFetched -> {
                    val audioName = INDUS5_AUDIO_PREFIX + response.audioName
//                    startBluetoothScanning(audioName,"call from onDataReceived Indus5DeviceImpl")
                    deviceInformation.audioName = audioName
                }
                is Indus5Response.AudioNameChanged -> {
                    deviceInformation.audioName = INDUS5_AUDIO_PREFIX + response.newAudioName
                    audioNameListener?.onAudioNameChanged(deviceInformation.audioName)
                }
                is Indus5Response.EEGStatus -> {
                    _isEEGEnabled = response.isEnabled
                    deviceStatusCallback?.onEEGStatusChange(response.isEnabled)
                }
                is Indus5Response.EEGFrame -> {
                    dataReceiver?.onEEGFrame(
                        TimedBLEFrame(
                            SystemClock.elapsedRealtime(),
                            response.data
                        )
                    )
                }
                is Indus5Response.TriggerStatusConfiguration -> {
                    dataReceiver?.onTriggerStatusConfiguration(response.triggerStatusAllocationSize)
                }
                is Indus5Response.ImsStatus -> {
                    deviceStatusCallback?.onIMSStatusChange(response.isEnabled)
                }
                is Indus5Response.ImsFrame -> {
                    dataReceiver?.onAccelerometerFrame(response.data)
                }
                is Indus5Response.SerialNumberChanged -> {
                    serialNumberChangedListener?.onSerialNumberChanged(response.newSerialNumber)
                }
                is Indus5Response.GetDeviceSystemStatus -> {
                    deviceSystemStatusListener?.onDeviceSystemStatusFetched(response.deviceSystemStatus)
                }
                is Indus5Response.GetSensorStatuses -> {
                    sensorStatusListener?.onSensorStatusFetched(response.sensorStatuses)
                }
                is Indus5Response.GetIMSConfig -> {
                    accelerometerConfigListener?.onAccelerometerConfigFetched(response.accelerometerConfig)
                }
                is Indus5Response.SetIMSConfig -> {
                    dataReceiver?.onAccelerometerConfiguration(response.accelerometerConfig)
                }
                is Indus5Response.GetEEGFilterConfig -> {
                    eegFilterConfigListener?.onEEGFilterConfig(response.config)
                }
                is Indus5Response.SetEEGFilterConfig -> {
                    dataReceiver?.onEEGFilterConfig(response.appliedConfig)
                }
                else -> {
                    Timber.e("this type is not supported : ${response.javaClass.simpleName}")
                }
            }
        } else {
            Timber.e("data value is null")
        }
    }

    //----------------------------------------------------------------------------
    // MARK: internal ble manager
    //----------------------------------------------------------------------------
    override fun connectMbt(mbtDevice: MbtDevice, connectionListener: ConnectionListener, connectionMode:EnumBluetoothConnection) {
        /**
         * remove bond to fix data loss bug on Android 9 (SDK-415).
         * We do not apply this for Android 10 and later since this will show a popup to ask
         * user permission each time.
         */
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Timber.w("removeBond and sleep 200 ms to avoid false bonding notification")
            removeBond(mbtDevice.bluetoothDevice)
            Handler(Looper.getMainLooper()).postDelayed({
                super.connectMbt(mbtDevice, connectionListener,connectionMode)
            }, 200)
        } else {
            super.connectMbt(mbtDevice, connectionListener,connectionMode)
        }
    }

    override fun getDeviceType() = EnumMBTDevice.Q_PLUS

    override fun getDeviceInformation(deviceInformationListener: DeviceInformationListener) {
        this.deviceInformation = DeviceInformation().apply {
            this.bleAddress = targetMbtDevice?.bluetoothDevice?.address ?: ""
        }
        this.deviceInformationListener = deviceInformationListener
        beginAtomicRequestQueue()
            .add(getDeviceNameMailboxRequest())
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

    override fun setSerialNumber(serialNumber: String, listener: SerialNumberChangedListener?) {
        if (isConnectedAndReady()) {
            if (serialNumber.length != 10 && serialNumber.length != 11) {
                listener?.onSerialNumberError("Serial number length must be 10 or 11")
                return
            }
            this.serialNumberChangedListener = listener
            getSetSerialNumberMailboxRequest(serialNumber)
                .fail { _, failReason ->
                    this.serialNumberChangedListener?.onSerialNumberError("Error code = $failReason")
                }
                .enqueue()
        } else {
            listener?.onSerialNumberError("Device is not connected or is not ready")
        }
    }

    override fun setAudioName(audioName: String, listener: AudioNameListener?) {
        if (isConnectedAndReady()) {
            if (audioName.length <= 11) {
                this.audioNameListener = listener
                getSetAudioNameRequest(audioName)
                    .fail { _, failReason ->
                        this.audioNameListener?.onAudioNameError("Error code = $failReason")
                    }
                    .enqueue()
            } else {
                listener?.onAudioNameError("Name length is not valid")
            }
        } else {
            listener?.onAudioNameError("Device is not connected or is not ready")
        }
    }

    private fun getDeviceSystemStatusRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            EnumIndus5FrameSuffix.MBX_SYS_GET_STATUS.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    override fun enableSensors(
        streamingParams: StreamingParams,
        dataReceiver: MbtDataReceiver,
        deviceStatusCallback: MbtDeviceStatusCallback
    ) {
        this.streamingParams = streamingParams
        this.dataReceiver = dataReceiver
        this.deviceStatusCallback = deviceStatusCallback

        val requestQueue = beginAtomicRequestQueue()
        if (streamingParams.isEEGEnabled) {
            requestQueue.add(setFilterConfigRequest(streamingParams.filterConfig))
            requestQueue.add(getTriggerStatusOperation(streamingParams.isTriggerStatusEnabled))
            requestQueue.add(getStartEEGOperation())
        } else {
            requestQueue.add(getStopEEGOperation())
        }
        if (streamingParams.isAccelerometerEnabled) {
            requestQueue.add(setAccelerometerConfigRequest(streamingParams.accelerometerSampleRate))
            requestQueue.add(getStartIMSOperation())
        } else {
            requestQueue.add(getStopIMSOperation())
        }
        requestQueue.enqueue()
    }

    override fun disableSensors() {
        val requestQueue = beginAtomicRequestQueue()
        if (streamingParams?.isAccelerometerEnabled == true) {
            requestQueue.add(getStopIMSOperation())
        }
        if (streamingParams?.isEEGEnabled == true) {
            requestQueue.add(getStopEEGOperation())
        }
        requestQueue.enqueue()
    }

    override fun isEEGEnabled(): Boolean = _isEEGEnabled

    private fun getFilterConfigRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            EnumIndus5FrameSuffix.MBX_GET_FILTER_CONFIG_TYPE.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun setFilterConfigRequest(config: EnumEEGFilterConfig): WriteRequest {
        val command = EnumIndus5FrameSuffix.MBX_SET_FILTER_CONFIG_TYPE.bytes + config.byteCode
        return writeCharacteristic(
            txCharacteristic,
            command,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun setAccelerometerConfigRequest(sampleRate: EnumAccelerometerSampleRate): WriteRequest {
        val operationByte = EnumIndus5FrameSuffix.MBX_SET_IMS_CONFIG.bytes
        val sampleRateByte: Byte = sampleRate.mailboxValue
        val enableAxisByte: Byte = 0x07 // Default value: 0x07 : all axis are enabled
        val fullScaleByte: Byte = 0x00 // Default value: 0x00 : ±2g
        val command = operationByte + sampleRateByte + enableAxisByte + fullScaleByte
        return writeCharacteristic(
            txCharacteristic,
            command,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getAccelerometerConfigRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            EnumIndus5FrameSuffix.MBX_GET_IMS_CONFIG.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getStartIMSOperation(): Operation {
        return writeCharacteristic(
            txCharacteristic,
            EnumIndus5FrameSuffix.MBX_START_IMS_ACQUISITION.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getStopIMSOperation(): Operation {
        return writeCharacteristic(
            txCharacteristic,
            EnumIndus5FrameSuffix.MBX_STOP_IMS_ACQUISITION.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getTriggerStatusOperation(isTriggerStatusEnabled: Boolean): Operation {
        // create status operation
        val statusCommand = EnumIndus5FrameSuffix.MBX_P300_ENABLE.bytes.toMutableList()
        statusCommand.add(
            if (isTriggerStatusEnabled) {
                0x01
            } else {
                0x00
            }
        )
        return writeCharacteristic(
            txCharacteristic,
            statusCommand.toByteArray(),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getStartEEGOperation(): Operation {
        return writeCharacteristic(
            txCharacteristic,
            EnumIndus5FrameSuffix.MBX_START_EEG_ACQUISITION.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getStopEEGOperation(): Operation {
        return writeCharacteristic(
            txCharacteristic,
            EnumIndus5FrameSuffix.MBX_STOP_EEG_ACQUISITION.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getSensorStatusRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            EnumIndus5FrameSuffix.MBX_GET_SENSOR_STATUS.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    override fun getSensorStatuses(sensorStatusListener: SensorStatusListener) {
        if (isConnectedAndReady()) {
            this.sensorStatusListener = sensorStatusListener
            getSensorStatusRequest()
                .fail { _, errorCode ->
                    this.sensorStatusListener?.onSensorStatusError("errorCode = $errorCode")
                }
                .enqueue()
        } else {
            sensorStatusListener.onSensorStatusError("device is not connected or not ready")
        }
    }

    override fun getDeviceSystemStatus(deviceSystemStatusListener: DeviceSystemStatusListener) {
        if (isConnectedAndReady()) {
            this.deviceSystemStatusListener = deviceSystemStatusListener
            getDeviceSystemStatusRequest()
                .fail { _, errorCode ->
                    this.deviceSystemStatusListener?.onDeviceSystemStatusError("errorCode = $errorCode")
                }
                .enqueue()
        } else {
            deviceSystemStatusListener.onDeviceSystemStatusError("device is not connected or not ready")
        }
    }

    override fun getEEGFilterConfig(eegFilterConfigListener: EEGFilterConfigListener) {
        this.eegFilterConfigListener = eegFilterConfigListener
        getFilterConfigRequest()
            .fail { _, errorCode ->
                eegFilterConfigListener.onEEGFilterConfigError("errorCode=$errorCode")
            }
            .enqueue()
    }

    override fun getAccelerometerConfig(accelerometerConfigListener: AccelerometerConfigListener) {
        this.accelerometerConfigListener = accelerometerConfigListener
        getAccelerometerConfigRequest()
            .fail { _, errorCode ->
                accelerometerConfigListener.onAccelerometerConfigError("errorCode=$errorCode")
            }
            .enqueue()
    }

    private fun getMtuMailboxRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            generateMtuChangeCommand(MTU_SIZE),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun generateMtuChangeCommand(mtuSize: Int = 47): ByteArray {
        val result = EnumIndus5FrameSuffix.MBX_TRANSMIT_MTU_SIZE.bytes.toMutableList()
        result.add(mtuSize.toByte())
        return result.toByteArray()
    }

    private fun getBatteryLevelMailboxRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            EnumIndus5FrameSuffix.MBX_GET_BATTERY_VALUE.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    @Suppress("unused")
    private fun getDeviceNameMailboxRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            EnumIndus5FrameSuffix.MBX_GET_DEVICE_NAME.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getFirmwareVersionMailboxRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            EnumIndus5FrameSuffix.MBX_GET_FIRMWARE_VERSION.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getHardwareVersionMailboxRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            EnumIndus5FrameSuffix.MBX_GET_HARDWARE_VERSION.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getSerialNumberMailboxRequest(): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            EnumIndus5FrameSuffix.MBX_GET_SERIAL_NUMBER.bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getSetSerialNumberMailboxRequest(newSerialNumber: String): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            buildSetSerialNumberCommand(newSerialNumber),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun getSetAudioNameRequest(audioName: String): WriteRequest {
        return writeCharacteristic(
            txCharacteristic,
            buildSetAudioNameCommand(audioName),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    private fun buildSetSerialNumberCommand(newSerialNumber: String): ByteArray {
        val command = EnumIndus5FrameSuffix.MBX_SET_SERIAL_NUMBER.bytes.toMutableList()
        command.addAll(newSerialNumber.toByteArray(Charset.defaultCharset()).toMutableList())
        return command.toByteArray()
    }

    private fun buildSetAudioNameCommand(audioName: String): ByteArray {
        val command = EnumIndus5FrameSuffix.MBX_SET_A2DP_NAME.bytes.toMutableList()
        command.addAll(audioName.toByteArray(Charset.defaultCharset()).toMutableList())
        return command.toByteArray()
    }

    //----------------------------------------------------------------------------
    // MARK: private inner class
    //----------------------------------------------------------------------------
    private inner class Indus5GattCallback(private val rxDataReceivedCallback: DataReceivedCallback) :
        BleManagerGattCallback() {

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            connectionListener?.onServiceDiscovered("BLE device(Indus5) discovered")
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
            Timber.i("Dev_debug onDeviceReady of BLE device(QPlus) BLE_CONNECTED_STATUcalledS")
            connectionListener?.onDeviceReady(BLE_CONNECTED_STATUS)
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int) {
            Timber.i("onMtuChanged : mtu = $mtu")
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onDeviceDisconnected() {
            Timber.i("onDeviceDisconnected")
            _isEEGEnabled = false
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
