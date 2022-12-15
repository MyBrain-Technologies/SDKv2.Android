package com.mybraintech.sdk.core.model

import androidx.annotation.VisibleForTesting
import org.jetbrains.annotations.TestOnly
import timber.log.Timber

/**
 * represents an IMS bluetooth frame. Each IMS frame contains a number of IMS samples(ThreeDimensionalPosition).
 * Eg: with MTU = 47, a QPlus ims frame can contain 5 ims samples.
 * @see ThreeDimensionalPosition
 */
class AccelerometerFrame private constructor() {

    companion object {
        private const val FACTOR = 15.6f
        const val SAMPLE_ALLOCATION = 6
        const val INDEX_ALLOCATION = 2
    }

    var packetIndex: Long = -1
    lateinit var positions: ArrayList<ThreeDimensionalPosition>

//    @ExperimentalUnsignedTypes
    constructor(data: ByteArray) : this() {
        // data does not contain OP code 0x50
        // size = 2 (package index) + 6x positions
        if ((data.size - INDEX_ALLOCATION) % SAMPLE_ALLOCATION != 0) {
            packetIndex = -1
            positions = arrayListOf()
            Timber.e("error: frame size does not match")
        } else {
            packetIndex = data[0].toLong() * 256 + data[1].toUByte().toInt()
            val size = (data.size - 2) / SAMPLE_ALLOCATION
            val tmp = arrayListOf<ThreeDimensionalPosition>()
            for (i in 0 until size) {
                tmp.add(ThreeDimensionalPosition(
                        x = decodeBytesToValue(data[2 + i * SAMPLE_ALLOCATION], data[3 + i * SAMPLE_ALLOCATION]),
                        y = decodeBytesToValue(data[4 + i * SAMPLE_ALLOCATION], data[5 + i * SAMPLE_ALLOCATION]),
                        z = decodeBytesToValue(data[6 + i * SAMPLE_ALLOCATION], data[7 + i * SAMPLE_ALLOCATION])))
            }
            positions = tmp
        }
    }

    @Suppress("unused")
    @TestOnly
    @VisibleForTesting
    internal constructor(packetIndex: Long, positions: ArrayList<ThreeDimensionalPosition>) : this() {
        this.packetIndex = packetIndex
        this.positions = positions
    }

    private fun decodeBytesToValue(byte1: Byte, byte2: Byte): Float {
        val sign =
                if (byte2.compareTo(0xFF) == 0) {
                    -1
                } else {
                    1
                }
        return sign * byte1 * FACTOR
    }

    override fun equals(other: Any?): Boolean {
        if (other is AccelerometerFrame) {
            if (other.packetIndex.compareTo(this.packetIndex) != 0) {
                return false
            }
            if (other.positions.size != this.positions.size) {
                return false
            }
            for (i in other.positions.indices) {
                if (other.positions[i].x.compareTo(this.positions[i].x) != 0) {
                    return false
                }
                if (other.positions[i].y.compareTo(this.positions[i].y) != 0) {
                    return false
                }
                if (other.positions[i].z.compareTo(this.positions[i].z) != 0) {
                    return false
                }
            }
            return true
        } else {
            return false
        }
    }

    override fun hashCode(): Int {
        var result = packetIndex.hashCode()
        result = 31 * result + positions.hashCode()
        return result
    }

    override fun toString(): String {
        return "AccelerometerFrame(packetIndex=$packetIndex, positions=$positions)"
    }
}