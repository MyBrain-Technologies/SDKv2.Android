package core.bluetooth.lowenergy

sealed class Indus5Response() {
    class MtuChange(val size: Int) : Indus5Response()
    class TriggerConfiguration(val isEnabled: Boolean) : Indus5Response()
    class EegFrame(val data: ByteArray) : Indus5Response()
    class EegStatus(val isEnabled: Boolean) : Indus5Response()
    class BatteryLevel(val percent: Float) : Indus5Response()
    class ImsFrame(val data: ByteArray) : Indus5Response()
    class ImsStatus(val isEnabled: Boolean) : Indus5Response()
    class PpgFrame(val data: ByteArray) : Indus5Response()
    class PpgStatus(val isEnabled: Boolean) : Indus5Response()
    class UnknownResponse(bytes: ByteArray) : Indus5Response()
}