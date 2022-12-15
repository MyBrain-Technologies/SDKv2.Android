package com.mybraintech.sdk.core.bluetooth.devices.qplus

import org.junit.Assert
import org.junit.Test

class QPlusMailboxHelperTest {

    private val SERIAL_NUMBER_CHANGED_RESPONSE_SUFFIX = byteArrayOf(0x1A, 0x71, 0x70, 0x5F)

    private val expectedSerialNumbers = listOf<String>(
        "0123456789"
    )

    private val responses = listOf<ByteArray>(
        SERIAL_NUMBER_CHANGED_RESPONSE_SUFFIX + expectedSerialNumbers[0].toByteArray()
    )

    @Test
    fun test_parseRawIndus5Response() {
        for (i in responses.indices) {
            val response = QPlusMailboxHelper.parseRawIndus5Response(responses[i])
            Assert.assertEquals(
                Indus5Response.SerialNumberChanged::class.java,
                response::class.java
            )
            Assert.assertEquals(
                expectedSerialNumbers[i],
                (response as Indus5Response.SerialNumberChanged).newSerialNumber
            )
        }
    }
}