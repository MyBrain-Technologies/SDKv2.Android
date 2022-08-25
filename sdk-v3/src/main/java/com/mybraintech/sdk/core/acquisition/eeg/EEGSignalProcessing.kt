package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.android.jnibrainbox.QualityChecker
import com.mybraintech.sdk.core.acquisition.AcquisierThreadFactory
import com.mybraintech.sdk.core.acquisition.EnumBluetoothProtocol
import com.mybraintech.sdk.core.listener.EEGListener
import com.mybraintech.sdk.core.listener.StreamListener
import com.mybraintech.sdk.core.model.*
import com.mybraintech.sdk.util.ErrorDataHelper2
import com.mybraintech.sdk.util.MatrixUtils2
import com.mybraintech.sdk.util.NumericalUtils
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import kotlin.math.pow

abstract class EEGSignalProcessing(
    private val sampleRate: Int,
    protocol: EnumBluetoothProtocol,
    val isTriggerStatusEnabled: Boolean,
    protected val isQualityCheckerEnabled: Boolean,
    var eegListener: EEGListener?
) {

    /**
     * this scheduler is reserved to handle eeg frame tasks
     */
    private var eegFrameScheduler = RxJavaPlugins.createSingleScheduler(AcquisierThreadFactory)

    private val eegFrameSubject = PublishSubject.create<TimedBLEFrame>()
    private val eegPacketSubject = PublishSubject.create<MbtEEGPacket2>()

    private var recordingBuffer = mutableListOf<MbtEEGPacket2>()
    private var recordingErrorData = RecordingErrorData2()
    private var isRecording: Boolean = false

    /**
     * we use timestamps to construct MBTPacket
     */
    private var timeCalibrationPhase: TimeCalibrationPhase = TimeCalibrationPhase.INACTIVE
    private var calibratingQueue = mutableListOf<TimedBLEFrame>()
    private var firstStartTime: Long = 0L
    private var currentStartTime: Long = 0L
    private var calibratedEndTime: Long = 0L

    /**
     * index allocation size in the eeg frame
     */
    var indexAlloc = protocol.getFrameIndexAllocationSize()
    private val indexCycle = 2.0.pow(indexAlloc * 8).toLong()

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

    /**
     * number of time the ble frame index does overflow
     */
    private var indexOverflowCount = 0L
    private var previousIndex = -1L

    var streamListener: StreamListener? = null

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

    private var qualityChecker: QualityChecker = QualityChecker(sampleRate)

    private var disposable = CompositeDisposable()

    init {
        eegPacketSubject
            .observeOn(Schedulers.io())
            .subscribe {
                eegListener?.onEegPacket(it)
            }
            .addTo(disposable)

        eegFrameSubject
            .observeOn(eegFrameScheduler)
            .subscribe {
                try {
                    internalOnEEGFrame(it)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            .addTo(disposable)

        Timber.i("BLE frame indexCycle = $indexCycle")
    }

    /**
     * this will clear the buffer
     */
    fun startRecording() {
        isRecording = true
        clearBuffer()
    }

    fun stopRecording() {
        isRecording = false
    }

    abstract fun createKwak(recordingOption: RecordingOption): Kwak

    fun clearBuffer() {
        recordingErrorData = RecordingErrorData2()
        rawBuffer = mutableListOf()
        recordingBuffer = mutableListOf()
        if (isQualityCheckerEnabled) {
            qualityChecker = QualityChecker(sampleRate)
        }
    }

    fun onEEGFrame(eegFrame: TimedBLEFrame) {
//        Timber.v("onEEGFrame : ${NumericalUtils.bytesToShortString(eegFrame)}")
        eegFrameSubject.onNext(eegFrame)
    }

    /**
     * please wrap this method in try catch to avoid crashing
     */
    @Throws(Exception::class)
    private fun internalOnEEGFrame(timedEegFrame: TimedBLEFrame) {
        Timber.d("internalOnEEGFrame : frame timestamp = ${timedEegFrame.timestamp}")
        when (timeCalibrationPhase) {
            TimeCalibrationPhase.INACTIVE -> {
                timeCalibrationPhase = TimeCalibrationPhase.CALIBRATING
                Timber.d("timeCalibrationPhase = ${timeCalibrationPhase.name}")
                addDataToCalibratingQueue(timedEegFrame)
            }
            TimeCalibrationPhase.CALIBRATING -> {
                if (timedEegFrame.timestamp - calibratingQueue[0].timestamp < ONE_SECOND) {
                    addDataToCalibratingQueue(timedEegFrame)
                } else {
                    timeCalibrationPhase = TimeCalibrationPhase.CALIBRATED
                    Timber.d("timeCalibrationPhase = ${timeCalibrationPhase.name}")
                    calibrateTimestamps(timedEegFrame)
                }
            }
            TimeCalibrationPhase.CALIBRATED -> {
                if (timedEegFrame.timestamp >= firstStartTime) {
                    timeCalibrationPhase = TimeCalibrationPhase.BUFFERING
                    Timber.d("timeCalibrationPhase = ${timeCalibrationPhase.name}")
                    consumeEEGFrame(timedEegFrame)
                } else {
                    // CALIBRATED phase but the packet is not in the first start - end range, ignore frame
                }
            }
            TimeCalibrationPhase.BUFFERING -> {
                consumeEEGFrame(timedEegFrame)
            }
            TimeCalibrationPhase.ERROR -> {
                // if we can not calibrate timestamps, we proceeds without it
                consumeEEGFrame(timedEegFrame)
            }
        }
    }

    private fun calibrateTimestamps(nextSecondEegFrame: TimedBLEFrame) {
        Timber.d("calibrateTimestamps")
        if (calibratingQueue.size < MINIMUM_CALIBRATING_QUEUE_SIZE) {
            Timber.e("EEG samples received in calibrating phase is not enough : frame count = ${calibratingQueue.size}")

            // mark calibration process as Error and continue without Frequency Control
            timeCalibrationPhase = TimeCalibrationPhase.ERROR
            Timber.d("timeCalibrationPhase = ${timeCalibrationPhase.name}")
            consumeEEGFrame(nextSecondEegFrame)
            return //end procedure
        }
        var sum = 0L
        for (entry in calibratingQueue) {
            sum += entry.timestamp
        }
        val zeroCenterPoint: Long = sum / calibratingQueue.size
        Timber.d("zeroCenterPoint = $zeroCenterPoint")
        firstStartTime = zeroCenterPoint + HALF_SECOND
        currentStartTime = firstStartTime
        Timber.d("currentStartTime = $currentStartTime")
        val currentEndTime = currentStartTime + ONE_SECOND

        // consume data arrive after current start time which is in calibrating queue
        var afterStartTimeIndex = -1
        var index = calibratingQueue.size - 1
        while ((index >= 0) && (calibratingQueue[index].timestamp >= currentStartTime)) {
            afterStartTimeIndex = index
            index--
        }
        if (afterStartTimeIndex != -1) {
            for (i in afterStartTimeIndex until calibratingQueue.size) {
                Timber.d("push frame to next MBTPacket")
                if (timeCalibrationPhase == TimeCalibrationPhase.CALIBRATED) {
                    timeCalibrationPhase = TimeCalibrationPhase.BUFFERING
                    Timber.d("timeCalibrationPhase = ${timeCalibrationPhase.name}")
                }
                consumeEEGFrame(calibratingQueue[i])
            }
        }

        // consume new data
        if (nextSecondEegFrame.timestamp >= currentStartTime) {
            if (timeCalibrationPhase == TimeCalibrationPhase.CALIBRATED) {
                timeCalibrationPhase = TimeCalibrationPhase.BUFFERING
                Timber.d("timeCalibrationPhase = ${timeCalibrationPhase.name}")
            }
            consumeEEGFrame(nextSecondEegFrame)
        }
    }

    /**
     * please wrap this method in try catch to avoid crashing
     */
    @Throws(Exception::class)
    private fun consumeEEGFrame(timedEegFrame: TimedBLEFrame) {
//        Timber.v("consumeEEGFrame")
        val eegFrame = timedEegFrame.data
        if (!isValidFrame(eegFrame)) {
            Timber.e("bad format eeg frame : ${NumericalUtils.bytesToShortString(eegFrame)}")
            return
        }

        //1st step : check index
        var rawIndex = getFrameIndex(eegFrame)
        val indexCandidate = indexOverflowCount * indexCycle + rawIndex
        if (indexCandidate < previousIndex) {
            //index in ble frame is from 0 to (2^16 - 1), this bracket is entered when the raw index does overflow
            indexOverflowCount++
            Timber.i("increase indexOverflowCount : indexOverflowCount = $indexOverflowCount")
        }
        val newFrameIndex = indexOverflowCount * indexCycle + rawIndex
        Timber.v("newFrameIndex = $newFrameIndex")

        if (previousIndex == -1L) {
            //init first frame index
            previousIndex = newFrameIndex - 1
        }

        if (recordingErrorData.startingIndex == -1L) {
            recordingErrorData.startingIndex = newFrameIndex
        }
        recordingErrorData.currentIndex = newFrameIndex

        val indexDifference = newFrameIndex - previousIndex
        val missingFrame = indexDifference - 1
        if (missingFrame > 0) {
            recordingErrorData.increaseMissingEegFrame(missingFrame)
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
        if (missingFrame > 0) {
            Timber.w("diff is $indexDifference. Current index : $newFrameIndex | previousIndex : $previousIndex")
            for (i in 1..missingFrame) {
                //one frame contains n times of sample
                for (j in 1..getNumberOfTimes(eegFrame)) {
                    rawEEGList.add(RawEEGSample2.NAN_PACKET)
                }
            }
//            Timber.i("missing size = n channels * sample per frame = ${rawEEGList.size}")
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
                        val qualities = qualityChecker.computeQualityChecker(newEegData)
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

    private fun addDataToCalibratingQueue(timedEegFrame: TimedBLEFrame) {
        calibratingQueue.add(timedEegFrame)
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

    fun getEEGBuffer(): List<MbtEEGPacket2> {
        return recordingBuffer
    }

    fun getRecordingErrorData(): RecordingErrorData2 {
        return recordingErrorData
    }

    fun getDataLossPercent(): Float {
        return recordingErrorData.getMissingPercent()
    }

    companion object {
        const val SIGNAL_ALLOC = 2 //one EEG signal is 2 bytes

        const val ONE_SECOND = 1000
        const val HALF_SECOND = 500
        const val GRACE_OFFSET = 200 //ms

        /**
         * (MTE = 47) for Q+ there is around 50 BLE frames each second, for Melomind is around 25 BLE frames
         */
        const val MINIMUM_CALIBRATING_QUEUE_SIZE = 5

        /**
         * features.MbtFeatures.DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE
         */
        const val DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE = 40
    }

    private enum class TimeCalibrationPhase {
        INACTIVE, CALIBRATING, CALIBRATED, BUFFERING, ERROR
    }
}