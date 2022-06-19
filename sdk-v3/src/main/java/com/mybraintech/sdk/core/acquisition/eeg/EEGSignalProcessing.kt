package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.android.jnibrainbox.QualityChecker
import com.mybraintech.sdk.core.listener.EEGListener
import com.mybraintech.sdk.core.listener.RecordingListener
import com.mybraintech.sdk.core.model.*
import com.mybraintech.sdk.util.ErrorDataHelper2
import com.mybraintech.sdk.util.MatrixUtils2
import com.mybraintech.sdk.util.NumericalUtils
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.FileWriter

abstract class EEGSignalProcessing(
    private val sampleRate: Int,
    protocol: EnumBluetoothProtocol,
    val isTriggerStatusEnabled: Boolean,
    protected val isQualityCheckerEnabled: Boolean,
) {

    /**
     * this scheduler is reserved to handle eeg frame tasks
     */
    private var eegFrameScheduler = RxJavaPlugins.createSingleScheduler(EEGThreadFactory)

    private val eegFrameSubject = PublishSubject.create<ByteArray>()
    private val eegPacketSubject = PublishSubject.create<MbtEEGPacket2>()

    private var recordingOption: RecordingOption? = null
    private var kwak: Kwak = Kwak()
    private var recordingBuffer = mutableListOf<MbtEEGPacket2>()
    private var recordingListener: RecordingListener? = null
    var isRecording: Boolean = false
        private set

    /**
     * index allocation size in the eeg frame
     */
    var indexAlloc = protocol.getFrameIndexAllocationSize()

    /**
     * status allocation size must be set in real time when trigger command is sent. By default it is set to 0
     */
    protected var statusAlloc = 0

    /**
     * frame = header + data
     *
     * header = frame index + trigger
     *
     * data = eeg signals : For QPlus : ch1-t0 | ch2-t0 | ... | ch4-t0 | ch1-t1 | ch2-t1 | ... | ch4-t1 | ...
     */
    var headerAlloc = indexAlloc
        protected set

    private var previousIndex = -1L

    var isEEGEnabled = false
        private set

    var eegListener: EEGListener? = null

    private var recordingErrorData = RecordingErrorData2()

    /**
     * Buffer that will manage the EEG <b>RAW</b> data. It stores {@link RawEEGSample2} objects.
     */
    private var rawBuffer: MutableList<RawEEGSample2> = mutableListOf()

    /**
     * Object that will manage the EEG <b>CONSOLIDATED</b> data.
     */
    private var consolidatedEEGBuffer = ArrayList<ArrayList<Float>>()
    private var consolidatedStatusBuffer = ArrayList<Float>()

    private val dataConversion = MbtDataConversion2.Builder().buildForQPlus()

    private var qualityChecker: QualityChecker? = null

    private var recordingContainer = CompositeDisposable()
    private var otherContainer = CompositeDisposable()

    init {
        eegPacketSubject
            .observeOn(Schedulers.io())
            .subscribe {
                eegListener?.onEegPacket(it)
            }
            .addTo(otherContainer)

        eegFrameSubject
            .observeOn(eegFrameScheduler)
            .subscribe {
                try {
                    consumeEEGFrame(it)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            .addTo(otherContainer)
    }

    fun startRecording(recordingListener: RecordingListener, recordingOption: RecordingOption) {
        isRecording = true
        this.recordingOption = recordingOption
        this.recordingListener = recordingListener
        this.recordingErrorData.resetData()
        this.recordingBuffer.clear()
        this.kwak = createKwak(recordingOption)
    }

    abstract fun createKwak(recordingOption: RecordingOption): Kwak

    fun stopRecording() {
        isRecording = false
        Observable.just(1)
            .subscribeOn(Schedulers.computation())
            .subscribe {
                try {
                    if (recordingOption?.outputFile != null) {
                        val isOk = kwak.serializeJson(
                            isTriggerStatusEnabled,
                            recordingBuffer,
                            recordingErrorData,
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
                }
            }
            .addTo(recordingContainer)
    }

    fun onEEGStatusChange(isEnabled: Boolean) {
        this.isEEGEnabled = isEnabled
        if (isEnabled) {
            //reset buffer when starting streaming
            restart()
        } else {
            if (isRecording) {
                stopRecording()
            }
        }
    }

    private fun restart() {
        recordingErrorData = RecordingErrorData2()
        rawBuffer = mutableListOf()
        recordingBuffer = mutableListOf()
        if (isQualityCheckerEnabled) {
            qualityChecker = QualityChecker(sampleRate)
        }
    }

    fun onEEGFrame(eegFrame: ByteArray) {
        Timber.v("onEEGFrame : ${NumericalUtils.bytesToShortString(eegFrame)}")
        eegFrameSubject.onNext(eegFrame)
    }

    /**
     * please wrap this method in try catch to avoid crashing
     */
    @Throws(Exception::class)
    private fun consumeEEGFrame(eegFrame: ByteArray) {
//        Timber.v("onEEGFrameComputation")
        if (!isValidFrame(eegFrame)) {
            Timber.e("bad format eeg frame : ${NumericalUtils.bytesToShortString(eegFrame)}")
            return
        }

        //1st step : check index
        val newFrameIndex = getFrameIndex(eegFrame)
//        Timber.v("newFrameIndex = $newFrameIndex")

        if (previousIndex == -1L) {
            //init first frame index
            previousIndex = newFrameIndex - 1
        }

        if (recordingErrorData.startingIndex == -1L) {
            recordingErrorData.startingIndex = newFrameIndex
        }
        recordingErrorData.currentIndex = newFrameIndex

        val indexDifference = newFrameIndex - previousIndex
        if (indexDifference != 1L) {
            recordingErrorData.increaseMissingEegFrame(indexDifference - 1)
        }

        //this block is to count zero signals
        val eegData = eegFrame.copyOfRange(headerAlloc, eegFrame.size)
        with(ErrorDataHelper2.countZeroSample(eegData, getNumberOfChannels())) {
            if (this.first != 1) {
                recordingErrorData.increaseZeroSampleCounter(this.first.toLong())
            }
            if (this.second != 1) {
                recordingErrorData.increaseZeroTimeCounter(this.second.toLong())
            }
        }

        val rawEEGList = mutableListOf<RawEEGSample2>()

        //2nd step : Fill gap by NaN samples if there is missing frames
        if (indexDifference != 1L) {
            Timber.w("diff is $indexDifference. Current index : $newFrameIndex | previousIndex : $previousIndex")
            for (i in 1..indexDifference) {
                //one frame contains n times of sample
                for (j in 1..getNumberOfTimes(eegFrame)) {
                    rawEEGList.add(RawEEGSample2.NAN_PACKET)
                }
            }
        }

        //3rd step: Parse the new frame
        val rawEEGSamples: List<RawEEGSample2> = getEEGData(eegFrame)
        rawEEGList.addAll(rawEEGSamples)

        //4th step: save raw eeg data to buffer
        rawBuffer.addAll(rawEEGList)

        previousIndex = newFrameIndex

        //5th step: if raw buffer is reach threshold, generate consolidated eeg
        if (rawBuffer.size >= DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE) {
            val consolidatedEEG = dataConversion.convertRawDataToEEG(rawBuffer)
            val statuses = rawBuffer.map { it.statusData }
            //all raw data is converted to consolidated data, clear raw buffer
            rawBuffer.clear()
            consolidatedEEGBuffer.addAll(consolidatedEEG)
            consolidatedStatusBuffer.addAll(statuses)
            val count = consolidatedEEGBuffer.size
            if (count >= sampleRate) {
                //consolidated buffer can emit a MBTPacket
                val dataInverted = ArrayList(consolidatedEEGBuffer.subList(0, sampleRate))
                val newEegData = MatrixUtils2.invertFloatMatrix(dataInverted)
                val newStatusData = ArrayList(consolidatedStatusBuffer.subList(0, sampleRate))
                consolidatedEEGBuffer = ArrayList(consolidatedEEGBuffer.subList(sampleRate, count))
                consolidatedStatusBuffer =
                    ArrayList(consolidatedStatusBuffer.subList(sampleRate, count))
                val newPacket = MbtEEGPacket2(newEegData, newStatusData)
                if (isQualityCheckerEnabled) {
                    try {
                        val qualities = qualityChecker?.computeQualityChecker(newEegData)
                        if (qualities != null) {
                            newPacket.qualities = ArrayList(qualities.toList())
                        } else {
                            newPacket.qualities = ArrayList<Float>(sampleRate)
                            newPacket.qualities.fill(Float.NaN)
                        }
                    } catch (e: Exception) {
                        Timber.e(e)
                        newPacket.qualities = ArrayList<Float>(sampleRate)
                        newPacket.qualities.fill(Float.NaN)
                    }
                }

                if (isRecording) {
                    recordingBuffer.add(newPacket)
                }

                eegPacketSubject.onNext(newPacket)
            }
        }
    }

    /**
     * @param eegFrame eeg frame starting with index frame number
     */
    abstract fun getEEGData(eegFrame: ByteArray): List<RawEEGSample2>

    /**
     * We count the number of times eeg signal was captured in one eeg frame.
     *
     * Eg: For mtu = 47, number of channels = 4, there are 5 times of sample in one eeg frame.
     *
     * @param eegFrame eeg frame starting with index frame number
     */
    protected fun getNumberOfTimes(eegFrame: ByteArray): Int {
        return (eegFrame.size - headerAlloc) / (SIGNAL_ALLOC * getNumberOfChannels())
    }

    abstract fun getFrameIndex(eegFrame: ByteArray): Long

    abstract fun isValidFrame(eegFrame: ByteArray): Boolean

    abstract fun getNumberOfChannels(): Int

    fun getEEGBufferSize(): Int {
        return recordingBuffer.size
    }

    fun onTriggerStatusConfiguration(statusAllocationSize: Int) {
        statusAlloc = statusAllocationSize
        headerAlloc = indexAlloc + statusAlloc
        Timber.d("indexAlloc = $indexAlloc | statusAlloc = $statusAlloc | headerAlloc = $headerAlloc")
    }

    fun getDataLossPercent(): Float {
        return recordingErrorData.getMissingPercent()
    }

    companion object {
        const val SIGNAL_ALLOC = 2 //one EEG signal is 2 bytes

        /**
         * features.MbtFeatures.DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE
         */
        const val DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE = 40
    }
}