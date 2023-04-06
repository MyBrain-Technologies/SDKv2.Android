package com.mybraintech.sdk.core.acquisition

import android.os.Handler
import android.os.Looper
import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessingHyperion
import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessingMelomind
import com.mybraintech.sdk.core.acquisition.eeg.EEGSignalProcessingQPlus
import com.mybraintech.sdk.core.acquisition.ims.AccelerometerSignalProcessingDisabled
import com.mybraintech.sdk.core.acquisition.ims.AccelerometerSignalProcessingIndus5
import com.mybraintech.sdk.core.acquisition.ppg.PPGSignalProcessingDisabled
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.*
import com.mybraintech.sdk.core.recording.BaseAccelerometerRecorder
import com.mybraintech.sdk.core.recording.BaseEEGRecorder
import io.reactivex.Maybe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.FileWriter
import kotlin.math.max
import kotlin.math.min

/**
 * please call [dispose] to release occupied memory when stop using
 */
internal class SignalProcessingManager(
    val deviceType: EnumMBTDevice, val streamingParams: StreamingParams
) : RecordingInterface, MbtDataReceiver {

    private val bleFrameScheduler = RxJavaPlugins.createSingleScheduler(AcquisierThreadFactory)

    private var eegListener: EEGListener? = null
    private val eegCallback = object : BaseEEGRecorder.EEGCallback {
        override fun onNewEEG(eegPacket: MbtEEGPacket) {
            eegListener?.onEegPacket(eegPacket)
        }
    }

    private var accelerometerListener: AccelerometerListener? = null
    private val accelerometerCallback by lazy {
        object : AccelerometerSignalProcessingIndus5.AccelerometerCallback {
            override fun onAccelerometerPacket(packet: AccelerometerPacket) {
                accelerometerListener?.onAccelerometerPacket(packet)
            }
        }
    }

    private var isRecording = false

    /**
     * use to calibrate multi signal recordings
     */
    private val recordingSignals by lazy { ArrayList<EnumSignalType>() }

    private val lateSignalHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    /**
     * use to calibrate multi signal recordings
     */
    private var isWaitingLateSignals = false

    private var recordingOption: RecordingOption? = null
    private var kwak: Kwak = Kwak()
    private var recordingListener: RecordingListener? = null

    private var recordingDisposable = CompositeDisposable()

    private var eegSignalProcessing: BaseEEGRecorder = when (deviceType) {
        EnumMBTDevice.Q_PLUS -> {
            EEGSignalProcessingQPlus(streamingParams, eegCallback, bleFrameScheduler)
        }
        EnumMBTDevice.MELOMIND -> {
            EEGSignalProcessingMelomind(streamingParams, eegCallback, bleFrameScheduler)
        }
        EnumMBTDevice.HYPERION -> {
            EEGSignalProcessingHyperion(streamingParams, eegCallback, bleFrameScheduler)
        }
        else -> {
            throw UnsupportedOperationException("device type not known")
        }
    }

    private var accelerometerSignalProcessing: BaseAccelerometerRecorder = when (deviceType) {
        EnumMBTDevice.Q_PLUS -> {
            AccelerometerSignalProcessingIndus5(accelerometerCallback, bleFrameScheduler)
        }
        else -> {
            AccelerometerSignalProcessingDisabled()
        }
    }

    private val ppgSignalProcessing by lazy { PPGSignalProcessingDisabled() }

    fun dispose() {
        eegSignalProcessing.dispose()
        recordingDisposable.dispose()
    }

    //----------------------------------------------------------------------------
    // MARK: RecordingInterface
    //----------------------------------------------------------------------------
    override fun startRecording(
        recordingListener: RecordingListener, recordingOption: RecordingOption
    ) {
        Timber.d("startRecording")

        this.recordingOption = recordingOption
        this.recordingListener = recordingListener
        this.kwak = KwakBuilder().createKwak(
            deviceType, recordingOption, streamingParams.isQualityCheckerEnabled
        )

        isRecording = true
        isWaitingLateSignals = false

        recordingSignals.clear()
        if (streamingParams.isEEGEnabled) {
            recordingSignals.add(EnumSignalType.EEG)
            eegSignalProcessing.startRecording()
        }
        if (streamingParams.isAccelerometerEnabled) {
            recordingSignals.add(EnumSignalType.ACCELEROMETER)
            accelerometerSignalProcessing.startRecording()
        }
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
    override fun stopRecording() {
        Timber.d("stopRecording")

        var minLen = Int.MAX_VALUE
        for (signalType in recordingSignals) {
            minLen = when (signalType) {
                EnumSignalType.EEG -> {
                    min(minLen, eegSignalProcessing.getBufferSize())
                }
                EnumSignalType.ACCELEROMETER -> {
                    min(minLen, accelerometerSignalProcessing.getBufferSize())
                }
                EnumSignalType.PPG -> {
                    min(minLen, ppgSignalProcessing.getBufferSize())
                }
            }
        }

        if (recordingSignals.size < 2) {
            isWaitingLateSignals = false
            stopAllRecording()
            saveRecording(minLen)
        } else {
            var maxLen = 0
            for (signalType in recordingSignals) {
                maxLen = when (signalType) {
                    EnumSignalType.EEG -> {
                        max(maxLen, eegSignalProcessing.getBufferSize())
                    }
                    EnumSignalType.ACCELEROMETER -> {
                        max(maxLen, accelerometerSignalProcessing.getBufferSize())
                    }
                    EnumSignalType.PPG -> {
                        max(maxLen, ppgSignalProcessing.getBufferSize())
                    }
                }
            }
            if (minLen == maxLen) {
                isWaitingLateSignals = false
                stopAllRecording()
                saveRecording(minLen)
            } else {
                isWaitingLateSignals = true
                lateSignalHandler.postDelayed(
                    {
                        if (isWaitingLateSignals) {
                            isWaitingLateSignals = false
                            stopAllRecording()
                            saveRecording(minLen + 1)
                        }
                    }, 1000
                )
            }
        }
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
     *
     * Follows these steps to calibrate recording file :
     * 1. Step 1 :
     *     - if there is only one signal enabled, stop recording immediately, otherwise switch to step 2 .
     * 2. Step 2 :
     *     - calculate the min values of recording buffer lengths (of EEG, Accelerometer,...).
     * 3. Step 3 (end ) :
     *     - if min >= trim value : stop all recording and trim.
     *     - if min < trim value : try to get the signals of second (min + 1) for all signals.
     */
    override fun stopRecording(trim: Int) {
        Timber.d("stopRecording : length = $trim")
        if (recordingSignals.size < 2) {
            stopAllRecording()
            saveRecording(trim)
        } else {
            var min = 0
            for (signalType in recordingSignals) {
                min = when (signalType) {
                    EnumSignalType.EEG -> {
                        min(min, eegSignalProcessing.getBufferSize())
                    }
                    EnumSignalType.ACCELEROMETER -> {
                        min(min, accelerometerSignalProcessing.getBufferSize())
                    }
                    EnumSignalType.PPG -> {
                        min(min, ppgSignalProcessing.getBufferSize())
                    }
                }
            }
            if (min >= trim) {
                isWaitingLateSignals = false
                stopAllRecording()
                saveRecording(trim)
            } else {
                isWaitingLateSignals = true
                lateSignalHandler.postDelayed(
                    {
                        if (isWaitingLateSignals) {
                            isWaitingLateSignals = false
                            stopAllRecording()
                            saveRecording(trim)
                        }
                    }, 1000
                )
            }
        }
    }

    private fun stopAllRecording() {
        Timber.d("stopAllRecording")

        for (signalType in recordingSignals) {
            when (signalType) {
                EnumSignalType.EEG -> {
                    eegSignalProcessing.stopRecording()
                }
                EnumSignalType.ACCELEROMETER -> {
                    accelerometerSignalProcessing.stopRecording()
                }
                EnumSignalType.PPG -> {
                    ppgSignalProcessing.stopRecording()
                }
            }
        }
        isRecording = false
    }

    private fun saveRecording(trim: Int) {
        Timber.d("trim = $trim")
        Timber.d("eegBuffer.size = ${eegSignalProcessing.getBufferSize()}")
        Timber.d("imsBuffer.size = ${accelerometerSignalProcessing.getBufferSize()}")

        var eegBuffer: List<MbtEEGPacket>
        val eegErrorCounter: EEGStreamingErrorCounter

        if (recordingSignals.contains(EnumSignalType.EEG)) {
            eegBuffer = eegSignalProcessing.getBuffer()
            if (eegBuffer.size > trim) {
                eegBuffer = eegBuffer.subList(0, trim)
            }
            eegErrorCounter = eegSignalProcessing.getRecordingErrorData()
        } else {
            eegBuffer = emptyList()
            eegErrorCounter = EEGStreamingErrorCounter()
        }

        var imsBuffer: List<ThreeDimensionalPosition>
        if (recordingSignals.contains(EnumSignalType.ACCELEROMETER)) {
            imsBuffer = accelerometerSignalProcessing.getBuffer()
            val endIndex = trim * streamingParams.accelerometerSampleRate.sampleRate
            if (imsBuffer.size > endIndex) {
                imsBuffer = imsBuffer.subList(0, endIndex)
            }
        } else {
            imsBuffer = emptyList()
        }

        saveRecording(streamingParams, eegBuffer, eegErrorCounter, imsBuffer)
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

    override fun onAccelerometerConfiguration(accelerometerConfig: AccelerometerConfig) {
        accelerometerSignalProcessing.onAccelerometerConfiguration(accelerometerConfig)
        if (streamingParams.isAccelerometerEnabled) {
            /**
             * if headset does not support [EnumAccelerometerSampleRate] change:
             * we overwrite the real value in the [streamingParams] and notify user if needed
             */
            if (streamingParams.accelerometerSampleRate != accelerometerConfig.sampleRate) {
                with("Accelerometer frequency is ${accelerometerConfig.sampleRate.sampleRate} Hz, it is not equal expected value [${streamingParams.accelerometerSampleRate.sampleRate}]") {
                    Timber.e(this)
                    accelerometerListener?.onAccelerometerError(Throwable(this))
                }
                streamingParams.accelerometerSampleRate = accelerometerConfig.sampleRate
            }
        }
    }

    override fun onEEGFrame(data: TimedBLEFrame) {
        eegSignalProcessing.addSignalData(data)
    }

    override fun onAccelerometerFrame(data: ByteArray) {
        accelerometerSignalProcessing.addSignalData(data)
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

    override fun onEEGFilterConfig(config: EnumEEGFilterConfig) {
        Timber.d("onEEGFilterConfig : ${config.name}")
    }

    override fun onEEGDataError(error: Throwable) {
        eegListener?.onEegError(error)
    }

    //----------------------------------------------------------------------------
    // MARK:
    //----------------------------------------------------------------------------
    private fun saveRecording(
        streamingParams: StreamingParams,
        eegBuffer: List<MbtEEGPacket>,
        eegErrorData: EEGStreamingErrorCounter,
        imsBuffer: List<ThreeDimensionalPosition>
    ) {
        Maybe.fromCallable {
            if (recordingOption?.outputFile != null) {
                val isOk = kwak.serializeJson(
                    streamingParams,
                    eegBuffer,
                    eegErrorData,
                    imsBuffer,
                    FileWriter(recordingOption?.outputFile!!)
                )
                if (!isOk) {
                    recordingListener?.onRecordingError(RuntimeException("Can not serialize file"))
                } else {
                    recordingListener?.onRecordingSaved(recordingOption?.outputFile!!)
                }
            } else {
                recordingListener?.onRecordingError(RuntimeException("outputFile is null"))
            }
        }
            .observeOn(Schedulers.computation())
            .subscribe()
            .addTo(recordingDisposable)
    }
}