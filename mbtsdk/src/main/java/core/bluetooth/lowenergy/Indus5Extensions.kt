package core.bluetooth.lowenergy

import utils.LogUtils
import java.util.*

fun ByteArray.parseRawIndus5Response() : Indus5Response {
    try {
        //TODO: refactor to parse with enum class
        if (this[0].compareTo(0x29) == 0) {
            // only keep the 2nd byte where stores the number of sample
            return Indus5Response.MtuChangedResponse(this[1].toInt())
        }
        if (this[0].compareTo(0x40) == 0) {
            // remove command code
            val data = this.copyOfRange(1, this.size)
            return Indus5Response.EegFrameResponse(data)
        }
    } catch (e: Exception) {
        LogUtils.e(e)
        return Indus5Response.UnknownResponse(this)
    }
    return Indus5Response.UnknownResponse(this)
}