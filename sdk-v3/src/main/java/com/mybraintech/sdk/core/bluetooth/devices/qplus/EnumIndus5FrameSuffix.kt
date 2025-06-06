package com.mybraintech.sdk.core.bluetooth.devices.qplus

/**
 * indus 5 mailbox command:
 *
 * https://mybrain.atlassian.net/wiki/spaces/FI/pages/1957659071/INDUS5+Specification+-+Mailbox#List-of-current-mailbox-command
 */
@Suppress("unused")
enum class EnumIndus5FrameSuffix(vararg val bytes: Byte) {
    //----------------------------------------------------------------------------
    // MARK: commands
    //----------------------------------------------------------------------------
    MBX_START_OTA_TXF(0x03),
    MBX_SYS_GET_STATUS(0x08),
    MBX_SYS_REBOOT_EVT(0x09, 0x29, 0x08),
    MBX_SET_SERIAL_NUMBER(0x1A, 0x53, 0x4D),
    MBX_SET_A2DP_NAME(0x0A, 0xAB.toByte(), 0x21),
    MBX_SET_NOTCH_FILT(0x0B),
    MBX_SET_BANDPASS_FILT(0x0C),
    MBX_SET_AMP_GAIN(0x0D),
    MBX_GET_EEG_CONFIG(0x0E),
    MBX_P300_ENABLE(0x0F),
    MBX_DC_OFFSET_ENABLE(0x10),
    MBX_CONNECT_IN_A2DP(0x11),
    MBX_DISCONNECT_A2DP(0x12),
    MBX_UPGRADE_FIRMWARE(0x13),
    MBX_GET_BATTERY_VALUE(0x20),
    MBX_GET_SERIAL_NUMBER(0x22),
    MBX_GET_DEVICE_NAME(0x23),
    MBX_START_EEG_ACQUISITION(0x24),
    MBX_STOP_EEG_ACQUISITION(0x25),
    MBX_RESET(0x26),
    MBX_GET_FIRMWARE_VERSION(0x27),
    MBX_GET_HARDWARE_VERSION(0x28),
    MBX_TRANSMIT_MTU_SIZE(0x29),
    MBX_GET_FILTER_CONFIG_TYPE(0x30),
    MBX_SET_FILTER_CONFIG_TYPE(0x31),
    MBX_SET_ADS_CONFIG(0x32),
    MBX_START_IMS_ACQUISITION(0x33),
    MBX_STOP_IMS_ACQUISITION(0x34),
    MBX_START_PPG_ACQUISITION(0x35),
    MBX_STOP_PPG_ACQUISITION(0x36),

    MBX_SET_IMS_CONFIG(0x39),
    MBX_GET_IMS_CONFIG(0x3A),

    MBX_GET_SENSOR_STATUS(0x41),

    //----------------------------------------------------------------------------
    // MARK: events
    //----------------------------------------------------------------------------
    MBX_OTA_MODE_EVT(0x05),
    MBX_OTA_IDX_RESET_EVT(0x06),
    MBX_OTA_STATUS_EVT(0x07),
    MBX_EEG_DATA_FRAME_EVT(0x40),
    MBX_IMS_DATA_FRAME_EVT(0x50),
    MBX_PPG_DATA_FRAME_EVT(0x60),
    MBX_TEMP_DATA_FRAME_EVT(0x70),
    ;

    fun getOperationCode(): Byte {
        return bytes[0]
    }
}