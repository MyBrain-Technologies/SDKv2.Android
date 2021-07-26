package core.bluetooth.lowenergy

import org.junit.Assert
import org.junit.jupiter.api.Test

internal class Indus5ExtensionsKtTest {

    val unknownResponse = byteArrayOf(0x99.toByte())
    val mtuResponse = byteArrayOf(0x29, 0x0A)
    val eegResponse = byteArrayOf(
            0x40, 0x16, 0xC2.toByte(),
            0x0F, 0xC2.toByte(), 0x0B, 0xAF.toByte(), 0x0C, 0xD3.toByte(), 0x08, 0xF4.toByte(), 0xFD.toByte(),
            0x71, 0xF8.toByte(), 0x65, 0xF9.toByte(), 0xD2.toByte(), 0xF8.toByte(), 0x91.toByte(), 0xFA.toByte(),
            0xEF.toByte(), 0xF4.toByte(), 0xCB.toByte(), 0x0A, 0x1A, 0x07, 0x5C, 0x09, 0x98.toByte(),
            0x06, 0x90.toByte(), 0xFD.toByte(), 0x14, 0xF7.toByte(), 0x8B.toByte(), 0xFB.toByte(), 0xD3.toByte(),
            0xF9.toByte(), 0x25, 0xFD.toByte(), 0x7A, 0xF5.toByte(), 0xAE.toByte()
    )

    @Test
    fun parseRawIndus5Response() {
        val mtu = mtuResponse.parseRawIndus5Response()
        Assert.assertEquals(mtu::class.java, Indus5Response.MtuChange::class.java)
        Assert.assertEquals((mtu as Indus5Response.MtuChange).size, 0x0A.toInt())

        val eeg = eegResponse.parseRawIndus5Response()
        Assert.assertEquals(eeg::class.java, Indus5Response.EegFrame::class.java)
        Assert.assertEquals((eeg as Indus5Response.EegFrame).data.size, eegResponse.size-1)
        println("parsed eeg content = ${(eeg as Indus5Response.EegFrame).data.contentToString()}")

        val unknown = unknownResponse.parseRawIndus5Response()
        Assert.assertEquals(unknown::class.java, Indus5Response.UnknownResponse::class.java)
    }
}