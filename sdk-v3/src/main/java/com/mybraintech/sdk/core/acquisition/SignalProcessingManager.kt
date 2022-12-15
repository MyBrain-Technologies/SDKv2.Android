package com.mybraintech.sdk.core.acquisition

import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessing
import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessingHyperion
import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessingMelomind
import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessingQPlus
import com.mybraintech.sdk.core.acquisition.ims.AccelerometerSignalProcessing
import com.mybraintech.sdk.core.acquisition.ims.AccelerometerSignalProcessingDisabled
import com.mybraintech.sdk.core.acquisition.ims.AccelerometerSignalProcessingQPlus
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.*
import com.mybraintech.sdk.util.toJson
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.FileWriter
import kotlin.math.max

/**
 * please call [terminate] to release occupied memory when stop using
 */
internal class SignalProcessingManager(
    val deviceType: EnumMBTDevice,
    val streamingParams: StreamingParams
) : RecordingInterface,
    MbtDataReceiver, AccelerometerListener {

    private var eegListener: EEGListener? = null
    private val eegCallback = object : EEGSignalProcessing.EEGCallback {
        override fun onNewEEG(eegPacket: MbtEEGPacket) {
            eegListener?.onEegPacket(eegPacket)
        }
    }

    private var accelerometerListener: AccelerometerListener? = null
    private val accelerometerCallback by lazy {
        object : AccelerometerSignalProcessingQPlus.AccelerometerCallback {
            override fun onAccelerometerPacket(packet: AccelerometerPacket) {
                accelerometerListener?.onAccelerometerPacket(packet)
            }
        }
    }

    private var isRecording = false
    private var isWaitingLateSignals = false
    private val lateSignals by lazy { mutableListOf<EnumSignalType>() }

    private var recordingOption: RecordingOption? = null
    private var kwak: Kwak = Kwak()
    private var recordingListener: RecordingListener? = null

//    private val eegRelaxIndexProcessor: EEGToRelaxIndexProcessor = EEGToRelaxIndexProcessor()

    private var recordingDisposable = CompositeDisposable()

    private var eegSignalProcessing: EEGSignalProcessing = when (deviceType) {
        EnumMBTDevice.Q_PLUS -> {
            EEGSignalProcessingQPlus(streamingParams, eegCallback)
        }
        EnumMBTDevice.MELOMIND -> {
            EEGSignalProcessingMelomind(streamingParams, eegCallback)
        }
        EnumMBTDevice.HYPERION -> {
            EEGSignalProcessingHyperion(streamingParams, eegCallback)
        }
        else -> {
            throw UnsupportedOperationException("device type not known")
        }
    }

    private var accelerometerSignalProcessing: AccelerometerSignalProcessing = when (deviceType) {
        EnumMBTDevice.Q_PLUS -> {
            AccelerometerSignalProcessingQPlus(
                sampleRate = 100, protocol = EnumBluetoothProtocol.BLE,
                accelerometerCallback = accelerometerCallback
            )
        }
        else -> {
            AccelerometerSignalProcessingDisabled()
        }
    }

    fun terminate() {
        eegSignalProcessing.terminate()
        recordingDisposable.dispose()
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
        isWaitingLateSignals = false

        if (streamingParams.isEEGEnabled) {
            eegSignalProcessing.startRecording()
        }
        if (streamingParams.isAccelerometerEnabled) {
            accelerometerSignalProcessing.startRecording()
        }
    }

    override fun stopRecording() {
        Timber.d("stopRecording")
        internalStopRecording(NO_TRIM)
    }

    override fun stopRecording(length: Long) {
        Timber.d("stopRecording : length = $length")
        internalStopRecording(length)
    }

    /**
     * stop the recording process and handle recording lengths.
     *
     * For example if there is 30 seconds of EEG but only 29 seconds of Accelerometer,
     * we may wait a little bit to receive the 30th second of Accelerometer to have a completed
     * recording.
     *
     * But if user want to trim it to 25 seconds, we can stop the recording on both EEG and
     * Accelerometer immediately and export the data.
     */
    private fun internalStopRecording(length: Long) {
        Timber.d("internalStopRecording")

        if (streamingParams.isEEGEnabled && streamingParams.isAccelerometerEnabled) {
            Timber.d("start to stop recording : streaming options = isEEGEnabled ON | isAccelerometerEnabled ON")
            val eegSize = eegSignalProcessing.getBufferSize()
            val imsSize = accelerometerSignalProcessing.getBufferSize()
            if (eegSize == imsSize) {
                Timber.d("recording is completed on both EEG and Accelerometer : isRecording is set to False ")
                isWaitingLateSignals = false
            } else {
                if (length == NO_TRIM) {
                    lateSignals.clear()
                    val expected = max(eegSize, imsSize)
                    if (expected > eegSize) {
                        lateSignals.add(EnumSignalType.EEG)
                    } else {
                        eegSignalProcessing.stopRecording()
                    }
                    if (expected > imsSize) {
                        lateSignals.add(EnumSignalType.ACCELEROMETER)
                    } else {
                        accelerometerSignalProcessing.stopRecording()
                    }
                    if (lateSignals.isEmpty()) {
                        Timber.e("error: lateSignals is empty | Action : Ignore | Expected : lateSignals is not empty.")
                        isWaitingLateSignals = false
                    } else {
                        Timber.i("wait late signals : ${lateSignals.toJson()}")
                        isWaitingLateSignals = true
                    }
                } else {

                }
            }
        }

        if (isWaitingLateSignals) {
            if (!isRecording) {
                Timber.e("error: isRecording is False | Action : Ignore | Expected : we would continue to record on the late signals.")
            }
            if (streamingParams.isEEGEnabled) {
                if (!lateSignals.contains(EnumSignalType.EEG)) {
                    eegSignalProcessing.stopRecording()
                }
            }
            if (streamingParams.isAccelerometerEnabled) {
                if (!lateSignals.contains(EnumSignalType.ACCELEROMETER)) {
                    accelerometerSignalProcessing.stopRecording()
                }
            }
        } else {
            Timber.v("isRecording is set to False")
            isRecording = false
            if (streamingParams.isEEGEnabled) {
                eegSignalProcessing.stopRecording()
            }
            if (streamingParams.isAccelerometerEnabled) {
                accelerometerSignalProcessing.stopRecording()
            }
            //todo here
            val eegBuffer = eegSignalProcessing.getBuffer()
            Timber.d("eegBuffer.size = ${eegBuffer.size}")
            val eegErrorData = eegSignalProcessing.getRecordingErrorData()
            val imsBuffer = accelerometerSignalProcessing.getBuffer()
            Timber.d("imsBuffer.size = ${imsBuffer.size}")
            generateRecording(eegBuffer, eegErrorData, imsBuffer)
        }
    }

    override fun clearBuffer() {
        eegSignalProcessing.clearBuffer()
        accelerometerSignalProcessing.clearBuffer()
    }

    override fun isRecordingEnabled(): Boolean {
        return isRecording
    }

    override fun getRecordingBufferSize(): Int {
        return if (streamingParams.isEEGEnabled) {
            eegSignalProcessing.getBufferSize()
        } else {
            accelerometerSignalProcessing.getBufferSize()
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

    override fun onAccelerometerFrame(data: ByteArray) {
        accelerometerSignalProcessing.onFrame(data)
    }

    override fun setEEGListener(eegListener: EEGListener?) {
        this.eegListener = eegListener
    }

    override fun setEEGRealtimeListener(eegRealtimeListener: EEGRealtimeListener?) {
        this.eegSignalProcessing.setRealtimeListener(eegRealtimeListener)
    }

    override fun setAccelerometerListener(accelerometerListener: AccelerometerListener?) {
        this.accelerometerListener = accelerometerListener
    }

    override fun onEEGDataError(error: Throwable) {
        eegListener?.onEegError(error)
    }

    //----------------------------------------------------------------------------
    // MARK: AccelerometerListener
    //----------------------------------------------------------------------------
    override fun onAccelerometerStatusChange(isEnabled: Boolean) {
        // this should not be called since this class only consume data but not eeg status
        Timber.w("should not be called ! onIMSStatusChange : $isEnabled")
    }

    override fun onAccelerometerPacket(accelerometerPacket: AccelerometerPacket) {
        accelerometerListener?.onAccelerometerPacket(accelerometerPacket)

        if (isRecording) {

        }
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
        eegBuffer: List<MbtEEGPacket>,
        eegErrorData: RecordingErrorData2,
        imsBuffer: List<ThreeDimensionalPosition>
    ) {
        Observable.just(Unit)
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
                    recordingDisposable.clear()
                }
            }
            .addTo(recordingDisposable)
    }

    companion object {
        private const val NO_TRIM = -1L
    }
}