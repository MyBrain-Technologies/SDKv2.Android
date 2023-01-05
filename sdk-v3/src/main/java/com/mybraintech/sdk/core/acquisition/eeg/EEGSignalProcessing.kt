package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.android.jnibrainbox.QualityChecker
import com.mybraintech.sdk.BuildConfig
import com.mybraintech.sdk.core.acquisition.*
import com.mybraintech.sdk.core.listener.EEGFrameDecodeInterface
import com.mybraintech.sdk.core.model.*
import com.mybraintech.sdk.core.recording.BaseEEGRecorder
import com.mybraintech.sdk.util.ErrorDataHelper2
import com.mybraintech.sdk.util.MatrixUtils2
import com.mybraintech.sdk.util.NumericalUtils
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow

abstract class EEGSignalProcessing(
    protocol: EnumBluetoothProtocol,
    val isTriggerStatusEnabled: Boolean,
    protected val isQualityCheckerEnabled: Boolean,
    callback: EEGCallback?,
    val eegFrameScheduler: Scheduler
) : BaseEEGRecorder(callback), EEGFrameDecodeInterface {

    private val eegFrameSubject = PublishSubject.create<TimedBLEFrame>()
    private val eegRealtimeSubject = PublishSubject.create<EEGSignalPack>()
    private val eegPacketSubject = PublishSubject.create<MbtEEGPacket>()

    private var recordingBuffer = mutableListOf<MbtEEGPacket>()
    private var eegStreamingErrorCounter = EEGStreamingErrorCounter()
    private var isRecording: Boolean = false

    /**
     * index allocation size in the eeg frame
     */
    protected var indexAlloc = protocol.getFrameIndexAllocationSize()
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

    /**
     * table : size = [ number of samples * number of channels ]
     */
    private var consolidatedEEGBuffer = ArrayList<ArrayList<Float>>()

    private var consolidatedStatusBuffer = ArrayList<Float>()

    private val dataConversion: MbtDataConversion2 by lazy {
        MbtDataConversion2.generateInstance(getDeviceType())
    }

    private var qualityChecker: QualityChecker = QualityChecker(250)

    init {
        eegFrameSubject
            .observeOn(eegFrameScheduler)
            .subscribe(
                ::consumeEEGFrame,
                Timber::e
            )
            .addTo(disposable)

        eegRealtimeSubject
            .observeOn(Schedulers.io())
            .subscribe(
                ::notifyRealtime,
                Timber::e
            )
            .addTo(disposable)

        eegPacketSubject
            .observeOn(Schedulers.io())
            .subscribe(
                ::notifyPacket,
                Timber::e
            )
            .addTo(disposable)

        Timber.i("BLE frame indexCycle = $indexCycle")
    }

    protected abstract fun getDeviceType(): EnumMBTDevice

    /**
     * this will clear the buffer
     */
    override fun startRecording() {
        isRecording = true
        clearBuffer()
    }

    override fun stopRecording() {
        Timber.v("stopRecording")
        isRecording = false
    }

    override fun isRecording(): Boolean {
        return isRecording
    }

    override fun clearBuffer() {
        eegStreamingErrorCounter = EEGStreamingErrorCounter()
        recordingBuffer = mutableListOf()
        consolidatedEEGBuffer.clear()
        consolidatedStatusBuffer.clear()
        if (isQualityCheckerEnabled) {
            qualityChecker = QualityChecker(getSampleRate())
        }
    }

    override fun addSignalData(data: TimedBLEFrame) {
//        Timber.v("onEEGFrame : ${NumericalUtils.bytesToShortString(eegFrame)}")
        eegFrameSubject.onNext(data)
    }

    /**
     * please wrap this method in try catch to avoid crashing
     */
    @Throws(Exception::class)
    private fun consumeEEGFrame(timedBLEFrame: TimedBLEFrame) {
//        Timber.v("consumeEEGFrame")
        val eegFrame = timedBLEFrame.data
        if (!isValidFrame(eegFrame)) {
            Timber.e("bad format eeg frame : ${NumericalUtils.bytesToShortString(eegFrame)}")
            return
        }

        // 1st step : check index
        val rawIndex = getFrameIndex(eegFrame)
        val indexCandidate = indexOverflowCount * indexCycle + rawIndex
        if (indexCandidate < previousIndex) {
            //index in ble frame is from 0 to (2^16 - 1), this bracket is entered when the raw index does overflow
            indexOverflowCount++
            Timber.d("increase indexOverflowCount : indexOverflowCount = $indexOverflowCount")
        }
        val newFrameIndex = indexOverflowCount * indexCycle + rawIndex
//        Timber.v("newFrameIndex = $newFrameIndex")

        // 2st step : parse raw data to standard table and notify realtime if needed
        val eegSignals = decodeEEGData(timedBLEFrame.data)
        val statuses: List<Float> = eegSignals.map { it.statusData }
        val standardEEGs = dataConversion.convertRawDataToEEG(eegSignals)
        if (hasRealtimeListener()) {
            eegRealtimeSubject.onNext(
                EEGSignalPack(
                    timestamp = timedBLEFrame.timestamp,
                    index = newFrameIndex,
                    eegSignals = standardEEGs,
                    triggers = statuses
                )
            )
        }

        if (previousIndex == -1L) {
            //init first frame index
            previousIndex = newFrameIndex - 1
        }

        if (eegStreamingErrorCounter.startingIndex == -1L) {
            eegStreamingErrorCounter.startingIndex = newFrameIndex
        }
        eegStreamingErrorCounter.currentIndex = newFrameIndex

        val indexDifference = newFrameIndex - previousIndex
        val missingFrame = indexDifference - 1
        if (missingFrame > 0) {
            eegStreamingErrorCounter.increaseMissingEegFrame(missingFrame)
        }

        //this block is to count zero signals
        val eegData = eegFrame.copyOfRange(headerAlloc, eegFrame.size)
        with(ErrorDataHelper2.countZeroSample(eegData, getNumberOfChannels())) {
            if (this.first != 1) {
                eegStreamingErrorCounter.increaseZeroSampleCounter(this.first.toLong())
            }
            if (this.second != 1) {
                eegStreamingErrorCounter.increaseZeroTimeCounter(this.second.toLong())
            }
        }

        val missingEEGSamples = mutableListOf<RawEEGSample2>()

        // 3rd step : Fill gap by NaN samples if there is missing frames
        if (missingFrame > 0) {
            var missingCount = 0L
            val sampleNb = getNumberOfTimes(eegFrame)
            Timber.w("diff is $indexDifference. Current index : $newFrameIndex | previousIndex : $previousIndex")
            for (i in 1..missingFrame) {
                //one frame contains n times of sample
                for (j in 1..sampleNb) {
                    missingEEGSamples.add(RawEEGSample2.NAN_PACKET)
                    missingCount++
                }
            }
            if (BuildConfig.DEBUG) {
                assert(missingCount == missingFrame * sampleNb)
            }
//            Timber.i("missing size = n channels * sample per frame = ${rawEEGList.size}")
        }
        val missingStatuses: List<Float> = missingEEGSamples.map { it.statusData }
        val standardMissingEEGs = dataConversion.convertRawDataToEEG(missingEEGSamples)
        consolidatedEEGBuffer.addAll(standardMissingEEGs)
        consolidatedStatusBuffer.addAll(missingStatuses)

        // 4th step: save raw eeg data to buffer
        consolidatedEEGBuffer.addAll(standardEEGs)
        consolidatedStatusBuffer.addAll(statuses)

        previousIndex = newFrameIndex

        //5th step: if raw buffer is reach threshold, generate consolidated eeg
        val count = consolidatedEEGBuffer.size
        val sampleRate = getSampleRate()
        if (count >= sampleRate) {
            //consolidated buffer can emit a MBTPacket
            Timber.v("consolidatedEEGBuffer = [${consolidatedEEGBuffer.size}x${consolidatedEEGBuffer[0].size}]")
            Timber.v("consolidatedStatusBuffer = [${consolidatedStatusBuffer.size}]")
            val invertedEegData = ArrayList(consolidatedEEGBuffer.subList(0, sampleRate))
            val newStatusData = ArrayList(consolidatedStatusBuffer.subList(0, sampleRate))
            consolidatedEEGBuffer = ArrayList(consolidatedEEGBuffer.subList(sampleRate, count))
            consolidatedStatusBuffer =
                ArrayList(consolidatedStatusBuffer.subList(sampleRate, count))

            // invert table : from [nbSample * nbChannel]  to [nbChannel * nbSample]
            @Suppress("NAME_SHADOWING")
            val eegData = MatrixUtils2.invertFloatMatrix(invertedEegData)

            val newPacket = MbtEEGPacket(eegData, newStatusData)
            if (isQualityCheckerEnabled) {
                try {
//                    Timber.d("new packet : ${Arrays.toString(eegData.toArray())}")
                    val qualities = qualityChecker.computeQualityChecker(eegData)
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
//            Timber.d("new qualities : ${newPacket.qualities.toJson()}")

            if (isRecording) {
                recordingBuffer.add(newPacket)
                Timber.d("eeg recordingBuffer size = ${recordingBuffer.size}")
            }

            eegPacketSubject.onNext(newPacket)
        }
    }

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

    override fun getBufferSize(): Int {
        return recordingBuffer.size
    }

    override fun onTriggerStatusConfiguration(statusAllocationSize: Int) {
        statusAlloc = statusAllocationSize
        headerAlloc = indexAlloc + statusAlloc
        Timber.d("indexAlloc = $indexAlloc | statusAlloc = $statusAlloc | headerAlloc = $headerAlloc")
    }

    override fun getBuffer(): List<MbtEEGPacket> {
        return recordingBuffer
    }

    override fun getRecordingErrorData(): EEGStreamingErrorCounter {
        return eegStreamingErrorCounter
    }

    fun getDataLossPercent(): Float {
        return eegStreamingErrorCounter.getMissingPercent()
    }

    companion object {
        const val SIGNAL_ALLOC = 2 //one EEG signal is 2 bytes
    }
}