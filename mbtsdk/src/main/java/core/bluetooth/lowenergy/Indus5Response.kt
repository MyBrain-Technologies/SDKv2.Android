package core.bluetooth.lowenergy

import utils.LogUtils

sealed class Indus5Response() {
    class FirmwareVersion(val version: String) : Indus5Response()
    class MtuChange(val size: Int) : Indus5Response()
    class TriggerConfiguration(val triggerSize: Int) : Indus5Response()
    class EegFrame(val data: ByteArray) : Indus5Response()
    class EegStatus(val isEnabled: Boolean) : Indus5Response()
    class BatteryLevel(val percent: Float) : Indus5Response()
    class ImsFrame(val data: ByteArray) : Indus5Response()
    class ImsStatus(val isEnabled: Boolean) : Indus5Response()
    class PpgFrame(val data: ByteArray) : Indus5Response()
    class PpgStatus(val isEnabled: Boolean) : Indus5Response()
    class UnknownResponse(bytes: ByteArray) : Indus5Response()
}

//TODO: refactor to parse with enum class
fun ByteArray.parseRawIndus5Response() : Indus5Response {
    try {
        if (this[0].compareTo(0x27) == 0) {
            val data = this.copyOfRange(1, this.size)
            return Indus5Response.FirmwareVersion(String(data))
        }
        if (this[0].compareTo(0x29) == 0) {
            // only keep the 2nd byte where stores the number of sample
            return Indus5Response.MtuChange(this[1].toInt())
        }
        if (this[0].compareTo(0x0F) == 0) {
            return Indus5Response.TriggerConfiguration(this[1].toInt())
        }
        if (this[0].compareTo(0x40) == 0) {
            // remove command code
            val data = this.copyOfRange(1, this.size)
            return Indus5Response.EegFrame(data)
        }
        if (this[0].compareTo(0x24) == 0) {
            return Indus5Response.EegStatus(true)
        }
        if (this[0].compareTo(0x25) == 0) {
            return Indus5Response.EegStatus(false)
        }
        if (this[0].compareTo(0x20) == 0) {
            // 0x00 .. 0x04 = 0% | 0x05 = 12,5% -> 0x0C = 100%
            val percent = if (this[1] < 4) 0f else ((this[1] - 4) * 12.5f)
            return Indus5Response.BatteryLevel(percent)
        }
        if (this[0].compareTo(0x33) == 0) {
            return Indus5Response.ImsStatus(true)
        }
        if (this[0].compareTo(0x34) == 0) {
            return Indus5Response.ImsStatus(false)
        }
        if (this[0].compareTo(0x35) == 0) {
            return Indus5Response.PpgStatus(true)
        }
        if (this[0].compareTo(0x36) == 0) {
            return Indus5Response.PpgStatus(false)
        }
        if (this[0].compareTo(0x50) == 0) {
            val data = this.copyOfRange(1, this.size)
            return Indus5Response.ImsFrame(data)
        }
        if (this[0].compareTo(0x60) == 0) {
            // optimize performance : keep op code in bytes to reduce calculations
            return Indus5Response.PpgFrame(this)
        }
    } catch (e: Exception) {
        LogUtils.e(e)
        return Indus5Response.UnknownResponse(this)
    }
    return Indus5Response.UnknownResponse(this)
}