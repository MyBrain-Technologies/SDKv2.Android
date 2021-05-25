package core.bluetooth.lowenergy

import utils.LogUtils

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
        if (this[0].compareTo(0x24) == 0) {
            return Indus5Response.EegStartResponse()
        }
        if (this[0].compareTo(0x25) == 0) {
            return Indus5Response.EegStopResponse()
        }
        if (this[0].compareTo(0x25) == 0) {
            return Indus5Response.EegStopResponse()
        }
        if (this[0].compareTo(0x20) == 0) {
            // 0x00 .. 0x04 = 0% | 0x05 = 12,5% -> 0x0C = 100%
            val percent = (this[1] - 4) * 12.5f
            return Indus5Response.BatteryLevelResponse(percent)
        }
        if (this[0].compareTo(0x33) == 0) {
            return Indus5Response.AccelerometerCommand(true)
        }
        if (this[0].compareTo(0x34) == 0) {
            return Indus5Response.AccelerometerCommand(false)
        }
        if (this[0].compareTo(0x50) == 0) {
            val data = this.copyOfRange(1, this.size)
            return Indus5Response.AccelerometerFrame(data)
        }
    } catch (e: Exception) {
        LogUtils.e(e)
        return Indus5Response.UnknownResponse(this)
    }
    return Indus5Response.UnknownResponse(this)
}