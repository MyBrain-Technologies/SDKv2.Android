package com.mybraintech.sdk.core.bluetooth.devices

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothProfile.ServiceListener
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.mybraintech.sdk.core.acquisition.MBTSession
import com.mybraintech.sdk.core.bluetooth.MbtAudioDeviceInterface
import com.mybraintech.sdk.core.bluetooth.MbtDeviceInterface
import com.mybraintech.sdk.core.bluetooth.devices.melomind.MelomindCharacteristic
import com.mybraintech.sdk.core.listener.AccelerometerConfigListener
import com.mybraintech.sdk.core.listener.AudioNameListener
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener
import com.mybraintech.sdk.core.listener.DeviceSystemStatusListener
import com.mybraintech.sdk.core.listener.EEGFilterConfigListener
import com.mybraintech.sdk.core.listener.ScanResultListener
import com.mybraintech.sdk.core.listener.SensorStatusListener
import com.mybraintech.sdk.core.listener.SerialNumberChangedListener
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.DeviceInformation
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.MBTErrorCode
import com.mybraintech.sdk.core.model.MbtDevice
import com.mybraintech.sdk.util.AUDIO_CONNECTED_STATUS
import com.mybraintech.sdk.util.getString
import no.nordicsemi.android.ble.BleManager
import timber.log.Timber


abstract class BaseMbtDevice(ctx: Context) :
    BleManager(ctx), MbtDeviceInterface {

    protected val INDUS5_BLE_PREFIX = "qp_"
    protected val INDUS5_AUDIO_PREFIX = "QP"
    protected val MELOMIND_BLE_PREFIX = "melo_"
    protected val XON_BLE_PREFIX = "xon_"
    protected val MELOMIND_AUDIO_PREFIX = "MM"

    protected val MTU_SIZE = 47

    protected var isScanning: Boolean = false

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


    private var targetDeviceAudioAddress: String = ""
        get() {
            return field
        }
        set(value) {
            field = value
            broadcastReceiver.targetAudioAddress = value
        }

    var a2dp: BluetoothA2dp? = null  //class to connect to an A2dp device
    val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    abstract fun scanConnectedA2DP()
    protected var broadcastReceiver = MbtBleBroadcastReceiver(bluetoothAdapter)
    fun innitA2dpService() {
        Timber.d("Dev_debug innitA2dpService called ")
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(
            context,
            // listener notifies BluetoothProfile clients when they have been connected to or disconnected from the service
            object : ServiceListener {
                override fun onServiceDisconnected(profile: Int) {
                    setIsA2dpReady(false)
                    Timber.d("Dev_debug innitA2dpService onServiceDisconnected ")
                }

                override fun onServiceConnected(
                    profile: Int,
                    proxy: BluetoothProfile
                ) {
                    a2dp = proxy as? BluetoothA2dp

                    setIsA2dpReady(true)
                    scanConnectedA2DP()
                }
            }, BluetoothProfile.A2DP
        )
    }


    private fun routeAudioToBluetoothHeadset() {
        Timber.d("Dev_debug  routeAudioToBluetoothHeadset ")

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isBluetoothScoOn = true
        audioManager.startBluetoothSco()
    }

    override fun removeBondMbt() {
        val isBonded = super.isBonded()
        val isConnection = super.isConnected()
        Timber.d("Dev_debug  removeBondMbt  isBonded:$isBonded isConnection:$isConnection")

        if (isBonded) {
            targetMbtDevice = null
            this.broadcastReceiver.bleTargetDevice = null
             removeBond().enqueue()
        } else {
            connectionListener?.onConnectionError(
                Throwable("there is no connected device to remove!"),
                MBTErrorCode.NO_CONNECTED_DEVICE_TO_DISCONNECT
            )
        }
    }
    private var mIsA2dpReady = false
    fun setIsA2dpReady(ready: Boolean) {
        mIsA2dpReady = ready
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    fun connectUsingBluetoothA2dpBinder(
        deviceToConnect: BluetoothDevice?
    ) {

        fun implementAudioA2dp() {

            try {
                //establishing bluetooth connection with A2DP devices
                var connectedDevices = a2dp?.connectedDevices ?: arrayListOf()
                val connectedDeviceSize = connectedDevices.size
                var founded = false

                Timber.d("Dev_debug implementAudioA2dp check connect a2dp list size is:${connectedDeviceSize}")
                if (connectedDeviceSize > 0) {
                    for (device in connectedDevices) {
                        val deviceName = device.name

                        Timber.d("Dev_debug implementAudioA2dp  a2dp connectedDevice name:${device.name}")
                        if (deviceName.equals(deviceToConnect?.name)) {
                            founded = true
                            Timber.d("Dev_debug implementAudioA2dp  call back to caller listener:$connectionListener with action AUDIO_CONNECTED_STATUS")
                            connectionListener?.onDeviceReady(AUDIO_CONNECTED_STATUS)
                            break
                        }
                    }
                }
                //reconnect

                if (!founded) {
                    Timber.d("implementAudioA2dp  not found a2dp device do the connect again")
                    val connect =
                        a2dp?.javaClass
                            ?.getMethod("connect", BluetoothDevice::class.java)

//                    connect?.isAccessible = true
                    val a2dpConnectResult = connect?.invoke(a2dp, deviceToConnect)
                    Timber.d("Dev_debug after connect a2dp with result a2dpConnectResult:${a2dpConnectResult} and a2dp status:${a2dp?.isA2dpPlaying(deviceToConnect)}")

                }


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            Timber.d("Dev_debug  connectUsingBluetoothA2dpBinder device:${deviceToConnect?.name}")
            //establish a connection to the profile proxy object associated with the profile
            if (a2dp != null) {
                implementAudioA2dp()
            } else {
                BluetoothAdapter.getDefaultAdapter().getProfileProxy(
                    context,
                    // listener notifies BluetoothProfile clients when they have been connected to or disconnected from the service
                    object : ServiceListener {
                        override fun onServiceDisconnected(profile: Int) {
                            setIsA2dpReady(false)
                            Timber.d("Dev_debug getProfileProxy  onServiceDisconnected ")
                            disConnectUsingBluetoothA2dpBinder(deviceToConnect)
                        }

                        override fun onServiceConnected(
                            profile: Int,
                            proxy: BluetoothProfile
                        ) {
                            Timber.d("Dev_debug getProfileProxy  onServiceConnected ")
                            a2dp = proxy as? BluetoothA2dp
                            implementAudioA2dp()
                            setIsA2dpReady(true)
                        }
                    }, BluetoothProfile.A2DP
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    fun disConnectUsingBluetoothA2dpBinder(
        deviceToConnect: BluetoothDevice?
    ) {
        try {
            Timber.d("%s%s", "Dev_debug disConnectUsingBluetoothA2dpBinder deviceToDisConnect:", deviceToConnect)
            // For Android 4.2 Above Devices
            try {
                //disconnecting bluetooth device
                a2dp?.javaClass?.getMethod(
                    "disconnect",
                    BluetoothDevice::class.java
                )?.invoke(a2dp, deviceToConnect)


//                BluetoothAdapter.getDefaultAdapter()
//                    .closeProfileProxy(BluetoothProfile.A2DP, a2dp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    //


    var audioBroadCastAlreadrSetup = false
    fun setupAudioBTBroadcast(context: Context, listener:MbtAudioDeviceInterface) {
        Timber.d("Dev_debug BaseMbtDevice setupAudioBTBroadcast setupAudioBroadcast:$audioBroadCastAlreadrSetup")
        if (audioBroadCastAlreadrSetup) {
            return
        } else {
            broadcastReceiver.setupBluetoothBroadcast(context)
            audioBroadCastAlreadrSetup = true
            broadcastReceiver.registerAudioDevice(listener)
        }
    }
//    fun setupAudioBroadcast(context: Context) {
//        Timber.d("Dev_debug BaseMbtDevice MbtAudioDeviceInterface setupAudioBroadcast:$audioBroadCastAlreadrSetup")
//        if (audioBroadCastAlreadrSetup) {
//            return
//        } else {
//            broadcastReceiver.setupBluetoothBroadcast(context)
//            audioBroadCastAlreadrSetup = true
//            broadcastReceiver.registerAudioDevice(object : MbtAudioDeviceInterface {
//                override fun onMbtAudioDeviceFound(
//                    device: BluetoothDevice,
//                    action: String,
//                    state: Int
//                ) {
//                    Timber.d("Dev_debug BaseMbtDevice MbtAudioDeviceInterface callback with info action:$action state:$state device:${device.name}")
//
//                    if (action == BluetoothDevice.ACTION_FOUND) {
//
//                        Timber.d("Dev_debug BaseMbtDevice MbtAudioDeviceInterface device audio found")
//
//                        if (state == BluetoothDevice.BOND_BONDED) {
////                            connectUsingBluetoothA2dp(device)
//                            connectUsingBluetoothA2dpBinder(device)
//
////                            connectUsingBluetoothHeadset(device)
//                        } else {
//
//                            device.createBond()
//                        }
//
//
//                        bluetoothAdapter?.cancelDiscovery()
//                    } else if (action == BluetoothDevice.ACTION_ACL_CONNECTED && state == BluetoothDevice.BOND_BONDED) {
//                        Timber.d("Dev_debug  onDeviceReady call from registerAudioDevice call back with action ACTION_ACL_CONNECTED")
//                        connectUsingBluetoothA2dpBinder(device)
//                        //TODO: revert AUDIO_CONNECTED_STATUS uncomment below
////                        connectionListener.onDeviceReady(AUDIO_CONNECTED_STATUS)
//                    }
//                }
//            })
//        }
//    }
//    open fun startBluetoothScanning(audioName: String, caller: String) {
//        targetDeviceAudio = audioName
//        targetDeviceAudioAddress = audioName
//
//        Timber.d("Dev_debug tobe startBluetoothScanning bluetoothAdapter:$bluetoothAdapter withCaller:$caller targetDeviceAudio:$targetDeviceAudio")
//        val pairedDevices: MutableSet<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
//        var found = false
//        if (pairedDevices != null) {
//            for (device in pairedDevices) {
//                val deviceName = device.name
//                val macAddress = device.address
//                // Do something with the device (e.g., display in a list)
//                Timber.d("Dev_debug Paired device: $deviceName at $macAddress")
//                val targetAudioNameLength = targetDeviceAudio.length
//                var lastCharTargetAudioName = targetDeviceAudio
//                if (targetAudioNameLength > 5) {
//                    lastCharTargetAudioName = targetDeviceAudio.substring(
//                        targetAudioNameLength - 5,
//                        targetAudioNameLength
//                    )
//                }
//                Timber.d("Dev_debug tobe startBluetoothScanning lastCharTargetAudioName:$lastCharTargetAudioName")
//                if (deviceName.contains(lastCharTargetAudioName)) {
//                    found = true
//                    connectUsingBluetoothA2dpBinder(device)
////                    connectUsingBluetoothSocket(device)
////                    connectUsingBluetoothHeadset(device)
//                    break
//                }
//            }
//        }
//        if (found) {
//            Timber.d("Dev_debug found paired device call on audio paired device")
//
//        } else {
//            Timber.d("Dev_debug not found paired device start to trigger scan by startDiscovery bluetoothAdapter:$bluetoothAdapter")
//            bluetoothAdapter?.startDiscovery()
//        }
//
//
//    }

    //----------------------------------------------------------------------------
    // MARK: internal ble manager
    //----------------------------------------------------------------------------
    override fun getBleConnectionStatus(): BleConnectionStatus {
        val isConnectedAndReady = isConnectedAndReady()

        Timber.d("Dev_debug getBleConnectionStatu isConnectedAndReady:$isConnectedAndReady")
        if (isConnectedAndReady) {
//            Timber.d("device is ready and is connected")
            if (bluetoothDevice != null) {
                return BleConnectionStatus(MbtDevice(bluetoothDevice!!), true)
            } else {
                // this case should never happen
                Timber.e("Dev_debug fatal error: bluetoothDevice is null")
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
        val isConnected = super.isConnected()
        val isReady = super.isReady()
        Timber.i("Dev_debug isConnectedAndReady isConnected:$isConnected isReady:$isReady")
        return (isConnected && isReady)
    }

    override fun startScan(targetName:String,scanResultListener: ScanResultListener) {

        Timber.d("Dev_debug startScan called")
        if (!isBluetoothEnabled()) {
            scanResultListener.onScanError(Throwable("Bluetooth is not enabled"))
            return
        }
        this.scanResultListener = scanResultListener
        if (!isScanning) {
            isScanning = true
            mbtBleScanner.startScan(object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    Timber.d("Dev_debug onScanResult : name = ${result.device.name} | address = ${result.device.address} ")

                    handleScanResults(targetName,listOf(result))
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>) {
                    Timber.d("Dev_debug onBatchScanResults : size = ${results.size}")
                    handleScanResults(targetName,results)
                }

                override fun onScanFailed(errorCode: Int) {
                    val msg = "Dev_debug onScanFailed : errorCode=$errorCode"
                    Timber.e(msg)
                    scanResultListener.onScanError(Throwable(msg))
                }
            })
        } else {
            Timber.w("Dev_debug scan is running already!")
        }
    }

    override fun stopScan() {

        Timber.d("Dev_debug stopScan called")
        isScanning = false
        mbtBleScanner.stopScan()
    }

    override fun connectMbt(
        mbtDevice: MbtDevice,
        connectionListener: ConnectionListener,
        connectionMode: EnumBluetoothConnection
    ) {
        val currentThread = Thread.currentThread()
        Timber.d("Dev_debug connectMbt mbtDevice:$mbtDevice  Current thread: ${currentThread.name}")
        connectMbtWithRetries(mbtDevice, connectionListener, 0, 3, connectionMode)
    }

    private fun connectMbtWithRetries(
        mbtDevice: MbtDevice,
        connectionListener: ConnectionListener,
        currentRetry: Int = 0,
        maxRetry: Int = 3,
        connectionMode: EnumBluetoothConnection
    ) {
        Timber.d("Dev_debug connectMbtWithRetries currentRetry:$currentRetry maxRetry:$maxRetry")


        if (!isBluetoothEnabled()) {
            connectionListener.onConnectionError(
                Throwable("Bluetooth is not enabled"),
                MBTErrorCode.BLUETOOTH_DISABLED
            )
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
//        if (connectionMode == EnumBluetoothConnection.BLE_AUDIO) {
//            setupAudioBroadcast(context)
//        }

        val timeout = 10000L
        Timber.i("Dev_debug  connect : timeout = $timeout")
        val mainHandler = Handler(Looper.getMainLooper())

        // In your background thread:
        mainHandler.post {
            val currentThread = Thread.currentThread().name
            Timber.i("Dev_debug connect currentThreasd:${currentThread}")

            connect(mbtDevice.bluetoothDevice)
            .useAutoConnect(false)
            .timeout(timeout)
            .done {
                Timber.i("Dev_debug  ble connect done")
            }
            .fail { device, status ->
                if (currentRetry <= maxRetry) {
                    val delay = 10L
                    Timber.i("Dev_debug  Retry ${currentRetry + 1}. Wait $delay ms")
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
            .enqueue()}
    }

    override fun disconnectMbt() {
        val isConnected = super.isConnected()
        Timber.i("Dev_debug  disconnectMbt : isConnected = $isConnected")
        if (isConnected) {
            targetMbtDevice = null
            this.broadcastReceiver.bleTargetDevice = null
            disconnect().enqueue()


        } else {
            Timber.i("Dev_debug no action to disconnectMbt")
//            connectionListener?.onConnectionError(
//                Throwable("there is no connected device to disconnect!"),
//                MBTErrorCode.NO_CONNECTED_DEVICE_TO_DISCONNECT
//            )
        }
    }

    protected abstract fun getDeviceType(): EnumMBTDevice

    abstract fun handleScanResults(targetName:String,results: List<ScanResult>)



    /**
     * can be implement in subclass of [BaseMbtDevice]
     */
    override fun setSerialNumber(serialNumber: String, listener: SerialNumberChangedListener?) {
        throw UnsupportedOperationException("not supported")
    }

    override fun setAudioName(audioName: String, listener: AudioNameListener?) {
        throw UnsupportedOperationException("not supported")
    }

    override fun stopScanAudio() {
        Timber.d("Dev_debug stopScanAudio called")
        bluetoothAdapter?.cancelDiscovery()
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

    fun startDiscovery() {

        bluetoothAdapter?.startDiscovery()
    }
}
