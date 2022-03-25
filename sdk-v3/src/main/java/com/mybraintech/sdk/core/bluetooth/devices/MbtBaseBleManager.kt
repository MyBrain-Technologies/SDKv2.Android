package com.mybraintech.sdk.core.bluetooth.devices

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.mybraintech.sdk.core.bluetooth.IMbtBleManager
import com.mybraintech.sdk.core.bluetooth.MbtBleUtils
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener
import com.mybraintech.sdk.core.listener.ScanResultListener
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.DeviceInformation
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.MbtDevice
import com.mybraintech.sdk.util.getString
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanResult
import timber.log.Timber


abstract class MbtBaseBleManager(ctx: Context) :
    BleManager(ctx), IMbtBleManager {

    protected val MTU_SIZE = 47

    protected var isScanning: Boolean = false
    protected var broadcastReceiver = MbtBleBroadcastReceiver()
    protected val mbtBleScanner = MbtBleScanner()
    protected lateinit var scanResultListener: ScanResultListener

    protected var targetMbtDevice: MbtDevice? = null
    protected var deviceInformation = DeviceInformation()
    protected var connectionListener: ConnectionListener? = null

    protected var batteryLevelListener: BatteryLevelListener? = null
    protected var deviceInformationListener: DeviceInformationListener? = null

    //----------------------------------------------------------------------------
    // MARK: internal ble manager
    //----------------------------------------------------------------------------
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

            mbtBleScanner.startScan(getScanFilters(), getScanCallback())
        } else {
            Timber.w("a scan process is in progress already!")
        }
    }

    protected abstract fun getScanFilters() : List<ScanFilter>?

    override fun stopScan() {
        isScanning = false
        mbtBleScanner.stopScan()
    }

    override fun connectMbt(mbtDevice: MbtDevice, connectionListener: ConnectionListener) {
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
        connect(mbtDevice.bluetoothDevice)
            .useAutoConnect(false)
            .timeout(5000)
            .done {
                Timber.i("ble connect done")
            }
            .fail { device, status ->
                val name = if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    device?.name
                } else {
                    ""
                }
                connectionListener.onConnectionError(Throwable("fail to connect to MbtDevice : name = $name | status = $status"))
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

    protected abstract fun getDeviceType() : EnumMBTDevice

    override fun getBleConnectionStatus(): BleConnectionStatus {
        val gattConnectedDevices = MbtBleUtils.getGattConnectedDevices(context)
        var connectedDevice: BluetoothDevice? = null
        for (device in gattConnectedDevices) {
            if (getDeviceType() == EnumMBTDevice.Q_PLUS) {
                if (MbtBleUtils.isQPlus(device, context)) {
                    Timber.i("found a connected indus5")
                    connectedDevice = device
                    break
                }
            } else {
                connectedDevice = device
                break
            }
        }
        if (!isConnectedAndReady()) {
            Timber.d("device is NOT ready or is not connected")
            return if (connectedDevice != null) {
                BleConnectionStatus(MbtDevice(connectedDevice), false)
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

    abstract fun handleScanResults(results: List<BluetoothDevice>)

    private fun getScanCallback(): ScanCallback {
        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Timber.d(
                    "onScanResult : name = ${
                        MbtBleUtils.getDeviceName(
                            result.device,
                            context
                        )
                    } | address = ${result.device.address} "
                )
                handleScanResults(listOf(result.device))
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                Timber.d("onBatchScanResults : size = ${results.size}")
                handleScanResults(results.map { it.device })
            }

            override fun onScanFailed(errorCode: Int) {
                val msg = "onScanFailed : errorCode=$errorCode"
                Timber.e(msg)
                scanResultListener.onScanError(Throwable(msg))
            }
        }
    }
}
