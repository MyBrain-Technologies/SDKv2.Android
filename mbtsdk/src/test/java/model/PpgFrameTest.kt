package model

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class PpgFrameTest {

    private val ppgFrames: List<ByteArray> = arrayListOf(
            byteArrayOf(
                    0x60,
                    0x00, 0x01,
                    0x01, 0x10, 0x11,
                    0x02, 0x20, 0x21,
                    0x01, 0x18, 0x19,
                    0x02, 0x28, 0x29,
            )
    )

    private val expected: List<ArrayList<LedSignal>> = arrayListOf(
            arrayListOf(
                    LedSignal(byteArrayOf(0x01, 0x10, 0x11)),
                    LedSignal(byteArrayOf(0x01, 0x18, 0x19))
            ),
            arrayListOf(
                    LedSignal(byteArrayOf(0x02, 0x20, 0x21)),
                    LedSignal(byteArrayOf(0x02, 0x28, 0x29))
            )
    )

    @Test
    fun testConstructor() {
        for (i in ppgFrames.indices) {
            val frame = PpgFrame(ppgFrames[i])
            assertEquals(1, frame.packetIndex)
            assertEquals(expected.size, frame.leds.size)
            assertEquals(expected[0][0], frame.leds[0][0])
            assertEquals(expected[0][1], frame.leds[0][1])
            assertEquals(expected[1][0], frame.leds[1][0])
            assertEquals(expected[1][1], frame.leds[1][1])
        }
    }
}