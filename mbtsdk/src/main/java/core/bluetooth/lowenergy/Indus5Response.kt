package core.bluetooth.lowenergy

sealed class Indus5Response() {
    class MtuChangedResponse(val size: Int) : Indus5Response()
    class EegFrameResponse(val data: ByteArray) : Indus5Response()
    class EegStartResponse() : Indus5Response()
    class BatteryLevelResponse(val percent: Float) : Indus5Response()
    class AccelerometerFrame(val data: ByteArray) : Indus5Response()
    class AccelerometerCommand(val isEnabled: Boolean) : Indus5Response()
    class PpgFrame(val data: ByteArray) : Indus5Response()
    class PpgCommand(val isEnabled: Boolean) : Indus5Response()
    class EegStopResponse() : Indus5Response()
    class UnknownResponse(bytes: ByteArray) : Indus5Response()
}