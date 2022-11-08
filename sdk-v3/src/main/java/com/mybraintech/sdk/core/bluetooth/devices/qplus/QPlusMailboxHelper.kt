package com.mybraintech.sdk.core.bluetooth.devices.qplus

import timber.log.Timber

object QPlusMailboxHelper {
    fun generateMtuChangeBytes(mtuSize: Int = 47): ByteArray {
        val result = EnumIndus5FrameSuffix.MBX_TRANSMIT_MTU_SIZE.bytes.toMutableList()
        result.add(mtuSize.toByte())
        return result.toByteArray()
    }

    fun parseRawIndus5Response(byteArray: ByteArray): QPlusResponse {
        try {
            return when (byteArray[0]) {
                EnumIndus5FrameSuffix.MBX_TRANSMIT_MTU_SIZE.getOperationCode() -> {
                    // only keep the 2nd byte where stores sample number
                    QPlusResponse.MtuChange(byteArray[1].toInt())
                }
                EnumIndus5FrameSuffix.MBX_GET_BATTERY_VALUE.getOperationCode() -> {
                    // 0x00 .. 0x04 = 0% | 0x05 = 12,5% -> 0x0C = 100%
                    val percent = if (byteArray[1] < 4) 0f else ((byteArray[1] - 4) * 12.5f)
                    QPlusResponse.BatteryLevel(percent)
                }
                EnumIndus5FrameSuffix.MBX_GET_DEVICE_NAME.getOperationCode() -> {
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    QPlusResponse.DeviceName(String(data))
                }
                EnumIndus5FrameSuffix.MBX_GET_FIRMWARE_VERSION.getOperationCode() -> {
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    QPlusResponse.FirmwareVersion(String(data))
                }
                EnumIndus5FrameSuffix.MBX_GET_HARDWARE_VERSION.getOperationCode() -> {
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    QPlusResponse.HardwareVersion(String(data))
                }
                EnumIndus5FrameSuffix.MBX_GET_SERIAL_NUMBER.getOperationCode() -> {
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    QPlusResponse.SerialNumber(String(data))
                }
                EnumIndus5FrameSuffix.MBX_P300_ENABLE.getOperationCode() -> {
                    QPlusResponse.TriggerStatusConfiguration(byteArray[1].toInt())
                }
                EnumIndus5FrameSuffix.MBX_EEG_DATA_FRAME_EVT.getOperationCode() -> {
                    // remove operation code
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    QPlusResponse.EEGFrame(data)
                }
                EnumIndus5FrameSuffix.MBX_START_EEG_ACQUISITION.getOperationCode() -> {
                    QPlusResponse.EEGStatus(true)
                }
                EnumIndus5FrameSuffix.MBX_STOP_EEG_ACQUISITION.getOperationCode() -> {
                    QPlusResponse.EEGStatus(false)
                }
                EnumIndus5FrameSuffix.MBX_START_IMS_ACQUISITION.getOperationCode() -> {
                    QPlusResponse.ImsStatus(true)
                }
                EnumIndus5FrameSuffix.MBX_STOP_IMS_ACQUISITION.getOperationCode() -> {
                    QPlusResponse.ImsStatus(false)
                }
                EnumIndus5FrameSuffix.MBX_START_PPG_ACQUISITION.getOperationCode() -> {
                    QPlusResponse.PpgStatus(true)
                }
                EnumIndus5FrameSuffix.MBX_STOP_PPG_ACQUISITION.getOperationCode() -> {
                    QPlusResponse.PpgStatus(false)
                }
                EnumIndus5FrameSuffix.MBX_IMS_DATA_FRAME_EVT.getOperationCode() -> {
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    QPlusResponse.ImsFrame(data)
                }
                EnumIndus5FrameSuffix.MBX_PPG_DATA_FRAME_EVT.getOperationCode() -> {
                    // optimize performance : keep op code in bytes to reduce calculations
                    QPlusResponse.PpgFrame(byteArray)
                }
                else -> {
                    QPlusResponse.UnknownResponse(byteArray)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            return QPlusResponse.UnknownResponse(byteArray)
        }
    }
}