package com.mybraintech.sdk.core

import android.content.Context
import com.mybraintech.sdk.MbtClient
import com.mybraintech.sdk.core.acquisition.MbtDeviceStatusCallback
import com.mybraintech.sdk.core.acquisition.RecordingInterface
import com.mybraintech.sdk.core.acquisition.SignalProcessingManager
import com.mybraintech.sdk.core.bluetooth.MbtDeviceInterface
import com.mybraintech.sdk.core.bluetooth.devices.hyperion.HyperionDeviceImpl
import com.mybraintech.sdk.core.bluetooth.devices.melomind.MelomindDeviceImpl
import com.mybraintech.sdk.core.bluetooth.devices.qplus.QPlusDeviceImpl
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.*
import timber.log.Timber

/**
 * DO NOT USE THIS CLASS OUTSIDE OF THE SDK
 * This is new class to support Q+ device, Melomind device...
 */
internal class MbtClientImpl(
    @Suppress("CanBeParameter") private val context: Context,
    private var deviceType: EnumMBTDevice
) :
    MbtClient, MbtDeviceStatusCallback {

    private lateinit var manager: SignalProcessingManager
    private var mbtDeviceInterface: MbtDeviceInterface
    private var recordingInterface: RecordingInterface? = null
    private var dataReceiver: MbtDataReceiver? = null
    private var streamingParams: StreamingParams? = null

    private var eegListener: EEGListener? = null
    private var accelerometerListener: AccelerometerListener? = null
    private var eegRealtimeListener: EEGRealtimeListener? = null

    private var isStreaming = false
    private var isEEGEnabled = false
    private var isIMSEnabled = false

    init {
        when (deviceType) {
            EnumMBTDevice.Q_PLUS -> {
                mbtDeviceInterface = QPlusDeviceImpl(context)
                recordingInterface = null
            }
            EnumMBTDevice.MELOMIND -> {
                mbtDeviceInterface = MelomindDeviceImpl(context)
                recordingInterface = null
            }
            EnumMBTDevice.HYPERION -> {
                mbtDeviceInterface = HyperionDeviceImpl(context)
                recordingInterface = null
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
        return mbtDeviceInterface.getBleConnectionStatus()
    }

    override fun startScan(scanResultListener: ScanResultListener) {
        mbtDeviceInterface.startScan(scanResultListener)
    }

    override fun stopScan() {
        mbtDeviceInterface.stopScan()
    }

    override fun connect(mbtDevice: MbtDevice, connectionListener: ConnectionListener) {
        val connectionStatus = mbtDeviceInterface.getBleConnectionStatus()
        if (connectionStatus.isConnectionEstablished && connectionStatus.mbtDevice == mbtDevice) {
            connectionListener.onDeviceReady()
        } else {
            mbtDeviceInterface.connectMbt(mbtDevice, connectionListener)
        }
    }

    override fun disconnect() {
        mbtDeviceInterface.disconnectMbt()
    }

    override fun getBatteryLevel(batteryLevelListener: BatteryLevelListener) {
        mbtDeviceInterface.getBatteryLevel(batteryLevelListener)
    }

    override fun getDeviceInformation(deviceInformationListener: DeviceInformationListener) {
        mbtDeviceInterface.getDeviceInformation(deviceInformationListener)
    }

    override fun getStreamingState(sensorStatusListener: SensorStatusListener) {
        mbtDeviceInterface.getSensorStatuses(sensorStatusListener)
    }

    override fun startStreaming(streamingParams: StreamingParams) {
        this.streamingParams = streamingParams

        // dispose old SignalProcessingManager then create a new one
        if (::manager.isInitialized) {
            manager.terminate()
        }
        manager = SignalProcessingManager(deviceType, streamingParams)

        this.recordingInterface = manager
        this.dataReceiver = manager.apply {
            setEEGListener(eegListener)
            setAccelerometerListener(accelerometerListener)
            setEEGRealtimeListener(eegRealtimeListener)
        }
        mbtDeviceInterface.enableSensors(streamingParams, dataReceiver!!, this)
    }

    override fun stopStreaming() {
        if (isRecordingEnabled()) {
            stopRecording()
        }
        mbtDeviceInterface.disableSensors()
        isStreaming = false
    }

    override fun setEEGListener(eegListener: EEGListener) {
        this.eegListener = eegListener
        this.dataReceiver?.setEEGListener(eegListener)
    }

    @LabStreamingLayer
    override fun setEEGRealtimeListener(eegRealtimeListener: EEGRealtimeListener) {
        this.eegRealtimeListener = eegRealtimeListener
        this.dataReceiver?.setEEGRealtimeListener(eegRealtimeListener)
    }

    override fun setAccelerometerListener(accelerometerListener: AccelerometerListener) {
        this.accelerometerListener = accelerometerListener
        this.dataReceiver?.setAccelerometerListener(accelerometerListener)
    }

    override fun startRecording(
        recordingOption: RecordingOption,
        recordingListener: RecordingListener
    ) {
        if (isStreaming) {
            recordingInterface?.startRecording(
                recordingListener,
                recordingOption
            )
        } else {
            recordingListener.onRecordingError(Throwable("Streaming is not activated yet!"))
        }
    }

    override fun stopRecording() {
        if (isRecordingEnabled()) {
            recordingInterface?.stopRecording()
        } else {
            Timber.e("Recording is not enabled")
        }
    }

    override fun stopRecording(trim: Int) {
        if (isRecordingEnabled()) {
            recordingInterface?.stopRecording(trim)
        } else {
            Timber.e("Recording is not enabled")
        }
    }

    override fun isEEGEnabled(): Boolean {
        return isEEGEnabled
    }

    override fun isRecordingEnabled(): Boolean {
        return (recordingInterface?.isRecordingEnabled() == true)
    }

    override fun getRecordingBufferSize(): Int {
        return if (!isRecordingEnabled()) {
            0
        } else {
            recordingInterface?.getRecordingBufferSize() ?: -1
        }
    }

    //----------------------------------------------------------------------------
    // MARK: MbtDeviceStatusCallback
    //----------------------------------------------------------------------------
    override fun onEEGStatusChange(isEnabled: Boolean) {
        Timber.i("onEEGStatusChange = $isEnabled")
        isEEGEnabled = isEnabled
        updateStreamingStatus()
        eegListener?.onEEGStatusChange(isEnabled)
    }

    override fun onIMSStatusChange(isEnabled: Boolean) {
        Timber.i("onIMSStatusChange = $isEnabled")
        isIMSEnabled = isEnabled
        updateStreamingStatus()
        accelerometerListener?.onAccelerometerStatusChange(isEnabled)
    }

    override fun onEEGStatusError(error: Throwable) {
        eegListener?.onEegError(error)
    }

    override fun onIMSStatusError(error: Throwable) {
        accelerometerListener?.onAccelerometerError(error)
    }

    private fun updateStreamingStatus() {
        if (streamingParams == null) {
            isStreaming = false
        } else {
            isStreaming = (isEEGEnabled == streamingParams!!.isEEGEnabled)
                    && (isIMSEnabled == streamingParams!!.isAccelerometerEnabled)
        }
    }

    override fun getDataLossPercent(): Float {
        return recordingInterface?.getDataLossPercentage() ?: 0.0f
    }

    @TestBench
    override fun setSerialNumber(serialNumber: String, listener: SerialNumberChangedListener?) {
        mbtDeviceInterface.setSerialNumber(serialNumber, listener)
    }

    @TestBench
    override fun setAudioName(audioName: String, listener: AudioNameListener?) {
        mbtDeviceInterface.setAudioName(audioName, listener)
    }

    @TestBench
    override fun getDeviceSystemStatus(deviceSystemStatusListener: DeviceSystemStatusListener) {
        mbtDeviceInterface.getDeviceSystemStatus(deviceSystemStatusListener)
    }
}