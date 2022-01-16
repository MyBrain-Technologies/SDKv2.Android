package indus5

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import config.ConnectionConfig
import config.StreamConfig
import core.Indus5Singleton
import core.bluetooth.StreamState
import core.bluetooth.lowenergy.EnumIndus5Command
import core.bluetooth.lowenergy.Indus5Response
import core.bluetooth.lowenergy.MelomindCharacteristics
import core.bluetooth.lowenergy.parseRawIndus5Response
import core.bluetooth.requests.StreamRequestEvent
import core.device.model.MelomindQPlusDevice
import engine.clientevents.BaseError
import engine.clientevents.BluetoothError
import engine.clientevents.DeviceBatteryListener
import eventbus.MbtEventBus
import eventbus.events.BluetoothEEGEvent
import eventbus.events.IMSEvent
import eventbus.events.PpgEvent
import model.AccelerometerFrame
import model.PpgFrame
import timber.log.Timber
import utils.ConversionUtils
import java.util.*

//TODO: refactor later
@SuppressLint("StaticFieldLeak")
object MbtClientIndus5 {

    private var deviceName: String? = null
    private var triggerListener: TriggerListener? = null
    private var deviceBatteryListener: DeviceBatteryListener<BaseError>? = null
    private var accelerometerListener: AccelerometerListener? = null
    private var firmwareListener: FirmwareListener? = null
    private var ppgListener: PpgListener? = null
    private const val MELOMIND_INDUS5_PREFIX_1 = "melo_2"
    private const val MELOMIND_INDUS5_PREFIX_2 = "qp_"
    private const val MELOMIND_PREFIX = "melo_"

    lateinit var context: Context
    private lateinit var config: ConnectionConfig
    private lateinit var device: BluetoothDevice
    private var currentState = BluetoothGatt.STATE_DISCONNECTED

    private lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var streamConfig: StreamConfig

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private var scanning = false
    private var discoveringService = false
    private var isSubscribingRx = false
    private var isRequestingMtu = false
    private lateinit var handler: Handler

    // Stops scanning after N seconds.
    private const val SCAN_PERIOD: Long = 30 * 1000
    private const val MTU_SIZE: Int = 47

    private lateinit var leScanCallback: MyScanCallback

    class MyScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            if (result.device?.name != null) {
                val name = result.device?.name!!

                //log scan result for melomind device
                if (isNewDevice(name)) {
                    foundDevices.add(name)

                    Timber.d("found new melomind device : name = ${result.device?.name}")
                    val services = result.scanRecord?.serviceUuids
                    if (!services.isNullOrEmpty()) {
                        Timber.d("found services in scan record")
                        for (service in services) {
                            Timber.i("${service.uuid}")
                        }
                    }
                }

                //stop scanning if target device found
                if (isTargetDevice(result.device)) {
                    Timber.i("found indus5 : stop scan")
                    handler.removeCallbacks(scanTimeoutRunnable)
                    stopScan(isTimeout = false)
                    Indus5Singleton.mbtDevice =
                        MelomindQPlusDevice(result.device.address, result.device.name)
                    connectGattServer(result.device!!)
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Timber.d("onScanFailed : errorCode = $errorCode")
        }
    }

    /**
     * list all found melomind devices on scanning
     */
    var foundDevices = mutableListOf<String>()

    fun isNewDevice(name: String): Boolean {
        return (!foundDevices.contains(name))
    }

    val gattCallback = MyGattCallback()

    class MyGattCallback : BluetoothGattCallback() {
        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
            Timber.v("onPhyUpdate")
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyRead(gatt, txPhy, rxPhy, status)
            Timber.v("onPhyRead")
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Timber.v("onConnectionStateChange : value = ${newState.translateBluetoothGattState()}")
            if (currentState == newState) {
                return
            } else {
                currentState = newState
            }
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Timber.v("discoverServices")
                if (!discoveringService) {
                    discoveringService = true
                    bluetoothGatt.discoverServices()
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                try {
                    bluetoothGatt.close()
                    config.connectionStateListener.onDeviceDisconnected(MelomindQPlusDevice(device))
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Timber.v("onServicesDiscovered")
            Timber.v("services =")
            for (service in gatt.services) {
                Timber.v("uuid = ${service.uuid}")
            }

            if (discoveringService) {
                //subscribe rx on new thread
                discoveringService = false
                Thread {
                    isSubscribingRx = true
                    subscribeRx()
                }.start()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Timber.v("onCharacteristicRead")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Timber.v("onCharacteristicWrite")
            Timber.v("values = ${Arrays.toString(characteristic?.value)}")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)

            val response = characteristic.value.parseRawIndus5Response()
            when (response) {
                is Indus5Response.FirmwareVersion -> {
                    Timber.d("indus5 FirmwareVersion = ${response.version}")
                    firmwareListener?.onFirmwareVersion(response.version)
                }
                is Indus5Response.MtuChange -> {
                    Timber.d("indus5 mtu changed : byte 2 = ${response.size}")
                    config.connectionStateListener.onDeviceConnected(MelomindQPlusDevice(device))
                }
                is Indus5Response.TriggerConfiguration -> {
                    Timber.d("indus5 TriggerConfigResponse : trigger size = ${response.triggerSize}")
                    triggerListener?.onTriggerResponse(response.triggerSize)
                }
                is Indus5Response.EegFrame -> {
                    Timber.v(
                        "indus5 eeg frame received: data = ${
                            ConversionUtils.bytesToHex(
                                characteristic.value
                            )
                        }"
                    )
                    MbtEventBus.postEvent(BluetoothEEGEvent(response.data))
                }
                is Indus5Response.BatteryLevel -> {
                    Timber.d(
                        "indus5 BatteryLevelResponse received: data = ${
                            Arrays.toString(
                                characteristic.value
                            )
                        }"
                    )
                    deviceBatteryListener?.onBatteryLevelReceived(response.percent.toString())
                }
                is Indus5Response.EegStatus -> {
                    Timber.d("indus5 EegStatus : is enabled = ${response.isEnabled}")
                    MbtEventBus.postEvent(
                        if (response.isEnabled) {
                            StreamState.STARTED
                        } else {
                            StreamState.STOPPED
                        }
                    )
                    val isRecordRequest = false
                    val computeQualities = true
                    val monitorDeviceStatus = false
                    val recordConfig = streamConfig.recordConfig
                    MbtEventBus.postEvent(
                        StreamRequestEvent(
                            response.isEnabled,
                            isRecordRequest,
                            computeQualities,
                            monitorDeviceStatus,
                            recordConfig
                        )
                    )
                }
                is Indus5Response.ImsStatus -> {
                    Timber.d("indus5 AccelerometerCommand IMS : is enabled = ${response.isEnabled}")
                    if (response.isEnabled) {
                        accelerometerListener?.onAccelerometerStarted()
                    } else {
                        accelerometerListener?.onAccelerometerStopped()
                    }
                }
                is Indus5Response.ImsFrame -> {
                    Timber.v("on ImsFrame : data = ${ConversionUtils.bytesToHex(response.data)}")
                    val frame = AccelerometerFrame(response.data)
                    MbtEventBus.postEvent(IMSEvent(frame.positions))
                    accelerometerListener?.onNewAccelerometerFrame(frame)
                }
                is Indus5Response.PpgStatus -> {
                    Timber.d("indus5 PpgCommand : is enabled = ${response.isEnabled}")
                    if (response.isEnabled) {
                        ppgListener?.onPpgStarted()
                    } else {
                        ppgListener?.onPpgStopped()
                    }
                }
                is Indus5Response.PpgFrame -> {
                    Timber.v("on PpgFrame : data = ${ConversionUtils.bytesToHex(response.data)}")
                    val frame = PpgFrame(response.data)
                    MbtEventBus.postEvent(PpgEvent(frame))
                    ppgListener?.onPpgFrame(frame)
                }
                else -> {
                    //it should be Indus5Response.UnknownResponse here
                    Timber.e(
                        "unknown indus5 frame : data = ${
                            ConversionUtils.bytesToHex(
                                characteristic.value
                            )
                        }"
                    )
                }
            }

//            Timber.v("onCharacteristicChanged")
//            Timber.v("values = ${Arrays.toString(characteristic?.value)}")
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorRead(gatt, descriptor, status)
            Timber.v("onDescriptorRead")
            Timber.v("uuid = ${descriptor.uuid} | value = ${Arrays.toString(descriptor.value)}")
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Timber.i("onDescriptorWrite : status = $status")
            if (descriptor.characteristic.uuid.toString() == getRx().uuid.toString() && descriptor.uuid.toString() == getRxDescriptor().uuid.toString()) {
                Timber.i("onDescriptorWrite INDUS_5_RX_CHARACTERISTIC")

                if (isSubscribingRx) {
                    //start changing mtu
                    isSubscribingRx = false
                    Thread {
                        Timber.i("request gatt to change mtu")
                        isRequestingMtu = true
                        gatt.requestMtu(MTU_SIZE)
                    }.start()
                }
            }
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
            super.onReliableWriteCompleted(gatt, status)
            Timber.v("onReliableWriteCompleted")
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            Timber.v("onReadRemoteRssi")
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            Timber.v("onMtuChanged: size = $mtu")

            if (isRequestingMtu) {
                //start send new mtu size by mailbox command
                isRequestingMtu = false
                Thread {
                    Timber.i("send mailbox command to change mtu")
                    val command = EnumIndus5Command.MBX_TRANSMIT_MTU_SIZE.bytes.toMutableList()
                    command.add(MTU_SIZE.toByte())
                    val tx = getTx()
                    tx.value = command.toByteArray()
                    gatt.writeCharacteristic(tx)
                }.start()
            }
        }
    }

    @JvmStatic
    fun connectBluetooth(context: Context, config: ConnectionConfig) {
        this.context = context
        this.config = config

        handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
        //start process
        setupBle(config)
    }

    @JvmStatic
    fun disconnectBluetooth() {
        bluetoothGatt.disconnect()

        // TODO: 13/09/2021 secure a firmware bug, the bug should be fix in the new firmware version then we can remove this function
//        removeBond(bluetoothGatt.device)
    }

    private fun removeBond(device: BluetoothDevice) {
        try {
            device::class.java.getMethod("removeBond").invoke(device)
        } catch (e: Exception) {
            Timber.e("Removing bond has been failed. ${e.message}")
        }
    }

    private fun setupBle(config: ConnectionConfig) {
        // Initializes Bluetooth adapter.
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter.isEnabled) {
            scanLeDevice(config.deviceName)
        } else {
            Timber.e("bluetooth is not ready")
            onConnectionError()
        }
    }

    private fun connectGattServer(device: BluetoothDevice) {
        Timber.d("connectGattServer")

        try {
            // TODO: 13/09/2021 secure a firmware bug, the bug should be fix in the new firmware version then we can remove this function
//            removeBond(device)
        } catch (e: Exception) {
            Timber.e(e)
        }

        this.device = device
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
        bluetoothGatt.connect()
        Timber.v("bluetoothGatt is $bluetoothGatt")
    }

    val scanTimeoutRunnable: Runnable =
        Runnable {
            Timber.e("Scan fails: Timeout")
            stopScan(isTimeout = true)
        }

    private fun scanLeDevice(name: String?) {
        this.deviceName = name
        //log connected ble devices
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
//        bluetoothManager.
        for (device in devices) {
            if ((device.type == BluetoothDevice.DEVICE_TYPE_LE) && isTargetDevice(device)) {
                Timber.v("already connected to ble device = %s", device.toString())
                Timber.v("try to reconnect...")
                connectGattServer(device)
                return
            }
        }

        bluetoothAdapter.bluetoothLeScanner?.let { scanner ->
            if (!scanning) { // Stops scanning after a pre-defined scan period.
                handler.postDelayed(scanTimeoutRunnable, SCAN_PERIOD)

                scanning = true
                discoveringService = false
                foundDevices = mutableListOf()

                leScanCallback = MyScanCallback()

                Timber.d("startScan")
                scanner.startScan(leScanCallback)
            }
        }
    }

    fun isTargetDevice(device: BluetoothDevice): Boolean {
        if (deviceName != null) {
            //if deviceName is defined, only search this name
            return deviceName == device.name
        }
        val condition1 = device.name?.startsWith(MELOMIND_INDUS5_PREFIX_1) == true
        val condition2 = device.name?.startsWith(MELOMIND_INDUS5_PREFIX_2) == true
        return (condition1 || condition2)
    }

    fun stopScan(isTimeout: Boolean) {
        try {
            Timber.d("stopScan")
            scanning = false
            bluetoothAdapter.bluetoothLeScanner?.stopScan(leScanCallback)
        } catch (e: Exception) {
            Timber.e(e)
        }
        if (isTimeout) {
            onConnectionError()
        }
    }

    fun subscribeRx() {
        val isSubscribed = bluetoothGatt.setCharacteristicNotification(getRx(), true)
        Timber.v("isSubscribed = $isSubscribed")
        val rxDescriptor = getRxDescriptor()
        rxDescriptor.value = (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        val isDescriptorWrittenSent = bluetoothGatt.writeDescriptor(rxDescriptor)
        Timber.v("isDescriptorWrittenSent = $isDescriptorWrittenSent")
    }

    private fun onConnectionError() {
        config.connectionStateListener.onError(BluetoothError.ERROR_CONNECT_FAILED)
    }

    @JvmStatic
    fun startStream(streamConfig: StreamConfig) {
        Timber.i("indus 5 startStream")
        val tx = getTx()
        tx.value = EnumIndus5Command.MBX_START_EEG_ACQUISITION.bytes
        bluetoothGatt.writeCharacteristic(tx)
        this.streamConfig = streamConfig
    }

    @JvmStatic
    fun stopStream() {
        Timber.i("indus 5 stopStream")
        val tx = getTx()
        tx.value = EnumIndus5Command.MBX_STOP_EEG_ACQUISITION.bytes
        bluetoothGatt.writeCharacteristic(tx)
    }

    @JvmStatic
    fun getBatteryLevelIndus5(listener: DeviceBatteryListener<BaseError>) {
        Timber.i("indus 5 getBatteryLevelIndus5")
        this.deviceBatteryListener = listener
        val tx = getTx()
        tx.value = EnumIndus5Command.MBX_GET_BATTERY_VALUE.bytes
        bluetoothGatt.writeCharacteristic(tx)
    }

    //----------------------------------------------------------------------------
    // get FW
    //----------------------------------------------------------------------------
    @JvmStatic
    fun getFirmwareVersion(listener: FirmwareListener): Boolean {
        this.firmwareListener = listener
        val tx = getTx()
        tx.value = EnumIndus5Command.MBX_GET_FIRMWARE_VERSION.bytes
        return bluetoothGatt.writeCharacteristic(tx)
    }

    //----------------------------------------------------------------------------
    // IMS - Accelerometer
    //----------------------------------------------------------------------------
    @JvmStatic
    fun startAccelerometer(listener: AccelerometerListener? = null): Boolean {
        this.accelerometerListener = listener
        val tx = getTx()
        tx.value = EnumIndus5Command.MBX_START_IMS_ACQUISITION.bytes
        return bluetoothGatt.writeCharacteristic(tx)
    }

    @JvmStatic
    fun stopAccelerometer(): Boolean {
        val tx = getTx()
        tx.value = EnumIndus5Command.MBX_STOP_IMS_ACQUISITION.bytes
        return bluetoothGatt.writeCharacteristic(tx)
    }

    //----------------------------------------------------------------------------
    // PPG
    //----------------------------------------------------------------------------
    @JvmStatic
    fun startPPG(listener: PpgListener? = null): Boolean {
        this.ppgListener = listener
        val tx = getTx()
        tx.value = EnumIndus5Command.MBX_START_PPG_ACQUISITION.bytes
        return bluetoothGatt.writeCharacteristic(tx)
    }

    @JvmStatic
    fun stopPPG(): Boolean {
        val tx = getTx()
        tx.value = EnumIndus5Command.MBX_STOP_PPG_ACQUISITION.bytes
        return bluetoothGatt.writeCharacteristic(tx)
    }

    @JvmOverloads
    @JvmStatic
    fun configureTrigger(isEnabled: Boolean, triggerListener: TriggerListener? = null): Boolean {
        this.triggerListener = triggerListener
        val p300Byte: Byte = if (isEnabled) {
            0x01
        } else {
            0x00
        }
        val command = EnumIndus5Command.MBX_P300_ENABLE.bytes.toMutableList()
        command.add(p300Byte)
        val tx = getTx()
        tx.value = command.toByteArray()
        return bluetoothGatt.writeCharacteristic(tx)
    }

    private fun getService(): BluetoothGattService {
        return bluetoothGatt.getService(MelomindCharacteristics.INDUS_5_TRANSPARENT_SERVICE)
    }

    private fun getRx(): BluetoothGattCharacteristic {
        return getService().getCharacteristic(MelomindCharacteristics.INDUS_5_RX_CHARACTERISTIC)
    }

    private fun getTx(): BluetoothGattCharacteristic {
        return getService().getCharacteristic(MelomindCharacteristics.INDUS_5_TX_CHARACTERISTIC)
    }

    private fun getRxDescriptor(): BluetoothGattDescriptor {
        return getRx().getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)
    }

    interface RxSubscriptionListener {
        fun onRxSubscribed()
    }
}