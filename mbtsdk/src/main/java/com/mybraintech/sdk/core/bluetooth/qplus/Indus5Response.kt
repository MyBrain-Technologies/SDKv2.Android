package com.mybraintech.sdk.core.bluetooth.qplus

sealed class Indus5Response {
    /**
     * this name should be the suffix of the advertising name. Eg: advertising name = QP1B234567 -> name should be 1B234567
     */
    class DeviceName(val name: String) : Indus5Response()
    class FirmwareVersion(val version: String) : Indus5Response()
    class HardwareVersion(val version: String) : Indus5Response()
    class SerialNumber(val serialNumber: String) : Indus5Response()
    class MtuChange(val size: Int) : Indus5Response()
    class TriggerConfiguration(val triggerSize: Int) : Indus5Response()
    class EEGFrame(val data: ByteArray) : Indus5Response()
    class EEGStatus(val isEnabled: Boolean) : Indus5Response()
    class BatteryLevel(val percent: Float) : Indus5Response()
    class ImsFrame(val data: ByteArray) : Indus5Response()
    class ImsStatus(val isEnabled: Boolean) : Indus5Response()
    class PpgFrame(val data: ByteArray) : Indus5Response()
    class PpgStatus(val isEnabled: Boolean) : Indus5Response()
    class UnknownResponse(bytes: ByteArray) : Indus5Response()
}
