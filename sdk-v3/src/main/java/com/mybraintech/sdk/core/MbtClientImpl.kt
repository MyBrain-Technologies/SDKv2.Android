package com.mybraintech.sdk.core

import android.content.Context
import com.mybraintech.sdk.MbtClient
import com.mybraintech.sdk.core.acquisition.MbtDeviceStatusCallback
import com.mybraintech.sdk.core.acquisition.RecordingInterface
import com.mybraintech.sdk.core.acquisition.SignalProcessingManager
import com.mybraintech.sdk.core.bluetooth.MbtDeviceInterface
import com.mybraintech.sdk.core.bluetooth.devices.EnumBluetoothConnection
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
    private var eegFilterConfigListener: EEGFilterConfigListener? = null

    /**
     * [startStreaming] takes times, this value is to prevent recording function while the [startStreaming] procedure is not finished
     */
    private var isRecordingAllowed = false
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
    //TODO: optional parameter
    override fun connect(mbtDevice: MbtDevice, connectionListener: ConnectionListener, connectionMode: EnumBluetoothConnection) {
        val connectionStatus = mbtDeviceInterface.getBleConnectionStatus()
        if (connectionStatus.isConnectionEstablished && connectionStatus.mbtDevice == mbtDevice) {
            connectionListener.onDeviceReady("BLE device")
        } else {
            mbtDeviceInterface.connectMbt(mbtDevice, connectionListener, connectionMode)
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
            manager.dispose()
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
        isRecordingAllowed = false
        if (isRecordingEnabled()) {
            stopRecording()
        }
        mbtDeviceInterface.disableSensors()
    }

    override fun isEEGEnabled(): Boolean {
        return mbtDeviceInterface.isEEGEnabled()
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

    @ResearchStudy
    override fun getEEGFilterConfig(listener: EEGFilterConfigListener) {
        this.eegFilterConfigListener = listener
        mbtDeviceInterface.getEEGFilterConfig(listener)
    }

    @TestBench
    override fun getAccelerometerConfig(accelerometerConfigListener: AccelerometerConfigListener) {
        mbtDeviceInterface.getAccelerometerConfig(accelerometerConfigListener)
    }

    override fun setAccelerometerListener(accelerometerListener: AccelerometerListener) {
        this.accelerometerListener = accelerometerListener
        this.dataReceiver?.setAccelerometerListener(accelerometerListener)
    }

    override fun startRecording(
        recordingOption: RecordingOption,
        recordingListener: RecordingListener
    ) {
        if (isRecordingAllowed) {
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
            isRecordingAllowed = isStreamingFullyStarted()
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
        isRecordingAllowed = isStreamingFullyStarted()
        eegListener?.onEEGStatusChange(isEnabled)
    }

    override fun onIMSStatusChange(isEnabled: Boolean) {
        Timber.i("onIMSStatusChange = $isEnabled")
        isIMSEnabled = isEnabled
        isRecordingAllowed = isStreamingFullyStarted()
        accelerometerListener?.onAccelerometerStatusChange(isEnabled)
    }

    override fun onEEGStatusError(error: Throwable) {
        eegListener?.onEegError(error)
    }

    override fun onIMSStatusError(error: Throwable) {
        accelerometerListener?.onAccelerometerError(error)
    }

    @Suppress("LiftReturnOrAssignment")
    private fun isStreamingFullyStarted(): Boolean {
        val result =
            if (streamingParams != null) {
                val eegCondition = (isEEGEnabled == streamingParams!!.isEEGEnabled)
                val imsCondition = (isIMSEnabled == streamingParams!!.isAccelerometerEnabled)
                eegCondition && imsCondition
            } else {
                false
            }
        Timber.d("isStreamingFullyStarted = $result")
        return result
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