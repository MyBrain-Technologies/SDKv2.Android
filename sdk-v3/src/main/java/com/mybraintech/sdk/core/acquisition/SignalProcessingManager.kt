package com.mybraintech.sdk.core.acquisition

import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessing
import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessingHyperion
import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessingMelomind
import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessingQPlus
import com.mybraintech.sdk.core.acquisition.ims.IMSSignalProcessing
import com.mybraintech.sdk.core.acquisition.ims.IMSSignalProcessingDisabled
import com.mybraintech.sdk.core.acquisition.ims.IMSSignalProcessingQPlus
import com.mybraintech.sdk.core.listener.AccelerometerListener
import com.mybraintech.sdk.core.listener.EEGListener
import com.mybraintech.sdk.core.listener.MbtDataReceiver
import com.mybraintech.sdk.core.listener.RecordingListener
import com.mybraintech.sdk.core.model.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.FileWriter

internal class SignalProcessingManager(
    val deviceType: EnumMBTDevice,
    val streamingParams: StreamingParams
) : RecordingInterface,
    MbtDataReceiver, EEGListener, AccelerometerListener {

    private var eegListener: EEGListener? = null
    private var accelerometerListener: AccelerometerListener? = null
    private var isRecording = false

    private var recordingOption: RecordingOption? = null
    private var kwak: Kwak = Kwak()
    private var recordingListener: RecordingListener? = null

//    private val eegRelaxIndexProcessor: EEGToRelaxIndexProcessor = EEGToRelaxIndexProcessor()

    private var recordingContainer = CompositeDisposable()

    private var eegSignalProcessing: EEGSignalProcessing = when (deviceType) {
        EnumMBTDevice.Q_PLUS -> {
            EEGSignalProcessingQPlus(streamingParams, this)
        }
        EnumMBTDevice.MELOMIND -> {
            EEGSignalProcessingMelomind(streamingParams, this)
        }
        EnumMBTDevice.HYPERION -> {
            EEGSignalProcessingHyperion(streamingParams, this)
        }
        else -> {
            throw UnsupportedOperationException("device type not known")
        }
    }

    private var imsSignalProcessing: IMSSignalProcessing = when (deviceType) {
        EnumMBTDevice.Q_PLUS -> {
            IMSSignalProcessingQPlus(
                sampleRate = 100, protocol = EnumBluetoothProtocol.BLE,
                accelerometerListener = this
            )
        }
        else -> {
            IMSSignalProcessingDisabled()
        }
    }

    //----------------------------------------------------------------------------
    // MARK: RecordingInterface
    //----------------------------------------------------------------------------
    override fun startRecording(
        recordingListener: RecordingListener,
        recordingOption: RecordingOption
    ) {
        Timber.d("startRecording")

        this.recordingOption = recordingOption
        this.recordingListener = recordingListener
        this.kwak = eegSignalProcessing.createKwak(recordingOption)

        isRecording = true
        if (streamingParams.isEEGEnabled) {
            eegSignalProcessing.startRecording()
        }
        if (streamingParams.isAccelerometerEnabled) {
            imsSignalProcessing.startIMSRecording()
        }
    }

    override fun stopRecording() {
        Timber.d("stopRecording")
        isRecording = false
        if (streamingParams.isEEGEnabled) {
            eegSignalProcessing.stopRecording()
        }
        if (streamingParams.isAccelerometerEnabled) {
            imsSignalProcessing.stopIMSRecording()
        }

        val eegBuffer = eegSignalProcessing.getEEGBuffer()
        val eegErrorData = eegSignalProcessing.getRecordingErrorData()
        val imsBuffer = imsSignalProcessing.getIMSBuffer()
        generateRecording(eegBuffer, eegErrorData, imsBuffer)
    }

    override fun clearBuffer() {
        eegSignalProcessing.clearBuffer()
        imsSignalProcessing.clearIMSBuffer()
    }

    override fun isRecordingEnabled(): Boolean {
        return isRecording
    }

    override fun getRecordingBufferSize(): Int {
        return if (streamingParams.isEEGEnabled) {
            eegSignalProcessing.getEEGBufferSize()
        } else {
            imsSignalProcessing.getIMSBufferSize()
        }
    }

    override fun getDataLossPercentage(): Float {
        return eegSignalProcessing.getRecordingErrorData().getMissingPercent()
    }

    //----------------------------------------------------------------------------
    // MARK: MbtDataReceiver
    //----------------------------------------------------------------------------
    override fun onTriggerStatusConfiguration(triggerStatusAllocationSize: Int) {
        eegSignalProcessing.onTriggerStatusConfiguration(triggerStatusAllocationSize)
    }

    override fun onEEGFrame(data: TimedBLEFrame) {
        eegSignalProcessing.onEEGFrame(data)
    }

    override fun onIMSFrame(data: ByteArray) {
        imsSignalProcessing.onIMSFrame(data)
    }

    override fun setEEGListener(eegListener: EEGListener?) {
        this.eegListener = eegListener
    }

    override fun setIMSListener(accelerometerListener: AccelerometerListener?) {
        this.accelerometerListener = accelerometerListener
    }

    override fun onEEGDataError(error: Throwable) {
        eegListener?.onEegError(error)
    }

    //----------------------------------------------------------------------------
    // MARK: EEGListener
    //----------------------------------------------------------------------------
    override fun onEEGStatusChange(isEnabled: Boolean) {
        // this should not be called since this class only consume data but not eeg status
        Timber.w("should not be called ! onEEGStatusChange : $isEnabled")
    }

    override fun onEegPacket(eegPacket: MbtEEGPacket2) {
        eegListener?.onEegPacket(eegPacket)
    }

    override fun onEegError(error: Throwable) {
        eegListener?.onEegError(error)
    }

    //----------------------------------------------------------------------------
    // MARK: AccelerometerListener
    //----------------------------------------------------------------------------
    override fun onIMSStatusChange(isEnabled: Boolean) {
        // this should not be called since this class only consume data but not eeg status
        Timber.w("should not be called ! onIMSStatusChange : $isEnabled")
    }

    override fun onAccelerometerPacket(imsPacket: ImsPacket) {
        accelerometerListener?.onAccelerometerPacket(imsPacket)
    }

    override fun onAccelerometerError(error: Throwable) {
        accelerometerListener?.onAccelerometerError(error)
    }

    //----------------------------------------------------------------------------
    // MARK: no override
    //----------------------------------------------------------------------------
    fun isTriggerStatusEnabled(): Boolean {
        return eegSignalProcessing.isTriggerStatusEnabled
    }

    private fun generateRecording(
        eegBuffer: List<MbtEEGPacket2>,
        eegErrorData: RecordingErrorData2,
        imsBuffer: List<ThreeDimensionalPosition>
    ) {
        Observable.just(1)
            .subscribeOn(Schedulers.computation())
            .subscribe {
                try {
                    if (recordingOption?.outputFile != null) {
                        val isOk = kwak.serializeJson(
                            eegSignalProcessing.isTriggerStatusEnabled,
                            eegBuffer,
                            eegErrorData,
                            imsBuffer,
                            FileWriter(recordingOption?.outputFile!!)
                        )
                        if (!isOk) {
                            recordingListener?.onRecordingError(Throwable("Can not serialize file"))
                        } else {
                            recordingListener?.onRecordingSaved(recordingOption?.outputFile!!)
                        }
                    } else {
                        recordingListener?.onRecordingError(Throwable("Recording file not found"))
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    recordingListener?.onRecordingError(e)
                } finally {
                    recordingListener = null
                    recordingContainer.clear()
                }
            }
            .addTo(recordingContainer)
    }
}