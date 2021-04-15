package core.bluetooth.lowenergy

sealed class Indus5Response() {
    class MtuChangedResponse(val sampleSize: Int) : Indus5Response()
    class EegFrameResponse(val data: ByteArray) : Indus5Response()
    class EegStartResponse() : Indus5Response()
    class EegStopResponse() : Indus5Response()
    class UnknownResponse(bytes: ByteArray) : Indus5Response()
}