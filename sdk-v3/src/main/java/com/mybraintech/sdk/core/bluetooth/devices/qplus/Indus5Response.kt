package com.mybraintech.sdk.core.bluetooth.devices.qplus

import com.mybraintech.sdk.core.model.AccelerometerConfig
import com.mybraintech.sdk.core.model.DeviceSystemStatus
import com.mybraintech.sdk.core.model.EnumEEGFilterConfig
import com.mybraintech.sdk.core.model.Indus5SensorStatus

sealed class Indus5Response {
    class AudioNameFetched(val audioName: String) : Indus5Response()
    class AudioNameChanged(val newAudioName: String) : Indus5Response()
    class FirmwareVersion(val version: String) : Indus5Response()
    class HardwareVersion(val version: String) : Indus5Response()
    class GetSerialNumber(val serialNumber: String) : Indus5Response()
    class MtuChange(val size: Int) : Indus5Response()
    class TriggerStatusConfiguration(val triggerStatusAllocationSize: Int) : Indus5Response()
    class EEGFrame(val data: ByteArray) : Indus5Response()
    class EEGStatus(val isEnabled: Boolean) : Indus5Response()
    class BatteryLevel(val percent: Float) : Indus5Response()
    class ImsFrame(val data: ByteArray) : Indus5Response()
    class ImsStatus(val isEnabled: Boolean) : Indus5Response()
    class PpgFrame(val data: ByteArray) : Indus5Response()
    class PpgStatus(val isEnabled: Boolean) : Indus5Response()
    class SerialNumberChanged(val newSerialNumber: String) : Indus5Response()
    class GetDeviceSystemStatus(val deviceSystemStatus: DeviceSystemStatus) : Indus5Response()
    class GetSensorStatuses(val sensorStatuses: Indus5SensorStatus) : Indus5Response()
    class SetIMSConfig(val accelerometerConfig: AccelerometerConfig) : Indus5Response()
    class GetIMSConfig(val accelerometerConfig: AccelerometerConfig) : Indus5Response()
    class GetEEGFilterConfig(val config: EnumEEGFilterConfig) : Indus5Response()
    class SetEEGFilterConfig(val appliedConfig: EnumEEGFilterConfig) : Indus5Response()
    class UnknownResponse(val bytes: ByteArray) : Indus5Response()

    @Suppress("unused")
    class Error(val error: String) : Indus5Response()
}
