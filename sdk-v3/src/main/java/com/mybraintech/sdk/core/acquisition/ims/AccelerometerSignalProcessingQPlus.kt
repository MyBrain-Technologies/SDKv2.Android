package com.mybraintech.sdk.core.acquisition.ims

import com.mybraintech.sdk.core.acquisition.AcquisierThreadFactory
import com.mybraintech.sdk.core.acquisition.EnumBluetoothProtocol
import com.mybraintech.sdk.core.acquisition.IndexReader
import com.mybraintech.sdk.core.model.AccelerometerFrame
import com.mybraintech.sdk.core.model.AccelerometerPacket
import com.mybraintech.sdk.core.model.ThreeDimensionalPosition
import com.mybraintech.sdk.util.NumericalUtils
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class AccelerometerSignalProcessingQPlus(
    val sampleRate: Int,
    protocol: EnumBluetoothProtocol,
    var accelerometerCallback: AccelerometerCallback? = null
) : AccelerometerSignalProcessing {

    private var isRecording: Boolean = false

    /**
     * this scheduler is reserved to handle ims frame tasks
     */
    private var imsFrameScheduler = RxJavaPlugins.createSingleScheduler(AcquisierThreadFactory)

    private val accelerometerPacketSubject = PublishSubject.create<AccelerometerPacket>()
    private val imsFrameSubject = PublishSubject.create<ByteArray>()

    private var previousIndex = -1L

    private var rawBuffer: MutableList<ThreeDimensionalPosition> = mutableListOf()
    private var recordingBuffer = mutableListOf<ThreeDimensionalPosition>()

    private var disposable = CompositeDisposable()

    init {
        accelerometerPacketSubject
            .observeOn(Schedulers.io())
            .subscribe {
                accelerometerCallback?.onAccelerometerPacket(it)
            }
            .addTo(disposable)

        imsFrameSubject
            .observeOn(imsFrameScheduler)
            .subscribe {
                try {
                    consumeIMSFrame(it)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            .addTo(disposable)
    }

    //----------------------------------------------------------------------------
    // MARK: interface IMSSignalProcessing implementation
    //----------------------------------------------------------------------------
    override fun startRecording() {
        isRecording = true
        rawBuffer = mutableListOf()
        recordingBuffer = mutableListOf()
    }

    override fun onFrame(data: ByteArray) {
//        Timber.v("onIMSFrame : ${NumericalUtils.bytesToShortString(data)}")
        imsFrameSubject.onNext(data)
    }

    override fun getBuffer(): List<ThreeDimensionalPosition> {
        return recordingBuffer
    }

    override fun stopRecording() {
        isRecording = false
    }

    override fun getBufferSize(): Int {
        return (recordingBuffer.size / sampleRate)
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
        val newFrameIndex = getFrameIndex(data)
        val imsBleFrame = AccelerometerFrame(data).apply {
            packetIndex = newFrameIndex
        }
//        Timber.v("newFrameIndex = $newFrameIndex")

        if (previousIndex == -1L) {
            //init first frame index
            previousIndex = newFrameIndex - 1
        }

        val indexDifference = newFrameIndex - previousIndex
        if (indexDifference != 1L) {
            Timber.w("ims diff is $indexDifference. Current index : $newFrameIndex | previousIndex : $previousIndex")
//            Log.w("consumeIMSFrame", "ims diff is $indexDifference. Current index : $newFrameIndex | previousIndex : $previousIndex")
        }

        previousIndex = newFrameIndex

        val positions = mutableListOf<ThreeDimensionalPosition>()

        //2nd step : Fill gap by 0.0 voltages if there is missing frames
        if (indexDifference != 1L) {
            val missingPositionNumber = imsBleFrame.positions.size * indexDifference
            for (i in 1..missingPositionNumber) {
                positions.add(ThreeDimensionalPosition(Float.NaN, Float.NaN, Float.NaN))
            }
        }

        //3rd step: add the new frame data
        positions.addAll(imsBleFrame.positions)

        //4th step: save data to buffer
        rawBuffer.addAll(positions)

        //5th step: if buffer reach 1 second, construct imsPacket and notify
        val count = rawBuffer.size
        if (count >= sampleRate) {
//            Timber.d("construct an IMS packet")
            val oneSecond = ArrayList(rawBuffer.subList(0, sampleRate))
            val accelerometerPacket = AccelerometerPacket(oneSecond)
            rawBuffer = ArrayList(rawBuffer.subList(sampleRate, count))

            if (isRecording) {
                recordingBuffer.addAll(oneSecond)
//                Log.w("recordingBuffer", "size = ${recordingBuffer.size}")
            }

            accelerometerCallback?.onAccelerometerPacket(accelerometerPacket)
        }
    }

    private fun getFrameIndex(imsFrame: ByteArray): Long {
        return IndexReader.decodeIndex(imsFrame)
    }

    private fun isValidFrame(imsFrame: ByteArray): Boolean {
        val size = imsFrame.size
        Timber.v("imsFrame.size = $size")
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