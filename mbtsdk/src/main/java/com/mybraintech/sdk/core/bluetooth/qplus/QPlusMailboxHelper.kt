package com.mybraintech.sdk.core.bluetooth.qplus

import utils.LogUtils

object QPlusMailboxHelper {
    fun generateMtuChangeBytes(mtuSize: Int = 47): ByteArray {
        val result = EnumIndus5FrameSuffix.MBX_TRANSMIT_MTU_SIZE.bytes.toMutableList()
        result.add(mtuSize.toByte())
        return result.toByteArray()
    }

    fun parseRawIndus5Response(byteArray: ByteArray): Indus5Response {
        try {
            return when (byteArray[0]) {
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
                    Indus5Response.DeviceName(String(data))
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
                    Indus5Response.SerialNumber(String(data))
                }
                EnumIndus5FrameSuffix.MBX_P300_ENABLE.getOperationCode() -> {
                    Indus5Response.TriggerConfiguration(byteArray[1].toInt())
                }
                EnumIndus5FrameSuffix.MBX_EEG_DATA_FRAME_EVT.getOperationCode() -> {
                    // remove operation code
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    Indus5Response.EEGFrame(data)
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
                EnumIndus5FrameSuffix.MBX_IMS_DATA_FRAME_EVT.getOperationCode() -> {
                    val data = byteArray.copyOfRange(1, byteArray.size)
                    Indus5Response.ImsFrame(data)
                }
                EnumIndus5FrameSuffix.MBX_PPG_DATA_FRAME_EVT.getOperationCode() -> {
                    // optimize performance : keep op code in bytes to reduce calculations
                    Indus5Response.PpgFrame(byteArray)
                }
                else -> {
                    Indus5Response.UnknownResponse(byteArray)
                }
            }
        } catch (e: Exception) {
            LogUtils.e(e)
            return Indus5Response.UnknownResponse(byteArray)
        }
    }
}