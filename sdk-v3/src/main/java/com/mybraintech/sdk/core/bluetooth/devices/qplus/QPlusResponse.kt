package com.mybraintech.sdk.core.bluetooth.devices.qplus

sealed class QPlusResponse {
    /**
     * this name should be the suffix of the advertising name. Eg: advertising name = QP1B234567 -> name should be 1B234567
     */
    class DeviceName(val name: String) : QPlusResponse()
    class FirmwareVersion(val version: String) : QPlusResponse()
    class HardwareVersion(val version: String) : QPlusResponse()
    class SerialNumber(val serialNumber: String) : QPlusResponse()
    class MtuChange(val size: Int) : QPlusResponse()
    class TriggerStatusConfiguration(val triggerStatusAllocationSize: Int) : QPlusResponse()
    class EEGFrame(val data: ByteArray) : QPlusResponse()
    class EEGStatus(val isEnabled: Boolean) : QPlusResponse()
    class BatteryLevel(val percent: Float) : QPlusResponse()
    class ImsFrame(val data: ByteArray) : QPlusResponse()
    class ImsStatus(val isEnabled: Boolean) : QPlusResponse()
    class PpgFrame(val data: ByteArray) : QPlusResponse()
    class PpgStatus(val isEnabled: Boolean) : QPlusResponse()
    class UnknownResponse(bytes: ByteArray) : QPlusResponse()
}
