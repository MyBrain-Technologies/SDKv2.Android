package com.mybraintech.sdk.core.bluetooth.devices.melomind

import com.mybraintech.sdk.core.decodeFromHex
import java.nio.ByteBuffer
import java.util.*

/**
 * A class that contains all currently used device commands (mailbox & other commands) codes.
 * One code has one specific functionality.
 */
enum class DeviceCommandEvent {
  /**
   * Event codes related to OAD Events
   */
  MBX_START_OTA_TXF(0x03.toByte()),  // Used by appli to request an OTA update (provides software major and minor in payload)
  MBX_OTA_IDX_RESET_EVT(0x06.toByte()),  // Notifies appli that we request a packet Idx reset
  MBX_OTA_STATUS_EVT(0x07.toByte()),  // Notifies appli with the status of the OTA transfer.
  OTA_STATUS_TRANSFER((-1).toByte()),  // Notifies SDK when an OAD packet has been transferred.
  OTA_BLUETOOTH_RESET((-2).toByte()),  // Notifies SDK when an the Bluetooth has been reset
  MBX_OTA_MODE_EVT(0x05.toByte(),
      object : HashMap<String, Byte>() {
        init {
          put("CMD_CODE_OTA_MODE_EVT_FAILED", 0x00.toByte())
          put("CMD_CODE_OTA_MODE_EVT_SUCCESS", 0xFF.toByte())
        }
      }
  ),

  /**
   * Event codes related to Device System Events
   */
  MBX_SET_ADS_CONFIG(0x00.toByte()), MBX_SET_AUDIO_CONFIG(0x01.toByte()),

  /**
   * Event codes related to Device Configuration Events
   */
  MBX_SET_PRODUCT_NAME(0x02.toByte()),  // Product name configuration request
  MBX_SET_NOTCH_FILT(0x0B.toByte()),  // allows to hotswap the filters' parameters
  MBX_SET_BANDPASS_FILT(0x0C.toByte()),  // Set the signal bandwidth by changing the embedded bandpass filter
  MBX_SET_AMP_GAIN(0x0D.toByte()),  // Set the eeg signal amplifier gain
  MBX_P300_ENABLE(0x0F.toByte()),  // Enable or disable the p300 functionnality of the melomind.
  MBX_DC_OFFSET_ENABLE(0x10.toByte()),  // Enable or disable the DC offset measurement computation and sending.
  MBX_SET_SERIAL_NUMBER(0x0A.toByte(),
      0x53.toByte(), 0x4D.toByte()),
  MBX_SET_EXTERNAL_NAME(MBX_SET_SERIAL_NUMBER.identifierCode,
      0xAB.toByte(), 0x21.toByte()),

  /**
   * Event codes related to a reading operation
   */
  MBX_SYS_GET_STATUS(0x08.toByte()),  // allows to retrieve to system global status
  MBX_GET_EEG_CONFIG(0x0E.toByte()),  // Get the current configuration of the Notch filter, the bandpass filter, and the amplifier gain.
  GET_EEG_CONFIG_ADDITIONAL(0x00.toByte(), 0x00.toByte()),  // Additional info to get the current configuration .
  CMD_GET_BATTERY_VALUE(0x20.toByte()),
  CMD_START_EEG_ACQUISITION(0x24.toByte()),
  CMD_STOP_EEG_ACQUISITION(0x25.toByte()),
  CMD_GET_DEVICE_CONFIG(0x50.toByte()),
  CMD_GET_PSM_CONFIG(0x51.toByte()),
  CMD_GET_IMS_CONFIG(0x52.toByte()),
  CMD_SET_DEVICE_CONFIG(0x53.toByte()),
  CMD_GET_DEVICE_INFO(0x54.toByte()),
  START_FRAME(0x3C.toByte()),
  PAYLOAD_LENGTH(0x00.toByte(), 0x00.toByte()),
  COMPRESS(0x00.toByte()),
  PACKET_ID(0x00.toByte()),
  PAYLOAD(0x00.toByte()),  // Additional info to get the current configuration .

  /**
   * Event codes related to Audio Connection Events
   */
  MBX_CONNECT_IN_A2DP(0x11.toByte(), byteArrayOf(0x25.toByte(), 0xA2.toByte()),
      object : HashMap<String, Byte>() {
        init {
          put("CMD_CODE_CONNECT_IN_A2DP_IN_PROGRESS", 0x01.toByte())
          put("CMD_CODE_CONNECT_IN_A2DP_FAILED_BAD_BDADDR", 0x02.toByte())
          put("CMD_CODE_CONNECT_IN_A2DP_FAILED_ALREADY_CONNECTED", 0x04.toByte())
          put("CMD_CODE_CONNECT_IN_A2DP_FAILED_TIMEOUT", 0x08.toByte())
          put("CMD_CODE_CONNECT_IN_A2DP_LINKKEY_INVALID", 0x10.toByte())
          put("CMD_CODE_CONNECT_IN_A2DP_JACK_CONNECTED", 0x20.toByte())
          put("CMD_CODE_CONNECT_IN_A2DP_SUCCESS", 0x80.toByte())
        }
      }
  ),

  /**
   * Event codes related to Audio Reconnection Events
   */
  MBX_SYS_REBOOT_EVT(0x09.toByte(),
      0x29.toByte(), 0x08.toByte()),

  /**
   * Event codes related to Audio Disconnection Events
   */
  MBX_DISCONNECT_IN_A2DP(0x12.toByte(), byteArrayOf(0x85.toByte(), 0x11.toByte()),
      object : HashMap<String, Byte>() {
        init {
          put("CMD_CODE_DISCONNECT_IN_A2DP_FAILED", 0x01.toByte())
          put("CMD_CODE_DISCONNECT_IN_A2DP_SUCCESS", 0xFF.toByte())
        }
      }
  ),

  /**
   * Unused codes
   */
  MBX_LEAD_OFF_EVT(0x04.toByte()),  // Notifies app of a lead off modification
  MBX_BAD_EVT(0xFF.toByte());

  /**
   * Returns the unique identifier of the command
   * @return the unique identifier of the command
   */
  /**
   * Unique identifier of the event.
   */
  var identifierCode: Byte
    private set

  /**
   * Returns the optional additional code associated to the event
   * to add security and avoid requests sent by hackers.
   */
  /**
   * Optional additional code associated to the event
   * to add security and avoid requests sent by hackers.
   */
  var additionalCodes: ByteArray? = null
    private set

  /**
   * Optional additional codes associated to the event
   * that holds the possible responses returned once the device command has been sent.
   */
  private var responseCodesMap: HashMap<String, Byte>? = null

  constructor(identifierCode: Byte, additionalCodes: ByteArray, responseCodesMap: HashMap<String, Byte>?) {
    this.identifierCode = identifierCode
    this.additionalCodes = additionalCodes
    this.responseCodesMap = responseCodesMap
  }

  constructor(identifierCode: Byte, responseCodesMap: HashMap<String, Byte>) {
    this.identifierCode = identifierCode
    this.responseCodesMap = responseCodesMap
  }

  constructor(identifierCode: Byte, vararg additionalCodes: Byte) {
    this.identifierCode = identifierCode
    this.additionalCodes = additionalCodes
  }

  constructor(identifierCode: Byte) {
    this.identifierCode = identifierCode
  }

  val assembledCodes: ByteArray
    get() = assembleCodes(byteArrayOf(identifierCode), additionalCodes)

  /**
   * Returns the ptional additional codes associated to the event
   * that holds the possible responses returned once the device command has been sent.
   */
  fun getResponseCodeForKey(key: String): Byte {
    if (responseCodesMap == null) {
      return 0.toByte()
    }
    return if (responseCodesMap!!.containsKey(key)) responseCodesMap!![key]?:0.toByte() else 0.toByte()
  }

  companion object {
    const val CMD_CODE_CONNECT_IN_A2DP_IN_PROGRESS = "CMD_CODE_CONNECT_IN_A2DP_IN_PROGRESS"
    const val CMD_CODE_CONNECT_IN_A2DP_FAILED_BAD_BDADDR = "CMD_CODE_CONNECT_IN_A2DP_FAILED_BAD_BDADDR"
    const val CMD_CODE_CONNECT_IN_A2DP_FAILED_ALREADY_CONNECTED = "CMD_CODE_CONNECT_IN_A2DP_FAILED_ALREADY_CONNECTED"
    const val CMD_CODE_CONNECT_IN_A2DP_FAILED_TIMEOUT = "CMD_CODE_CONNECT_IN_A2DP_FAILED_TIMEOUT"
    const val CMD_CODE_CONNECT_IN_A2DP_LINKKEY_INVALID = "CMD_CODE_CONNECT_IN_A2DP_LINKKEY_INVALID"
    const val CMD_CODE_CONNECT_IN_A2DP_JACK_CONNECTED = "CMD_CODE_CONNECT_IN_A2DP_JACK_CONNECTED"
    const val CMD_CODE_CONNECT_IN_A2DP_SUCCESS = "CMD_CODE_CONNECT_IN_A2DP_SUCCESS"
    const val CMD_CODE_DISCONNECT_IN_A2DP_FAILED = "CMD_CODE_DISCONNECT_IN_A2DP_FAILED"
    const val CMD_CODE_DISCONNECT_IN_A2DP_SUCCESS = "CMD_CODE_DISCONNECT_IN_A2DP_SUCCESS"
    const val CMD_CODE_OTA_MODE_EVT_FAILED = "CMD_CODE_OTA_MODE_EVT_FAILED"
    const val CMD_CODE_OTA_MODE_EVT_SUCCESS = "CMD_CODE_OTA_MODE_EVT_SUCCESS"

    /**
     * Returns the event that has an identifier that matchs the identifier code
     * @param identifierCode the identifier code
     * @return the event that has an identifier that matchs the identifier code
     */
    fun getEventFromIdentifierCode(identifierCode: Byte): DeviceCommandEvent? {
      for (event in values()) {
        if (identifierCode == event.identifierCode) return event
      }
      return null
    }

    @JvmStatic
    fun assembleCodes(vararg codes: ByteArray?): ByteArray {
      var bufferLength = 0
      for (code in codes) {
        if (code != null) {
          for (codeByte in code) {
            bufferLength++
          }
        }
      }
      val buffer = ByteBuffer.allocate(bufferLength)
      for (code in codes) {
        if (code != null) {
          for (codeByte in code) {
            buffer.put(codeByte)
          }
        }
      }
      return buffer.array()
    }
  }
}