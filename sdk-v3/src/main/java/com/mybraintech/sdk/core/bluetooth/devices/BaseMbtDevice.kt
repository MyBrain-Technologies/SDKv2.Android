package com.mybraintech.sdk.core.bluetooth.devices

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.mybraintech.sdk.core.bluetooth.MbtAudioDeviceInterface
import com.mybraintech.sdk.core.bluetooth.MbtDeviceInterface
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.DeviceInformation
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.MBTErrorCode
import com.mybraintech.sdk.core.model.MbtDevice
import com.mybraintech.sdk.util.getString
import no.nordicsemi.android.ble.BleManager
import timber.log.Timber


abstract class BaseMbtDevice(ctx: Context) :
    BleManager(ctx), MbtDeviceInterface {

    protected val INDUS5_BLE_PREFIX = "qp_"
    protected val INDUS5_AUDIO_PREFIX = "QP"
    protected val MELOMIND_BLE_PREFIX = "melo_"
    protected val MELOMIND_AUDIO_PREFIX = "MM"

    protected val MTU_SIZE = 47

    protected var isScanning: Boolean = false

    protected var broadcastReceiver = MbtBleBroadcastReceiver()
    protected val mbtBleScanner = MbtBleScanner()
    protected lateinit var scanResultListener: ScanResultListener

    protected var targetMbtDevice: MbtDevice? = null
    protected var deviceInformation = DeviceInformation()
    protected var connectionListener: ConnectionListener? = null

    protected var deviceSystemStatusListener: DeviceSystemStatusListener? = null
    protected var serialNumberChangedListener: SerialNumberChangedListener? = null
    protected var audioNameListener: AudioNameListener? = null
    protected var batteryLevelListener: BatteryLevelListener? = null
    protected var deviceInformationListener: DeviceInformationListener? = null
    protected var sensorStatusListener: SensorStatusListener? = null
    protected var accelerometerConfigListener: AccelerometerConfigListener? = null
    protected var eegFilterConfigListener: EEGFilterConfigListener? = null

    //classic bluetooth
    protected var targetDeviceAudio:String = ""
        get() {
          return  field
        }
        set(value) {
            field = value
            broadcastReceiver.targetAudioDevice = value
        }

    private var device: BluetoothDevice? = null
    private lateinit var a2dp: BluetoothA2dp  //class to connect to an A2dp device
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    fun connectUsingBluetoothA2dp(
        deviceToConnect: BluetoothDevice?
    ) {
        try {
            device = deviceToConnect
            BluetoothAdapter.getDefaultAdapter().getProfileProxy(
                context,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceDisconnected(profile: Int) {
                        //disConnectUsingBluetoothA2dp(device)
                    }
                    override fun onServiceConnected(
                        profile: Int,
                        proxy: BluetoothProfile
                    ) {
                        a2dp = proxy as BluetoothA2dp
                        val connectedDevice = a2dp.connectedDevices
                        var founded = false
                        try {
                            Timber.d("MelomindDeviceImpl getProfileProxy connectedDevice number:${connectedDevice.size}")
                            if (connectedDevice.size>0) {
                                for (device in connectedDevice) {
                                    val deviceName = device.name

                                    Timber.d("MelomindDeviceImpl getProfileProxy a2dp connectedDevice name:${device.name}")
                                    if (deviceName.equals(targetDeviceAudio)) {
                                        founded = true
                                        Timber.d("onDeviceReady call from r getProfileProxy a2dp with device already connected")
                                        connectionListener?.onDeviceReady("audio connected")
                                        break
                                    }
                                }
                            }
                            //reconnect
                            Timber.d("MelomindDeviceImpl getProfileProxy device was founded and connected before:${founded}")
                            if (!founded) {
                                a2dp.javaClass
                                    .getMethod("connect", BluetoothDevice::class.java)
                                    .invoke(a2dp, deviceToConnect)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }, BluetoothProfile.A2DP
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    open fun startBluetoothScanning(caller:String) {

        Timber.d("MelomindDeviceImpl tobe startBluetoothScanning bluetoothAdapter:$bluetoothAdapter withCaller:$caller")
        val pairedDevices: MutableSet<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        var found = false
        if (pairedDevices != null) {
            for (device in pairedDevices) {
                val deviceName = device.name
                val macAddress = device.address
                // Do something with the device (e.g., display in a list)
                Timber.d("MelomindDeviceImpl Paired device: $deviceName at $macAddress")
                if (deviceName.equals(targetDeviceAudio)) {
                    found = true
                    break
                }
            }
        }
        if (found) {
            Timber.d("not found paired device call on audio paired device")
            connectionListener?.onDeviceReady("audio connected")
        } else {
            Timber.d("not found paired device start to trigger scan by startDiscovery")
            bluetoothAdapter?.startDiscovery()
        }


    }

    //----------------------------------------------------------------------------
    // MARK: internal ble manager
    //----------------------------------------------------------------------------
    override fun getBleConnectionStatus(): BleConnectionStatus {
        if (isConnectedAndReady()) {
//            Timber.d("device is ready and is connected")
            if (bluetoothDevice != null) {
                return BleConnectionStatus(MbtDevice(bluetoothDevice!!), true)
            } else {
                // this case should never happen
                Timber.e("fatal error: bluetoothDevice is null")
                return BleConnectionStatus(null, false)
            }

        } else {
//            Timber.d("device is NOT ready or is not connected")
            return BleConnectionStatus(null, false)
        }
    }

    protected fun isBluetoothEnabled(): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isEnabled
    }

    protected fun isConnectedAndReady(): Boolean {
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
            Timber.w("scan is running already!")
        }
    }

    override fun stopScan() {
        isScanning = false
        mbtBleScanner.stopScan()
    }

    override fun connectMbt(mbtDevice: MbtDevice, connectionListener: ConnectionListener, connectionMode:EnumBluetoothConnection) {
        connectMbtWithRetries(mbtDevice, connectionListener, 0, 2, connectionMode)
    }

    private fun connectMbtWithRetries(
        mbtDevice: MbtDevice,
        connectionListener: ConnectionListener,
        currentRetry: Int = 0,
        maxRetry: Int = 3,
        connectionMode: EnumBluetoothConnection
    ) {
        if (!isBluetoothEnabled()) {
            connectionListener.onConnectionError(Throwable("Bluetooth is not enabled"),MBTErrorCode.BLUETOOTH_DISABLED)
            return
        }
        if (isConnectedAndReady()) {
            connectionListener.onConnectionError(
                Throwable("Device is connected already : ${bluetoothDevice.getString()}"),
                MBTErrorCode.DEVICE_CONNECTED_ALREADY
            )
            return
        }
        this.connectionListener = connectionListener
        this.targetMbtDevice = mbtDevice
        broadcastReceiver.register(context, mbtDevice.bluetoothDevice, connectionListener)
        if (connectionMode == EnumBluetoothConnection.BLE_AUDIO) {
            broadcastReceiver.registerAudioDevice(object : MbtAudioDeviceInterface {
                override fun onMbtAudioDeviceFound(
                    device: BluetoothDevice,
                    action: String,
                    state: Int
                ) {
                    Timber.d("BaseMbtDevice MbtAudioDeviceInterface callback with info action:$action state:$state device:${device.name}")

                    if (action == BluetoothDevice.ACTION_FOUND) {

                        Timber.d("BaseMbtDevice MbtAudioDeviceInterface device audio found")

                        if (state == BluetoothDevice.BOND_BONDED) {
                            connectUsingBluetoothA2dp(device)
                        } else {
                            device.createBond()
                        }
                        bluetoothAdapter?.cancelDiscovery()
                    } else if (action == BluetoothDevice.ACTION_ACL_CONNECTED && state == BluetoothDevice.BOND_BONDED) {
                        Timber.d("onDeviceReady call from registerAudioDevice call back with action ACTION_ACL_CONNECTED")
                        connectionListener.onDeviceReady("audio connected")
                    }
                }
            })
        }

        val timeout = 10000L
        Timber.i("connect : timeout = $timeout")
        connect(mbtDevice.bluetoothDevice)
            .useAutoConnect(false)
            .timeout(timeout)
            .done {
                Timber.i("ble connect done")
            }
            .fail { device, status ->
                if (currentRetry <= maxRetry) {
                    val delay = 10L
                    Timber.i("Retry ${currentRetry + 1}. Wait $delay ms")
                    val runnable = Runnable {
                        Thread.sleep(delay)
                        connectMbtWithRetries(
                            mbtDevice,
                            connectionListener,
                            currentRetry + 1,
                            maxRetry, connectionMode
                        )
                    }
                    Thread(runnable).start()
                } else {
                    val name = device?.name
                    connectionListener.onConnectionError(
                        Throwable("fail to connect to MbtDevice : name = $name | status = $status"),
                        MBTErrorCode.FAILED_TO_CONNECTED_TO_DEVICE
                    )
                }
            }
            .enqueue()
    }

    override fun disconnectMbt() {
        if (super.isConnected()) {
            targetMbtDevice = null
            this.broadcastReceiver.targetDevice = null
            disconnect().enqueue()
        } else {
            connectionListener?.onConnectionError(
                Throwable("there is no connected device to disconnect!"),
                MBTErrorCode.NO_CONNECTED_DEVICE_TO_CONNECT
            )
        }
    }

    protected abstract fun getDeviceType(): EnumMBTDevice

    abstract fun handleScanResults(results: List<ScanResult>)

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
                val msg = "onScanFailed : errorCode=$errorCode"
                Timber.e(msg)
                scanResultListener.onScanError(Throwable(msg))
            }
        }
    }

    /**
     * can be implement in subclass of [BaseMbtDevice]
     */
    override fun setSerialNumber(serialNumber: String, listener: SerialNumberChangedListener?) {
        throw UnsupportedOperationException("not supported")
    }

    override fun setAudioName(audioName: String, listener: AudioNameListener?) {
        throw UnsupportedOperationException("not supported")
    }
}
