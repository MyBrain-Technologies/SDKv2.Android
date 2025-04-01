package com.mybraintech.sdk.core

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.mybraintech.android.jnibrainbox.ComputeStatistics
import com.mybraintech.android.jnibrainbox.RelaxIndexSessionOutputData
import com.mybraintech.sdk.MbtClient
import com.mybraintech.sdk.core.acquisition.EEGCalibrateResult
import com.mybraintech.sdk.core.acquisition.EEGRecordedDatas
import com.mybraintech.sdk.core.acquisition.MbtDeviceStatusCallback
import com.mybraintech.sdk.core.acquisition.RecordingInterface
import com.mybraintech.sdk.core.acquisition.SignalProcessingManager
import com.mybraintech.sdk.core.bluetooth.MbtDeviceInterface
import com.mybraintech.sdk.core.bluetooth.devices.BaseMbtDevice
import com.mybraintech.sdk.core.bluetooth.devices.EnumBluetoothConnection
import com.mybraintech.sdk.core.bluetooth.devices.hyperion.HyperionDeviceImpl
import com.mybraintech.sdk.core.bluetooth.devices.melomind.MelomindDeviceImpl
import com.mybraintech.sdk.core.bluetooth.devices.qplus.QPlusDeviceImpl
import com.mybraintech.sdk.core.bluetooth.devices.xon.XonDeviceImpl
import com.mybraintech.sdk.core.listener.AccelerometerConfigListener
import com.mybraintech.sdk.core.listener.AccelerometerListener
import com.mybraintech.sdk.core.listener.AudioNameListener
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.core.listener.DeviceInformationListener
import com.mybraintech.sdk.core.listener.DeviceSystemStatusListener
import com.mybraintech.sdk.core.listener.EEGFilterConfigListener
import com.mybraintech.sdk.core.listener.EEGListener
import com.mybraintech.sdk.core.listener.EEGRealtimeListener
import com.mybraintech.sdk.core.listener.MbtDataReceiver
import com.mybraintech.sdk.core.listener.RecordingListener
import com.mybraintech.sdk.core.listener.ScanResultListener
import com.mybraintech.sdk.core.listener.SensorStatusListener
import com.mybraintech.sdk.core.listener.SerialNumberChangedListener
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.MbtDevice
import com.mybraintech.sdk.core.model.RecordingOption
import com.mybraintech.sdk.core.model.StreamingParams
import com.mybraintech.sdk.util.BLE_CONNECTED_STATUS
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
        set(value) {
            field = value

            Timber.d("%s%s", "Dev_debug isRecordingAllowed was set to:", value)
        }
    private var isEEGEnabled = false
    private var isIMSEnabled = false

    private var mConnectionMode = EnumBluetoothConnection.BLE

    init {
        Timber.d("Dev_debug set up sdk with new version 7")
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
            EnumMBTDevice.XON -> {
                mbtDeviceInterface = XonDeviceImpl(context)
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

    override fun startScan(targetName:String,scanResultListener: ScanResultListener) {
        mbtDeviceInterface.startScan(targetName,scanResultListener)
    }

    override fun stopScan() {
        mbtDeviceInterface.stopScan()
    }

    override fun startScanAudio(targetName: String, scanResultListener: ScanResultListener?) {
        mbtDeviceInterface.startScanAudio(targetName, scanResultListener)
    }

    override fun stopScanAudio() {
        mbtDeviceInterface.stopScanAudio()
    }

    //TODO: optional parameter
    override fun connect(
        mbtDevice: MbtDevice,
        connectionListener: ConnectionListener,
        connectionMode: EnumBluetoothConnection
    ) {
        mConnectionMode = connectionMode
        (mbtDeviceInterface as? MelomindDeviceImpl)?.connectionMode = mConnectionMode
        val connectionStatus = mbtDeviceInterface.getBleConnectionStatus()
        if (connectionStatus.isConnectionEstablished && connectionStatus.mbtDevice == mbtDevice) {
            Timber.i("Dev_debug case BLE already connected called BLE_CONNECTED_STATUS")
            connectionListener.onDeviceReady(BLE_CONNECTED_STATUS)
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                mbtDeviceInterface.connectMbt(mbtDevice, connectionListener, connectionMode)
            }, 200)
        }
    }

    private fun removeBond(device: BluetoothDevice) {
        try {
            device::class.java.getMethod("removeBond").invoke(device)
        } catch (e: Exception) {
            Timber.i("Removing bond has been failed: ${e.message}")
        }
    }

    override fun connectAudio(mbtDevice: MbtDevice, connectionListener: ConnectionListener) {
        mbtDeviceInterface.connectAudio(mbtDevice,connectionListener)
    }

    override fun connectAudioViaBLE(connectionListener: ConnectionListener) {
        (mbtDeviceInterface as? MelomindDeviceImpl)?.connectAudioViaBle()
    }

    override fun disconnectAudio(mbtDevice: BluetoothDevice?) {
       mbtDeviceInterface.disconnectAudio(mbtDevice)
    }

    override fun disconnectAudioViaBLE() {
        (mbtDeviceInterface as? MelomindDeviceImpl)?.disconnectAudioViaBLE()
    }
    override fun rebootHeadset() {
        (mbtDeviceInterface as? MelomindDeviceImpl)?.rebootHeadset()
    }

    override fun getA2DP(): BluetoothA2dp? {
       return (mbtDeviceInterface as? BaseMbtDevice)?.a2dp
    }

    override fun initA2DP() {
        (mbtDeviceInterface as? BaseMbtDevice)?.innitA2dpService()
    }

    override fun disconnect() {
        mbtDeviceInterface.disconnectMbt()
    }

    override fun removeBondBle() {
        mbtDeviceInterface.removeBondMbt()
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
        Timber.i("Dev_debug onEEGStatusChange = $isEnabled")
        isEEGEnabled = isEnabled
        isRecordingAllowed = isStreamingFullyStarted()
        eegListener?.onEEGStatusChange(isEnabled)
    }

    override fun onIMSStatusChange(isEnabled: Boolean) {
        Timber.i("Dev_debug onIMSStatusChange = $isEnabled")
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

    override fun eegCalibration(data: EEGRecordedDatas): EEGCalibrateResult {
     return manager.eegCalibrate(data)
    }

    override fun eegRelaxingIndex(eegs:EEGRecordedDatas): Float {
        return manager.eegRelaxingIndex(eegs)
    }

    override fun eggStartRelaxingIndexSession(data: EEGCalibrateResult) {
         manager.innitRelaxingIndex(data)
    }

    override fun eggEngRelaxingIndexSession(): RelaxIndexSessionOutputData? {
       return manager.endSessionRelaxingIndex()
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

    override fun computeStatistics(threshold: Float, snrValues: Array<Float>): HashMap<String, Float>?{
        Log.d("TAG",
            "Dev_debug SDKV3 computeStatistics called threshold:$threshold snrValues:$snrValues "
        )
        try {
            val computer = ComputeStatistics()
            Log.d("TAG",
                "Dev_debug SDKV3 computeStatistics computer:$computer"
            )
            val result = computer.computeStatisticsSNR(threshold,snrValues)
            Log.d("TAG",
                "Dev_debug SDKV3 computeStatistics result:$result"
            )
            return  result
        } catch (ex:Exception) {
            return null
        }

    }
}