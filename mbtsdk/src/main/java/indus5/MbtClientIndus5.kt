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
import core.bluetooth.lowenergy.EnumIndus5Command
import core.bluetooth.lowenergy.Indus5Response
import core.bluetooth.lowenergy.MelomindCharacteristics
import core.bluetooth.lowenergy.parseRawIndus5Response
import core.bluetooth.requests.StreamRequestEvent
import core.device.model.MbtDevice
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

    private var triggerListener: TriggerListener? = null
    private var deviceBatteryListener: DeviceBatteryListener<BaseError>? = null
    private var accelerometerListener: AccelerometerListener? = null
    private var ppgListener: PpgListener? = null
    private const val MELOMIND_INDUS5_PREFIX = "melo_2"
    private const val MELOMIND_PREFIX = "melo_"

    lateinit var context: Context
    private lateinit var config: ConnectionConfig
    private lateinit var device: BluetoothDevice
    private var isDeviceFoundAndConnected = false
    private var isReplied = false
    private var currentState = BluetoothGatt.STATE_DISCONNECTED
    private var mbtDevice: MbtDevice? = null

    private lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var service: BluetoothGattService
    private lateinit var rx: BluetoothGattCharacteristic
    private lateinit var tx: BluetoothGattCharacteristic
    private lateinit var rxDescriptor: BluetoothGattDescriptor
    private lateinit var streamConfig: StreamConfig

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private var scanning = false
    private var discoveringService = false
    private lateinit var handler: Handler

    // Stops scanning after N seconds.
    private const val SCAN_PERIOD: Long = 15 * 1000
    private const val MTU_SIZE: Int = 47

    private lateinit var leScanCallback: MyScanCallback

    class MyScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            if ((result.device?.name != null) && (result.device!!.name!!.startsWith(MELOMIND_PREFIX))) {
                val name = result.device?.name!!
                if (isNewDevice(name)) {
                    foundDevices.add(name)

                    Timber.d("found device : name = ${result.device?.name}")
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
                    stopScan()
                    Indus5Singleton.mbtDevice = MelomindQPlusDevice(result.device.address, result.device.name)
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
     * list all found devices on scanning
     */
    val foundDevices = mutableListOf<String>()

    fun isNewDevice(name: String): Boolean {
        return (name.startsWith("melo") && !(foundDevices.contains(name)))
    }

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
            service = gatt.getService(MelomindCharacteristics.INDUS_5_TRANSPARENT_SERVICE)
            Timber.v("service is $service")

            rx = service.getCharacteristic(MelomindCharacteristics.INDUS_5_RX_CHARACTERISTIC)
            Timber.v("rx is $rx")

            tx = service.getCharacteristic(MelomindCharacteristics.INDUS_5_TX_CHARACTERISTIC)
            Timber.v("tx is $tx")

            rxDescriptor = rx.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)
            Timber.v("rxDescriptor is $rxDescriptor")

            //subscribe rx on new thread
            Thread {
                subscribeRx()
            }.start()
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
                is Indus5Response.MtuChange -> {
                    Timber.d("indus5 mtu changed : byte 2 = ${response.size}")
                    config.connectionStateListener.onDeviceConnected(MelomindQPlusDevice(device))
                }
                is Indus5Response.TriggerConfiguration -> {
                    Timber.d("indus5 TriggerConfigResponse : enabled = ${response.isEnabled}")
                    triggerListener?.onTriggerResponse(response.isEnabled)
                }
                is Indus5Response.EegFrame -> {
//                    Timber.v("indus5 eeg frame received: data = ${Arrays.toString(characteristic.value)}")
                    MbtEventBus.postEvent(BluetoothEEGEvent(response.data))

                }
                is Indus5Response.BatteryLevel -> {
                    Timber.d("indus5 BatteryLevelResponse received: data = ${Arrays.toString(characteristic.value)}")
                    deviceBatteryListener?.onBatteryLevelReceived(response.percent.toString())
                }
                is Indus5Response.EegStatus -> {
                    Timber.d("indus5 EegStatus : is enabled = ${response.isEnabled}")
                    val isStartRequest = true
                    val isRecordRequest = false
                    val computeQualities = true
                    val monitorDeviceStatus = false
                    val recordConfig = streamConfig.recordConfig
                    if (response.isEnabled) {
                        Timber.d("indus5 eeg start")
                        MbtEventBus.postEvent(
                                StreamRequestEvent(
                                        response.isEnabled,
                                        isRecordRequest,
                                        computeQualities,
                                        monitorDeviceStatus,
                                        recordConfig
                                )
                        )

                    } else {
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
                    Timber.e("unknown indus5 frame : data = ${ConversionUtils.bytesToHex(characteristic.value)}")
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
            if (descriptor.uuid.toString() == rx.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID).uuid.toString()) {
                Timber.i("onDescriptorWrite INDUS_5_RX_CHARACTERISTIC")
            }

            //start changing mtu
            Thread(Runnable {
                Timber.i("request gatt to change mtu")
                gatt.requestMtu(MTU_SIZE)
            }).start()
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

            //start send new mtu size by mailbox command
            Thread {
                Timber.i("send mailbox command to change mtu")
                val command = EnumIndus5Command.MBX_TRANSMIT_MTU_SIZE.bytes.toMutableList()
                command.add(MTU_SIZE.toByte())
                tx.value = command.toByteArray()
                gatt.writeCharacteristic(tx)
            }.start()
        }
    }

    @JvmStatic
    fun connectBluetooth(context: Context, config: ConnectionConfig) {
        this.context = context
        this.config = config

        handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
        //start process
        setupBle()

        //after scan period device should be found and connected
        handler.postDelayed(Runnable { onConnectionError() }, SCAN_PERIOD)
    }

    @JvmStatic
    fun disconnectBluetooth() {
        bluetoothGatt.disconnect()
    }

    private fun setupBle() {
        // Initializes Bluetooth adapter.
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter.isEnabled) {
            scanLeDevice()
        } else {
            Timber.e("bluetooth is not ready")
            onConnectionError()
        }
    }

    private fun connectGattServer(device: BluetoothDevice) {
        Timber.d("connectGattServer")
        this.device = device
        val gattCallback = MyGattCallback()
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
        bluetoothGatt.connect()
        Timber.v("bluetoothGatt is $bluetoothGatt")
    }

    private fun scanLeDevice() {
        Timber.d("scanLeDevice")

        bluetoothAdapter.bluetoothLeScanner?.let { scanner ->
            if (!scanning) { // Stops scanning after a pre-defined scan period.
                handler.postDelayed({
                    stopScan()
                }, SCAN_PERIOD)

                scanning = true
                discoveringService = false
                foundDevices.clear()

                leScanCallback = MyScanCallback()
                scanner.startScan(leScanCallback)
            }
        }
    }

    fun isTargetDevice(device: BluetoothDevice): Boolean {
        return device.name?.startsWith(MELOMIND_INDUS5_PREFIX) == true
    }

    fun stopScan() {
        if (scanning) {
            Timber.v("stopScan")
            scanning = false
            bluetoothAdapter.bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }

    fun subscribeRx() {
        val isSubscribed = bluetoothGatt.setCharacteristicNotification(rx, true)
        Timber.v("isSubscribed = $isSubscribed")
        rxDescriptor.value = (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        val isDescriptorWritten = bluetoothGatt.writeDescriptor(rxDescriptor)
        Timber.v("isDescriptorWritten = $isDescriptorWritten")
    }

    private fun onConnectionError() {
        if (!isDeviceFoundAndConnected && !isReplied) {
            isReplied = true
            config.connectionStateListener.onError(BluetoothError.ERROR_CONNECT_FAILED)
        }
    }

    @JvmStatic
    fun startStream(streamConfig: StreamConfig) {
        Timber.i("indus 5 startStream")
        tx.value = EnumIndus5Command.MBX_START_EEG_ACQUISITION.bytes
        bluetoothGatt.writeCharacteristic(tx)
        this.streamConfig = streamConfig
    }

    @JvmStatic
    fun stopStream() {
        Timber.i("indus 5 stopStream")
        tx.value = EnumIndus5Command.MBX_STOP_EEG_ACQUISITION.bytes
        bluetoothGatt.writeCharacteristic(tx)
    }

    @JvmStatic
    fun getBatteryLevelIndus5(listener: DeviceBatteryListener<BaseError>) {
        Timber.i("indus 5 getBatteryLevelIndus5")
        this.deviceBatteryListener = listener
        tx.value = EnumIndus5Command.MBX_GET_BATTERY_VALUE.bytes
        bluetoothGatt.writeCharacteristic(tx)
    }

    //----------------------------------------------------------------------------
    // IMS - Accelerometer
    //----------------------------------------------------------------------------
    @JvmStatic
    fun startAccelerometer(listener: AccelerometerListener? = null): Boolean {
        this.accelerometerListener = listener
        tx.value = EnumIndus5Command.MBX_START_IMS_ACQUISITION.bytes
        return bluetoothGatt.writeCharacteristic(tx)
    }

    @JvmStatic
    fun stopAccelerometer(): Boolean {
        tx.value = EnumIndus5Command.MBX_STOP_IMS_ACQUISITION.bytes
        return bluetoothGatt.writeCharacteristic(tx)
    }

    //----------------------------------------------------------------------------
    // PPG
    //----------------------------------------------------------------------------
    @JvmStatic
    fun startPPG(listener: PpgListener? = null): Boolean {
        this.ppgListener = listener
        tx.value = EnumIndus5Command.MBX_START_PPG_ACQUISITION.bytes
        return bluetoothGatt.writeCharacteristic(tx)
    }

    @JvmStatic
    fun stopPPG(): Boolean {
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
        tx.value = command.toByteArray()
        return bluetoothGatt.writeCharacteristic(tx)
    }
}