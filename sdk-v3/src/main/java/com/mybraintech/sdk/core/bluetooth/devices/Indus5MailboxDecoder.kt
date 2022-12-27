package com.mybraintech.sdk.core.bluetooth.devices

import com.mybraintech.sdk.core.bluetooth.devices.qplus.EnumIndus5FrameSuffix
import com.mybraintech.sdk.core.bluetooth.devices.qplus.Indus5Response
import com.mybraintech.sdk.core.model.AccelerometerConfig
import com.mybraintech.sdk.core.model.DeviceSystemStatus
import com.mybraintech.sdk.core.model.Indus5SensorStatus
import timber.log.Timber

object Indus5MailboxDecoder {
    fun decodeRawIndus5Response(byteArray: ByteArray): Indus5Response {
        try {
            return when (byteArray[0]) {
                EnumIndus5FrameSuffix.MBX_EEG_DATA_FRAME_EVT.getOperationCode() -> {
                    // remove operation code
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    Indus5Response.EEGFrame(data)
                }
                EnumIndus5FrameSuffix.MBX_IMS_DATA_FRAME_EVT.getOperationCode() -> {
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    Indus5Response.ImsFrame(data)
                }
                EnumIndus5FrameSuffix.MBX_PPG_DATA_FRAME_EVT.getOperationCode() -> {
                    // optimize performance : keep op code in bytes to reduce calculations
                    Indus5Response.PpgFrame(byteArray)
                }
                EnumIndus5FrameSuffix.MBX_SYS_GET_STATUS.getOperationCode() -> {
                    getDeviceSystemStatus(byteArray)
                }
                EnumIndus5FrameSuffix.MBX_GET_SENSOR_STATUS.getOperationCode() -> {
                    Indus5Response.GetSensorStatuses(Indus5SensorStatus.parse(byteArray))
                }
                EnumIndus5FrameSuffix.MBX_SET_IMS_CONFIG.getOperationCode() -> {
                    Indus5Response.SetIMSConfig(byteArray)
                }
                EnumIndus5FrameSuffix.MBX_GET_IMS_CONFIG.getOperationCode() -> {
                    Indus5Response.GetIMSConfig(AccelerometerConfig.parse(byteArray).sampleRate)
                }
                EnumIndus5FrameSuffix.MBX_TRANSMIT_MTU_SIZE.getOperationCode() -> {
                    // only keep the 2nd byte where stores sample number
                    Indus5Response.MtuChange(byteArray[1].toInt())
                }
                EnumIndus5FrameSuffix.MBX_GET_BATTERY_VALUE.getOperationCode() -> {
                    // 0x00 .. 0x04 = 0% | 0x05 = 12,5% -> 0x0C = 100%
                    val percent = if (byteArray[1] < 4) 0f else ((byteArray[1] - 4) * 12.5f)
                    Indus5Response.BatteryLevel(percent)
                }
                EnumIndus5FrameSuffix.MBX_GET_DEVICE_NAME.getOperationCode() -> {
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    Indus5Response.AudioNameFetched(String(data))
                }
                EnumIndus5FrameSuffix.MBX_SET_A2DP_NAME.getOperationCode() -> {
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    Indus5Response.AudioNameChanged(String(data))
                }
                EnumIndus5FrameSuffix.MBX_GET_FIRMWARE_VERSION.getOperationCode() -> {
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    Indus5Response.FirmwareVersion(String(data))
                }
                EnumIndus5FrameSuffix.MBX_GET_HARDWARE_VERSION.getOperationCode() -> {
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    Indus5Response.HardwareVersion(String(data))
                }
                EnumIndus5FrameSuffix.MBX_GET_SERIAL_NUMBER.getOperationCode() -> {
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    Indus5Response.GetSerialNumber(String(data))
                }
                EnumIndus5FrameSuffix.MBX_SET_SERIAL_NUMBER.getOperationCode() -> {
                    val newSerialNumber = getNewSerialNumber(byteArray)
                    Indus5Response.SerialNumberChanged(newSerialNumber)
                }
                EnumIndus5FrameSuffix.MBX_P300_ENABLE.getOperationCode() -> {
                    Indus5Response.TriggerStatusConfiguration(byteArray[1].toInt())
                }
                EnumIndus5FrameSuffix.MBX_START_EEG_ACQUISITION.getOperationCode() -> {
                    Indus5Response.EEGStatus(true)
                }
                EnumIndus5FrameSuffix.MBX_STOP_EEG_ACQUISITION.getOperationCode() -> {
                    Indus5Response.EEGStatus(false)
                }
                EnumIndus5FrameSuffix.MBX_START_IMS_ACQUISITION.getOperationCode() -> {
                    Indus5Response.ImsStatus(true)
                }
                EnumIndus5FrameSuffix.MBX_STOP_IMS_ACQUISITION.getOperationCode() -> {
                    Indus5Response.ImsStatus(false)
                }
                EnumIndus5FrameSuffix.MBX_START_PPG_ACQUISITION.getOperationCode() -> {
                    Indus5Response.PpgStatus(true)
                }
                EnumIndus5FrameSuffix.MBX_STOP_PPG_ACQUISITION.getOperationCode() -> {
                    Indus5Response.PpgStatus(false)
                }
                else -> {
                    Indus5Response.UnknownResponse(byteArray)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            return Indus5Response.UnknownResponse(byteArray)
        }
    }

    /**
     * 1 byte for operation code and 4 bytes for device system statuses
     */
    private fun getDeviceSystemStatus(bytes: ByteArray): Indus5Response.GetDeviceSystemStatus {
        return if (bytes.size != 5) {
            Timber.e("GetDeviceSystemStatus : response data size is not equal 5")
            // this case should never happen
            Indus5Response.GetDeviceSystemStatus(
                DeviceSystemStatus().apply {
                    processorStatus = DeviceSystemStatus.EnumState.STATUS_ERROR
                    externalMemoryStatus = DeviceSystemStatus.EnumState.STATUS_ERROR
                    audioStatus = DeviceSystemStatus.EnumState.STATUS_ERROR
                    adsStatus = DeviceSystemStatus.EnumState.STATUS_ERROR
                }
            )
        } else {
            Indus5Response.GetDeviceSystemStatus(
                DeviceSystemStatus().apply {
                    processorStatus = DeviceSystemStatus.parse(bytes[1])
                    externalMemoryStatus = DeviceSystemStatus.parse(bytes[2])
                    audioStatus = DeviceSystemStatus.parse(bytes[3])
                    adsStatus = DeviceSystemStatus.parse(bytes[4])
                }
            )
        }
    }

    @Suppress("LocalVariableName")
    private fun getNewSerialNumber(byteArray: ByteArray): String {
        val SERIAL_CHANGED_COMMAND_SUFFIX_SIZE = 4
        val size = byteArray.size
        return String(byteArray.copyOfRange(SERIAL_CHANGED_COMMAND_SUFFIX_SIZE, size))
    }
}