package com.mybraintech.sdk.core

import android.content.Context
import com.mybraintech.sdk.MbtClient
import com.mybraintech.sdk.core.acquisition.eeg.SignalProcessingManager
import com.mybraintech.sdk.core.bluetooth.IMbtBleManager
import com.mybraintech.sdk.core.bluetooth.devices.melomind.MelomindBleManager
import com.mybraintech.sdk.core.bluetooth.devices.qplus.QPlusBleManager
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.*
import timber.log.Timber

/**
 * DO NOT USE THIS CLASS OUTSIDE OF THE SDK
 * This is new class to support Q+ device, Melomind device...
 */
internal class MbtClientImpl(private val context: Context, private var deviceType: EnumMBTDevice) :
    MbtClient {

    private var mbtBleManager: IMbtBleManager
    private var signalProcessingManager: SignalProcessingManager? = null

    init {
        when (deviceType) {
            EnumMBTDevice.Q_PLUS -> {
                mbtBleManager = QPlusBleManager(context)
                signalProcessingManager = null
            }
            EnumMBTDevice.MELOMIND -> {
                mbtBleManager = MelomindBleManager(context)
                signalProcessingManager = null
            }
            else -> {
                throw UnsupportedOperationException("device type is not supported!")
            }
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

    override fun startEEG(eegParams: EEGParams, eegListener: EEGListener) {
        signalProcessingManager = SignalProcessingManager(deviceType, eegParams)
        signalProcessingManager?.eegSignalProcessing?.let {
            it.eegListener = eegListener
            mbtBleManager.startEeg(it)
        }
    }

    override fun stopEEG() {
        mbtBleManager.stopEeg()
    }

    override fun startEEGRecording(
        recordingOption: RecordingOption,
        recordingListener: RecordingListener
    ) {
        Timber.d("ART : startEEGRecording : ${System.currentTimeMillis()}")
        if (isEEGEnabled()) {
            if (!isRecordingEnabled()) {
                signalProcessingManager!!.eegSignalProcessing.startRecording(
                    recordingListener,
                    recordingOption
                )
            } else {
                recordingListener.onRecordingError(Throwable("Recording is enabled already"))
            }
        } else {
            recordingListener.onRecordingError(Throwable("EEG is not enabled"))
        }
    }

    override fun stopEEGRecording() {
        if (isRecordingEnabled()) {
            signalProcessingManager?.eegSignalProcessing?.stopRecording()
        } else {
            Timber.e("Recording is not enabled")
        }
    }

    override fun isEEGEnabled(): Boolean {
        return (signalProcessingManager?.eegSignalProcessing?.isEEGEnabled == true)
    }

    override fun isRecordingEnabled(): Boolean {
        return isEEGEnabled() && (signalProcessingManager?.eegSignalProcessing?.isRecording == true)
    }

    override fun getRecordingBufferSize(): Int {
        if (!isRecordingEnabled()) {
            return 0
        } else {
            return signalProcessingManager?.eegSignalProcessing?.getEEGBufferSize() ?: -1
        }
    }

    override fun getDataLossPercent(): Float {
        return signalProcessingManager?.eegSignalProcessing?.getDataLossPercent() ?: 0.0f
    }
}