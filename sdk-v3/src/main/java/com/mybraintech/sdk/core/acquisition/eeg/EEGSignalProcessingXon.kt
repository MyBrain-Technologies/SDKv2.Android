package com.mybraintech.sdk.core.acquisition.eeg

import com.mybraintech.sdk.core.acquisition.EnumBluetoothProtocol
import com.mybraintech.sdk.core.decodeFromHex
import com.mybraintech.sdk.core.encodeToHex
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.RawEEGSample2
import com.mybraintech.sdk.core.model.StreamingParams
import com.mybraintech.sdk.core.model.XonStatus
import io.reactivex.Scheduler
import timber.log.Timber

class EEGSignalProcessingXon(
    streamingParams : StreamingParams,
    bleCallback: EEGCallback?,
    bleEEGFrameScheduler: Scheduler
) : EEGSignalProcessing(
    protocol = EnumBluetoothProtocol.BLE,
    isTriggerStatusEnabled = streamingParams.isTriggerStatusEnabled,
    isQualityCheckerEnabled = streamingParams.isQualityCheckerEnabled,
    callback = bleCallback,
    eegFrameScheduler = bleEEGFrameScheduler
) {
    override fun getDeviceType() = EnumMBTDevice.XON

    override fun getFrameIndex(eegFrame: ByteArray): Long {
        //        TODO("Not yet implemented")
        return -1
    }

    override fun isValidFrame(eegFrame: ByteArray): Boolean {
//        TODO("Not yet implemented")
        return true
    }

    override fun getNumberOfChannels(): Int {
        return 8
    }

    override fun decodeEEGData(eegFrame: ByteArray): List<RawEEGSample2> {
        val frameData = eegFrame
        //F3", "F4", "C3", "Cz", "C4", "P3", "P4", “CH8
        var resultList = mutableListOf<RawEEGSample2>()
        val bleVoltage: Float = (1.0e-6 / 8).toFloat()
        val subIdx = 2 //token index and size index.
        try {


            var packetTokenIdx = 0
            var packetLengthIdx = 1
            do {

                val packetTokenHex = "%02X".format(frameData[packetTokenIdx])
                val lengthOfData = frameData[packetLengthIdx].toInt()

                val data = frameData.copyOfRange(
                    packetLengthIdx + 1,
                    packetLengthIdx + 1 + lengthOfData
                )
                //process data
                //the oder F3", "F4", "C3", "Cz", "C4", "P3", "P4", “CH8”
                //check the token packet the accept token packet is:
                //0x90,0x91,0x92,0x93,0xA)
                // full package data = tokenPacket+lengthOfData+data
                val dataSize = data.size
                var startEegIndex = 0
                var endEegIndex = 0
                var hasStatus = false
                when (packetTokenHex) {
                    "92", "93" -> {
                        startEegIndex = 6-subIdx
                        endEegIndex = 29 + 1-subIdx
                        hasStatus = false
                    }
                    "B0" ->{
                        startEegIndex = 0
                        endEegIndex = dataSize
                        hasStatus = true
                    }

                    else -> {
                        hasStatus = false
                        startEegIndex = 6-subIdx
                        endEegIndex = 21 + 1-subIdx
                    }
                }
                if (dataSize >= endEegIndex) {

                    val deserializedData = data.copyOfRange(startEegIndex, endEegIndex)

                    if (hasStatus) {
                        val statusData = XonStatus()

                        val adcTimeStamp = bytesToUInt(deserializedData.copyOfRange(2-subIdx, 5+1-subIdx))
                        val radioTimeStamp = bytesToUInt(deserializedData.copyOfRange(6-subIdx, 9+1-subIdx))

                        val sampleCounter = bytesToUInt(deserializedData.copyOfRange(10-subIdx, 11+1-subIdx))
                        val rssi = bytesToUInt(deserializedData.copyOfRange(12-subIdx, 12+1-subIdx))
                        val batteryVoltage = bytesToUInt(deserializedData.copyOfRange(13-subIdx, 14+1-subIdx))
                        val batteryLevel = bytesToUInt(deserializedData.copyOfRange(15-subIdx, 15+1-subIdx))
                        statusData.packetTimeStamp = adcTimeStamp
                        statusData.syncTimeStamp = radioTimeStamp
                        statusData.packetSampleCounter = sampleCounter
                        statusData.rssi = rssi.toInt()
                        statusData.batteryVoltage = batteryVoltage.toUShort()
                        statusData.batteryLevel = batteryLevel.toUShort()
                    } else {
                        val chanels = convert24ByteArrayTo8ArraysOf3Bytes(deserializedData)
                        val rawEEGSample2 = RawEEGSample2(chanels,1.0f)
                        resultList.add(rawEEGSample2)
                    }
                }

                packetTokenIdx = packetLengthIdx + 1 + lengthOfData
                packetLengthIdx = packetTokenIdx + 1

            } while (packetTokenIdx < frameData.size)
        } catch (ex: Exception) {
            ex.printStackTrace()
            Timber.e("[TNDEBUG] has exception = ${ex.message}")
        }
        Timber.d("[TNDEBUG] has resultList = ${resultList}")
        return resultList
    }

    private fun bytesToUInt(byteArray: ByteArray): UInt {
        var combinedValueN = byteArray[0].toInt() and 0xFF

        for (idx in 1 until byteArray.size) {
            val currentByte = byteArray[idx].toInt() and 0xFF
            combinedValueN = combinedValueN or (currentByte shl (idx * 8))

        }

        return combinedValueN.toUInt()
    }

    private fun convert24ByteArrayTo8ArraysOf3Bytes(byteArray24: ByteArray): List<ByteArray> {
        require(byteArray24.size == 24) { "Input byte array must be 24 bytes long" }

        val result = mutableListOf<ByteArray>()

        for (i in 0 until 8) {
            val start = i * 3
            val end = start + 3
            val subArray = byteArray24.copyOfRange(start, end)
            result.add(subArray)
        }

        return result
    }
}