package model

import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger

internal class LedSignalTest {

    @Test
    fun testConstructor() {
        val led = LedSignal(byteArrayOf(0x01, 0x02, 0x03))
        val value = BigInteger(led.bytes)
        assertEquals(66051L, value.toLong())
    }
}