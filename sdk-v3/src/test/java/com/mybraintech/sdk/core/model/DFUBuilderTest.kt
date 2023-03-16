package com.mybraintech.sdk.core.model

import com.mybraintech.sdk.UnitTestFileReader
import com.mybraintech.sdk.gwen
import com.mybraintech.sdk.then
import com.mybraintech.sdk.whenever
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*
import java.util.zip.CRC32

/**
 * unit test for [DFUBuilder]
 */
@RunWith(RobolectricTestRunner::class)
class DFUBuilderTest {

    /**
     * pair : first = value, second = shift length
     * eg : 76000 in decimal = 0x0000001 00101000 11100000 in binary
     */
    private val inputPairsToShiftBit = listOf<Pair<Int, Int>>(
        Pair(76000, 0),
        Pair(76000, 8),
        Pair(76000, 16),
        Pair(76000, 24),
    )

    /**
     * expected of [inputPairsToShiftBit]
     */
    private val expectedByteAfterShifting = listOf<Byte>(
        224.toByte(),
        40,
        1,
        0
    )

    @Test
    fun test_rightShiftAndMask() {
        val builder = DFUBuilder()

        for (i in inputPairsToShiftBit.indices) {
            assertEquals(
                expectedByteAfterShifting[i],
                builder.rightShiftAndMask(
                    inputPairsToShiftBit[i].first,
                    inputPairsToShiftBit[i].second
                )
            )
        }
    }

    @Test
    fun test_integrateFileLength_fromBytes() = gwen {
        val builder = DFUBuilder()
        val inputBinaryBytes = ByteArray(120)
        val expectedOutputSize = 256

        whenever {
            builder.integrateFileLength(inputBinaryBytes)
        }

        then {
            assertEquals(expectedOutputSize, builder.formulatedFirmwareData.size)
        }
    }

    @Test
    fun test_integrateFile_fromFirmwareFile() = gwen {
        val originalBytes =
            UnitTestFileReader().readFile("mm_ota_melomind_q_plus_release_ads1299_0_9_0.bin")
//        val originalBytes = UnitTestFileReader().readFile("mm_ota_i3_1_7_14.bin")
//        val originalBytes = UnitTestFileReader().readFile("mm_ota_1_7_4.bin")
        val builder = DFUBuilder()

        whenever {
            builder.integrateFileLength(originalBytes)
//            val expected = (originalBytes[0].toUByte().toLong()
//                    + originalBytes[1].toUByte().toLong() * 256
//                    + originalBytes[4].toUByte().toLong() * 256 * 256
//                    + originalBytes[5].toUByte().toLong() * 256 * 256 * 256)
//            println("(1) = $expected")
//            var found = false
//            for (i in 0..100) {
//                val content = originalBytes.copyOfRange(i, originalBytes.size)
//                val crc32 = calculateCRC32(content)
//                println("(2) = $crc32")
//                if (expected == crc32) {
//                    println("i should be $i")
//                    found = true
//                    break
//                }
//            }
//            assertEquals(true, found)

            val newContent = builder!!.formulatedFirmwareData
        }

        then {
            val bytes = OADExtractionUtils.extractFirmwareVersionFromContent(originalBytes)
            assertEquals("0.10.0", bytes.getString())

            // assertEquals("0.10.0", builder.firmwareVersion.getString())
        }
    }

    fun calculateCRC32(value: ByteArray): Long {
        val crc32Calculator = CRC32()
        crc32Calculator.update(value)
        return crc32Calculator.value
    }

    fun ByteArray.getString(): String {
        return String(this)
    }
}