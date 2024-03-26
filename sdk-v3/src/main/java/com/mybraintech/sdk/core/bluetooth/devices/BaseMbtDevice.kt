package com.mybraintech.sdk.core.bluetooth.devices

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.mybraintech.sdk.core.bluetooth.MbtDeviceInterface
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.DeviceInformation
import com.mybraintech.sdk.core.model.EnumMBTDevice
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

    override fun connectMbt(mbtDevice: MbtDevice, connectionListener: ConnectionListener) {
        connectMbtWithRetries(mbtDevice, connectionListener, 0, 2)
    }

    private fun connectMbtWithRetries(
        mbtDevice: MbtDevice,
        connectionListener: ConnectionListener,
        currentRetry: Int = 0,
        maxRetry: Int = 3
    ) {
        if (!isBluetoothEnabled()) {
            connectionListener.onConnectionError(Throwable("Bluetooth is not enabled"))
            return
        }
        if (isConnectedAndReady()) {
            connectionListener.onConnectionError(Throwable("Device is connected already : ${bluetoothDevice.getString()}"))
            return
        }
        this.connectionListener = connectionListener
        this.targetMbtDevice = mbtDevice
        broadcastReceiver.register(context, mbtDevice.bluetoothDevice, connectionListener)

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
                            maxRetry
                        )
                    }
                    Thread(runnable).start()
                } else {
                    val name = device?.name
                    connectionListener.onConnectionError(Throwable("fail to connect to MbtDevice : name = $name | status = $status"))
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
            connectionListener?.onConnectionError(Throwable("there is no connected device to disconnect!"))
        }
    }

    protected abstract fun getDeviceType(): EnumMBTDevice

    abstract fun handleScanResults(results: List<ScanResult>)

    private fun getScanCallback(): ScanCallback {
        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Timber.d("onScanResult : name = ${result.device.name} | address = ${result.device.address}")
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
