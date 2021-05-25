package model

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class AccelerometerFrameTest {

    private val dataFrame1 = byteArrayOf(0x00, 0x03,
            0xFE.toByte(), 0xFF.toByte(), 0x01, 0x00, 0x3F, 0x00,
            0x01.toByte(), 0x00.toByte(), 0x02, 0x00, 0x03, 0x00)
    private val x0 = -2 * AccelerometerFrame.FACTOR
    private val y0 = 1 * AccelerometerFrame.FACTOR
    private val z0 = 63 * AccelerometerFrame.FACTOR
    private val x1 = 1 * AccelerometerFrame.FACTOR
    private val y1 = 2 * AccelerometerFrame.FACTOR
    private val z1 = 3 * AccelerometerFrame.FACTOR

    private val frames = listOf<AccelerometerFrame>(
            AccelerometerFrame(3, arrayListOf(Position3D(x0, y0, z0), Position3D(x1, y1, z1))),
            AccelerometerFrame(3, arrayListOf(Position3D(x0, y0, 1.0f))),
            AccelerometerFrame(3, arrayListOf(Position3D(x0, 1.0f, z0))),
            AccelerometerFrame(3, arrayListOf(Position3D(1.0f, y0, z0))),
            AccelerometerFrame(4, arrayListOf(Position3D(x0, y0, z0)))
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