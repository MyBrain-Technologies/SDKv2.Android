package com.mybraintech.sdk.core.acquisition.ims

import com.mybraintech.sdk.BuildConfig
import com.mybraintech.sdk.core.acquisition.IndexReader
import com.mybraintech.sdk.core.model.*
import com.mybraintech.sdk.core.recording.BaseAccelerometerRecorder
import com.mybraintech.sdk.util.NumericalUtils
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import kotlin.math.pow

class AccelerometerSignalProcessingIndus5(
    var accelerometerCallback: AccelerometerCallback? = null,
    @Suppress("CanBeParameter") private val imsFrameScheduler: Scheduler
) : BaseAccelerometerRecorder() {

    private var isRecording: Boolean = false

    private val accelerometerPacketSubject = PublishSubject.create<AccelerometerPacket>()
    private val imsFrameSubject = PublishSubject.create<ByteArray>()

    /**
     * index allocation size in the ims frame
     */
    private val indexAlloc = 2
    private val indexCycle = 2.0.pow(indexAlloc * 8).toLong()
    private var previousIndex = -1L

    /**
     * number of time the ble frame index does overflow
     */
    private var indexOverflowCount = 0L

    private var rawBuffer: MutableList<ThreeDimensionalPosition> = mutableListOf()
    private var recordingBuffer = mutableListOf<ThreeDimensionalPosition>()

    init {
        accelerometerPacketSubject
            .observeOn(Schedulers.io())
            .subscribe(
                {
                    accelerometerCallback?.onAccelerometerPacket(it)
                },
                Timber::e
            )
            .addTo(disposable)

        imsFrameSubject
            .observeOn(imsFrameScheduler)
            .subscribe(
                ::consumeIMSFrame,
                Timber::e
            )
            .addTo(disposable)
    }

    //----------------------------------------------------------------------------
    // MARK: interface IMSSignalProcessing implementation
    //----------------------------------------------------------------------------
    override fun onAccelerometerConfiguration(accelerometerConfig: AccelerometerConfig) {
        setSampleRate(accelerometerConfig.sampleRate)
    }

    override fun startRecording() {
        isRecording = true
        rawBuffer = mutableListOf()
        recordingBuffer = mutableListOf()
    }

    override fun addSignalData(data: ByteArray) {
//        Timber.v("onIMSFrame : ${NumericalUtils.bytesToShortString(data)}")
        imsFrameSubject.onNext(data)
    }

    override fun getBuffer(): List<ThreeDimensionalPosition> {
        return recordingBuffer
    }

    override fun stopRecording() {
        Timber.v("stopRecording")
        isRecording = false
    }

    override fun getBufferSize(): Int {
        return (recordingBuffer.size / getSampleRate())
    }

    override fun isRecording(): Boolean {
        return isRecording
    }

    override fun clearBuffer() {
        rawBuffer = mutableListOf()
        recordingBuffer = mutableListOf()
    }

    /**
     * please wrap this method in try catch to avoid crashing
     */
    @Throws(Exception::class)
    private fun consumeIMSFrame(data: ByteArray) {
//        Timber.v("consumeIMSFrame")
        if (!isValidFrame(data)) {
            Timber.e("bad format ims frame : ${NumericalUtils.bytesToShortString(data)}")
//            Log.e("consumeIMSFrame", "bad format ims frame : ${NumericalUtils.bytesToShortString(data)}")
            return
        }

        //1st step : check index
        val rawIndex = getFrameIndex(data)
        val indexCandidate = indexOverflowCount * indexCycle + rawIndex
        if (indexCandidate < previousIndex) {
            //index in ble frame is from 0 to (2^16 - 1), this bracket is entered when the raw index does overflow
            indexOverflowCount++
            Timber.d("increase indexOverflowCount : indexOverflowCount = $indexOverflowCount")
        }
        val newFrameIndex = indexOverflowCount * indexCycle + rawIndex

        val imsBleFrame = AccelerometerFrame(data).apply {
            packetIndex = newFrameIndex
        }
//        Timber.v("newFrameIndex = $newFrameIndex")

        if (previousIndex == -1L) {
            //init first frame index
            previousIndex = newFrameIndex - 1
        }

        val indexDifference = newFrameIndex - previousIndex
        val missingFrame = indexDifference - 1
        if (missingFrame != 0L) {
            Timber.w("ims diff is $indexDifference. Current index : $newFrameIndex | previousIndex : $previousIndex")
//            Log.w("consumeIMSFrame", "ims diff is $indexDifference. Current index : $newFrameIndex | previousIndex : $previousIndex")
        }

        previousIndex = newFrameIndex

        val positions = mutableListOf<ThreeDimensionalPosition>()

        //2nd step : Fill gap by NaN if there is missing frames
        var missingCount = 0L
        if (indexDifference != 1L) {
            val missingPositionNumber = imsBleFrame.positions.size * missingFrame
            for (i in 1..missingPositionNumber) {
                positions.add(ThreeDimensionalPosition(Float.NaN, Float.NaN, Float.NaN))
                missingCount++
            }
        }
        if (BuildConfig.DEBUG) {
            assert(missingCount == missingFrame * imsBleFrame.positions.size)
        }

        //3rd step: add the new frame data
        positions.addAll(imsBleFrame.positions)

        //4th step: save data to buffer
        rawBuffer.addAll(positions)

        //5th step: if buffer reach 1 second, construct imsPacket and notify
        val count = rawBuffer.size
        val sampleRate = getSampleRate()
        if (count >= sampleRate) {
//            Timber.d("construct an IMS packet")
            val oneSecond = ArrayList(rawBuffer.subList(0, sampleRate))
            val accelerometerPacket = AccelerometerPacket(oneSecond)
            rawBuffer = ArrayList(rawBuffer.subList(sampleRate, count))

            if (isRecording) {
                recordingBuffer.addAll(oneSecond)
                Timber.d("ims recordingBuffer size = ${recordingBuffer.size}")
            }

            accelerometerCallback?.onAccelerometerPacket(accelerometerPacket)
        }
    }

    private fun getFrameIndex(imsFrame: ByteArray): Long {
        return IndexReader.decodeIndex(imsFrame)
    }

    private fun isValidFrame(imsFrame: ByteArray): Boolean {
        val size = imsFrame.size
//        Timber.v("imsFrame.size = $size")
        if (size <= AccelerometerFrame.INDEX_ALLOCATION) {
            return false
        }
        val dataLength = size - AccelerometerFrame.INDEX_ALLOCATION
        return (dataLength % AccelerometerFrame.SAMPLE_ALLOCATION == 0)
    }

    interface AccelerometerCallback {
        fun onAccelerometerPacket(packet: AccelerometerPacket)
    }
}