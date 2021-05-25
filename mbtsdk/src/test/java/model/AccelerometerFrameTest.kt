package model

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class AccelerometerFrameTest {

    private val dataFrame1 = byteArrayOf(0x00, 0x03, 0xFE.toByte(), 0xFF.toByte(), 0x01, 0x00, 0x3F, 0x00)
    private val x = -2 * AccelerometerFrame.FACTOR
    private val y = 1 * AccelerometerFrame.FACTOR
    private val z = 63 * AccelerometerFrame.FACTOR

    private val frames = listOf<AccelerometerFrame>(
            AccelerometerFrame(3, listOf(Position3D(x, y, z))),
            AccelerometerFrame(3, listOf(Position3D(x, y, 1.0f))),
            AccelerometerFrame(3, listOf(Position3D(x, 1.0f, z))),
            AccelerometerFrame(3, listOf(Position3D(1.0f, y, z))),
            AccelerometerFrame(4, listOf(Position3D(x, y, z)))
    )

    private
    val expectedResults = listOf<Boolean>(true, false, false, false, false)

    @Test
    fun testConversion() {
        val result = AccelerometerFrame(dataFrame1)

        for (i in frames.indices) {
            assertEquals(expectedResults[i], result == frames[i])
            assertEquals(expectedResults[i], result.hashCode() == frames[i].hashCode())
        }
    }
}