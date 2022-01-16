package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.android.jnibrainbox.QualityChecker
import com.mybraintech.sdk.core.listener.EEGListener
import com.mybraintech.sdk.core.model.MbtDataConversion2
import com.mybraintech.sdk.core.model.RawEEGSample2
import com.mybraintech.sdk.core.model.RecordingErrorData2
import com.mybraintech.sdk.util.ErrorDataHelper2
import com.mybraintech.sdk.util.MatrixUtils2
import com.mybraintech.sdk.util.NumericalUtils
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


// TODO: 09/11/2021 : implement
abstract class EEGSignalProcessing(
    private val sampleRate: Int,
    protocol: EnumBluetoothProtocol,
    protected val statusAlloc: Int,
    private val isQualityCheckerEnabled: Boolean,
//    private val eegPacketManager: EEGPacketManager = EEGPacketManager(),
//    private val acquisisitonSaver: EEGAcquisitionSaver = EEGAcquisitionSaver()
) {

    //==============================================================================
    // MARK: - Packets
    //==============================================================================


//  /// Save the EEGPackets recorded
//  ///
//  /// - Parameters:
//  ///   - idUser: A *Int* id of the connected user
//  ///   - comments: An array of *String* contains Optional Comments
//  ///   - completion: A block which execute after create the file or fail to
//  ///   create
//  fun saveRecording(userId: Int,
//                    algorithm: MBTRelaxIndexAlgorithm?,
//                    comments: Array<String> = arrayOf(),
//                    device: DeviceInformation,
//                    recordingInformation: MBTRecordInfo,
//                    recordFileSaver: RecordFileSaver,
//                    completion: @escaping (Result<URL, Error>) -> Void) {
//
////    let packets = eegPacketManager.eegPackets
////      let qualities = eegPacketManager.qualities
////      let channelData = eegPacketManager.eegData
////      acquisisitonSaver.saveRecording(packets: packets,
////                                      qualities: qualities,
////                                      channelData: channelData,
////                                      idUser: idUser,
////                                      algorithm: algorithm,
////                                      comments: comments,
////                                      deviceInformation: device,
////                                      recordingInformation: recordingInformation,
////                                      recordFileSaver: recordFileSaver) {
////        [weak self] result in
////        switch result {
////          case .success(let url):
////          self?.eegPacketManager.removeAllEegPackets()
////          completion(.success(url))
////
////          case .failure(let error):
////          if (error as? EEGAcquisitionSaverV2.EEGAcquisitionSaverError)
////          == .unableToWriteFile {
////          self?.eegPacketManager.removeAllEegPackets()
////        }
////          completion(.failure(error))
////        }
////      }
//  }

    //----------------------------------------------------------------------------
    // MARK: - Process Received data Methods.
    //----------------------------------------------------------------------------

    /// Process the brain activty measurement received and return the processed
    /// data.
    /// - Parameters:
    ///     - data: *Data* received from MBT Headset EEGs.
    /// - Returns: *Dictionnary* with the packet Index (key: "packetIndex") and
    /// array of P3 and P4 samples arrays ( key: "packet" )
//    fun generateEegPacket(eegData: ByteArray): MbtEEGPacket? {
//        val eegPacket =
//            acquisitionProcessor.getEEGPacket(eegData, hasQualityChecker)
//                ?: return null
//
//
//        if (isRecording) {
//            eegPacketManager.saveEEGPacket(eegPacket)
//        }
//
//        return eegPacket
//    }


    /**
     * index allocation size in the eeg frame
     */
    var indexAlloc = protocol.getFrameIndexAllocationSize()

    /**
     * frame = header + data
     *
     * header = frame index + trigger
     *
     * data = eeg signals : For QPlus : ch1-t0 | ch2-t0 | ... | ch4-t0 | ch1-t1 | ch2-t1 | ... | ch4-t1 | ...
     */
    val headerAlloc = indexAlloc + statusAlloc
    protected val hasStatus = (statusAlloc > 0)

    private var previousIndex = -1L

    private var isEEGEnabled = false

    var eegListener: EEGListener? = null
    var eegBuffer = mutableListOf<MbtEEGPacket2>()

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

    //use 3 CompositeDisposable to safely handle data and optimize memory by clearing one by one
    private var disposableCount = 0
    private val disposablePerContainer = 20
    private var container1 = CompositeDisposable()
    private var container2 = CompositeDisposable()
    private var container3 = CompositeDisposable()

    fun onEEGStatusChange(isEnabled: Boolean) {
        this.isEEGEnabled = isEnabled
        if (isEnabled) {
            //reset buffer when starting streaming
            restart()
        }
    }

    private fun restart() {
        recordingErrorData = RecordingErrorData2()
        rawBuffer = mutableListOf()
        eegBuffer = mutableListOf()
        if (isQualityCheckerEnabled) {
            qualityChecker = QualityChecker(sampleRate)
        }
        container1.clear()
        container2.clear()
        container3.clear()
    }

    fun onEEGFrame(eegFrame: ByteArray) {
        Timber.d("onEEGFrame : ${NumericalUtils.bytesToShortString(eegFrame)}")
        //switch work to not-main thread
        if (disposableCount >= 3 * disposablePerContainer) {
            disposableCount = 0
        }
        disposableCount++
        val disposable = clearContainerAndSelectDisposable(disposableCount)
        Observable.just(eegFrame)
            .subscribeOn(Schedulers.trampoline()) //executes tasks in a FIFO (First In, First Out) manner
            .map {
                try {
                    onEEGFrameComputation(it)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            .subscribe()
            .addTo(disposable)
    }

    /**
     * disposable 1 will be used first then 2 then 3 then 1 again...
     */
    private fun clearContainerAndSelectDisposable(disposableCount: Int): CompositeDisposable {
        when (disposableCount / disposablePerContainer) {
            1 -> {
                container3.clear()
                return container2
            }
            2 -> {
                container1.clear()
                return container3
            }
            else -> {
                container2.clear()
                return container1
            }
        }
    }

    val lock = Unit

    /**
     * please wrap this method in try catch to avoid crashing
     */
    @Throws(Exception::class)
    private fun onEEGFrameComputation(eegFrame: ByteArray) {
//        synchronized(lock) {
        Timber.d("onEEGFrameComputation")
        if (!isValidFrame(eegFrame)) {
            Timber.e("bad format eeg frame : ${NumericalUtils.bytesToShortString(eegFrame)}")
            return
        }

        //1st step : check index
        val newFrameIndex = getFrameIndex(eegFrame)
        Timber.d("newFrameIndex = $newFrameIndex")

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
            Timber.v("diff is $indexDifference. Current index : $newFrameIndex | previousIndex : $previousIndex")
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
                val newEegData = ArrayList(consolidatedEEGBuffer.subList(0, sampleRate))
                val newStatusData = ArrayList(consolidatedStatusBuffer.subList(0, sampleRate))
                consolidatedEEGBuffer = ArrayList(consolidatedEEGBuffer.subList(sampleRate, count))
                consolidatedStatusBuffer =
                    ArrayList(consolidatedStatusBuffer.subList(sampleRate, count))
                val newPacket = MbtEEGPacket2(newEegData, newStatusData)
                if (isQualityCheckerEnabled) {
                    try {
                        val qualities = qualityChecker?.computeQualityChecker(
                            MatrixUtils2.invertFloatMatrix(newEegData)
                        )
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

                eegListener?.onEegPacket(newPacket)
            }
        }
//        } //end lock
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

    companion object {
        const val SIGNAL_ALLOC = 2 //one EEG signal is 2 bytes

        /**
         * features.MbtFeatures.DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE
         */
        const val DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE = 40
    }
}