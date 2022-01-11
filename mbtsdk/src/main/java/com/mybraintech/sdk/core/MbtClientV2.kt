package com.mybraintech.sdk.core

import android.content.Context
import com.mybraintech.sdk.MbtClient
import com.mybraintech.sdk.core.acquisition.eeg.SignalProcessingManager
import com.mybraintech.sdk.core.bluetooth.IMbtBleManager
import com.mybraintech.sdk.core.bluetooth.qplus.Indus5BleManager
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.EEGParams
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.MbtDevice

/**
 * DO NOT USE THIS CLASS OUTSIDE OF THE SDK
 * MbtClientV2 is new class to support Q+ device, Melomind device...
 */
class MbtClientV2(private val context: Context, private val deviceType: EnumMBTDevice) : MbtClient {

    private val mbtBleManager : IMbtBleManager
    private var signalProcessingManager: SignalProcessingManager? = null

    init {
        if (deviceType == EnumMBTDevice.Q_PLUS) {
            mbtBleManager = Indus5BleManager(context)
        } else {
            TODO("implement for other device types than Q Plus")
        }
    }

    override fun getDeviceType(): EnumMBTDevice {
        return deviceType
    }

    override fun getBleConnectionStatus(): BleConnectionStatus {
        return mbtBleManager.getBleConnectionStatus()
    }

    override fun startScan(scanResultListener: ScanResultListener) {
        mbtBleManager.startScan(scanResultListener)
    }

    override fun stopScan() {
        mbtBleManager.stopScan()
    }

    override fun connect(mbtDevice: MbtDevice, connectionListener: ConnectionListener) {
        mbtBleManager.connectMbt(mbtDevice, connectionListener)
    }

    override fun disconnect() {
        mbtBleManager.disconnectMbt()
    }

    override fun getBatteryLevel(batteryLevelListener: BatteryLevelListener) {
        mbtBleManager.getBatteryLevel(batteryLevelListener)
    }

    override fun getDeviceInformation(deviceInformationListener: DeviceInformationListener) {
        mbtBleManager.getDeviceInformation(deviceInformationListener)
    }

    override fun startEEG(eegListener: EEGListener, eegParams: EEGParams) {
        signalProcessingManager = SignalProcessingManager(deviceType, eegParams)
        signalProcessingManager?.eegSignalProcessing?.eegListener = eegListener
        signalProcessingManager?.eegSignalProcessing?.let {
            mbtBleManager.startEeg(it)
        }
    }

    override fun stopEEG() {
        mbtBleManager.stopEeg()
    }
}