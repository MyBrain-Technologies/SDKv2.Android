package model

import timber.log.Timber

class PpgFrame private constructor() {

    companion object {
        const val OP_CODE: Byte = 0x60
        private const val LED_LENGTH: Int = 3
    }

    var packetIndex: Long = -1
    var leds = ArrayList<ArrayList<LedSignal>>()

    /**
     * 1 byte OP + 2 byte packet index + 12 bytes led data
     * @param bytes contains op code and led data
     */
    @ExperimentalUnsignedTypes
    constructor(bytes: ByteArray) : this() {
        if (bytes[0] != OP_CODE || bytes.size != 15) {
            Timber.e("PPG frame format invalid : OP byte = ${bytes[0]} | size = ${bytes.size}")
            return
        }
        packetIndex = bytes[1].toLong() * 256 + bytes[2].toUByte().toInt()
        val header = 3
        val dataSize = bytes.size - header
        val sampleSize = LED_LENGTH * getLedNumber()

        //init empty list
        for (i in 1..getLedNumber()) {
            leds.add(ArrayList())
        }

        val times = dataSize / sampleSize
        for (t in 0 until times) {
            val sampleStartPos = header + t * sampleSize
            for (i in 0 until getLedNumber()) {
                val offset = i * LED_LENGTH
                leds[i].add(LedSignal(bytes.copyOfRange(sampleStartPos + offset, sampleStartPos + offset + LED_LENGTH)))
            }
        }
    }

    /**
     * next version: configure 1 or 2 led mode
     */
    private fun getLedNumber(): Int {
        return 2 //default value is 2 ?
    }
}