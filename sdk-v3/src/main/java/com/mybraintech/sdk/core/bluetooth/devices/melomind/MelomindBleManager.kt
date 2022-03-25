package com.mybraintech.sdk.core.bluetooth.devices.melomind

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessing
import com.mybraintech.sdk.core.bluetooth.MbtBleUtils
import com.mybraintech.sdk.core.bluetooth.devices.MbtBaseBleManager
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.DeviceInformation
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.MbtDevice
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import timber.log.Timber

class MelomindBleManager(ctx: Context) : MbtBaseBleManager(ctx) {

    // required services
    private var deviceInformationService: BluetoothGattService? = null
    private var measurementService: BluetoothGattService? = null

    private var eegSignalProcessing: EEGSignalProcessing? = null

    //----------------------------------------------------------------------------
    // MARK: ble manager
    //----------------------------------------------------------------------------
    override fun getGattCallback(): BleManager.BleManagerGattCallback = MelomindGattCallback()

    override fun log(priority: Int, message: String) {
        if (message.contains("value: (0x) 40")) {
            Timber.v(message)
        } else {
            Timber.log(priority, message)
        }
    }

    override fun getBatteryLevel(batteryLevelListener: BatteryLevelListener) {
        this.batteryLevelListener = batteryLevelListener
        TODO()
    }

    //----------------------------------------------------------------------------
    // MARK: internal ble manager
    //----------------------------------------------------------------------------
    override fun getBleConnectionStatus(): BleConnectionStatus {
        val gattConnectedDevices = MbtBleUtils.getGattConnectedDevices(context)
        var connectedIndus5: BluetoothDevice? = null
        for (device in gattConnectedDevices) {
            if (MbtBleUtils.isQPlus(device, context)) {
                Timber.i("found a connected indus5")
                connectedIndus5 = device
                break
            }
        }
        if (!isConnectedAndReady()) {
            Timber.d("device is NOT ready or is not connected")
            return if (connectedIndus5 != null) {
                BleConnectionStatus(MbtDevice(connectedIndus5), false)
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

    override fun getDeviceType() = EnumMBTDevice.MELOMIND

    override fun getScanFilters(): List<ScanFilter>? {
        val filters: MutableList<ScanFilter> = ArrayList()
        filters.add(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(MelomindService.MEASUREMENT.uuid))
                .build()
        )
        return filters
    }

    override fun getDeviceInformation(deviceInformationListener: DeviceInformationListener) {
        this.deviceInformation = DeviceInformation().also {
            it.productName = MbtBleUtils.getDeviceName(targetMbtDevice?.bluetoothDevice, context)
        }
        TODO()
    }

    override fun startEeg(eegSignalProcessing: EEGSignalProcessing) {
        if (deviceInformationService == null || measurementService == null) {
            eegSignalProcessing.eegListener?.onEegError(Throwable("required services not found!"))
            return
        }
        this.eegSignalProcessing = eegSignalProcessing

        // disable/enable status trigger operation
        val triggerChar =
            measurementService!!.getCharacteristic(MelomindCharacteristic.HEADSET_STATUS.uuid)
        val triggerOp = if (eegSignalProcessing.isEEGEnabled) {
            enableNotifications(triggerChar)
                .done { Timber.d("HEADSET_STATUS enabled") }
                .fail { _, _ -> Timber.e("Could not subscribe HEADSET_STATUS!") }
        } else {
            disableNotifications(triggerChar)
                .done { Timber.d("HEADSET_STATUS disabled") }
                .fail { _, _ -> Timber.e("Could not subscribe HEADSET_STATUS!") }
        }

        // eeg characteristic
        val eegChar =
            measurementService!!.getCharacteristic(MelomindCharacteristic.EEG_ACQUISITION.uuid)

        // setup eeg callback
        setNotificationCallback(eegChar).with { device, data ->
            if (data.value != null) {
                eegSignalProcessing.onEEGFrame(data.value!!)
            } else {
                eegSignalProcessing.eegListener?.onEegError(Throwable("received empty eeg frame!"))
            }
        }

        // configure headset trigger status then enable EEG
        beginAtomicRequestQueue()
            .add(triggerOp)
            .add(
                enableNotifications(eegChar)
                    .done {
                        Timber.d("EEG_ACQUISITION enabled")
                        eegSignalProcessing.onEEGStatusChange(true)
                    }
                    .fail { _, _ ->
                        Timber.e("Could not enable EEG_ACQUISITION")
                        eegSignalProcessing.eegListener?.onEegError(Throwable("could not start EEG"))
                    }
            )
            .enqueue()
    }

    override fun stopEeg() {
        if (deviceInformationService == null || measurementService == null) {
            eegSignalProcessing?.eegListener?.onEegError(Throwable("required services not found!"))
            return
        }

        // eeg characteristic
        val eegChar =
            measurementService!!.getCharacteristic(MelomindCharacteristic.EEG_ACQUISITION.uuid)

        disableNotifications(eegChar)
            .done {
                Timber.d("EEG_ACQUISITION disabled")
                eegSignalProcessing?.onEEGStatusChange(false)
            }
            .fail { _, _ ->
                Timber.e("Could not disable EEG_ACQUISITION")
                eegSignalProcessing?.eegListener?.onEegError(Throwable("could not stop EEG"))
            }
            .enqueue()
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

    override fun handleScanResults(results: List<BluetoothDevice>) {
        Timber.d("found melomind devices : number = ${results.size}")
        val list = results.map { MbtDevice(it) }
        if (list.isNotEmpty()) {
            scanResultListener.onMbtDevices(list)
        }
    }

    //----------------------------------------------------------------------------
    // MARK: private inner class
    //----------------------------------------------------------------------------
    private inner class MelomindGattCallback() :
        BleManager.BleManagerGattCallback() {

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
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (bluetoothDevice.bondState != BluetoothDevice.BOND_BONDED) {
                    bluetoothDevice.createBond()
                }
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
}
