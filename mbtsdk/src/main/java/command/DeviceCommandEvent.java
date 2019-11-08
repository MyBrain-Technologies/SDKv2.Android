package command;

import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * A class that contains all currently used device commands (mailbox & other commands) codes.
 * One code has one specific functionality.
 */
public enum DeviceCommandEvent {

    /**
     * Event codes related to OAD Events
     */
    MBX_START_OTA_TXF((byte)0x03),           // Used by appli to request an OTA update (provides software major and minor in payload)
    MBX_OTA_IDX_RESET_EVT((byte)0x06),       // Notifies appli that we request a packet Idx reset
    MBX_OTA_STATUS_EVT((byte)0x07),   // Notifies appli with the status of the OTA transfer.
    OTA_STATUS_TRANSFER((byte)-1),   // Notifies SDK when an OAD packet has been transferred.
    OTA_BLUETOOTH_RESET((byte)-2),   // Notifies SDK when an the Bluetooth has been reset

    MBX_OTA_MODE_EVT((byte)0x05,
        new HashMap<String, Byte>() {{
            put(CMD_CODE_OTA_MODE_EVT_FAILED, ((byte)0x00));
            put(CMD_CODE_OTA_MODE_EVT_SUCCESS, ((byte)0xFF));
        }}
    ),

    /**
     * Event codes related to Device System Events
     */
    MBX_SET_ADS_CONFIG((byte)0x00),
    MBX_SET_AUDIO_CONFIG((byte)0x01),

    /**
     * Event codes related to Device Configuration Events
     */
    MBX_SET_PRODUCT_NAME((byte)0x02), // Product name configuration request
    MBX_SET_NOTCH_FILT((byte)0x0B),      // allows to hotswap the filters' parameters
    MBX_SET_BANDPASS_FILT((byte)0x0C),   // Set the signal bandwidth by changing the embedded bandpass filter
    MBX_SET_AMP_GAIN((byte)0x0D),        // Set the eeg signal amplifier gain
    MBX_P300_ENABLE((byte)0x0F),         // Enable or disable the p300 functionnality of the melomind.
    MBX_DC_OFFSET_ENABLE((byte)0x10),         // Enable or disable the DC offset measurement computation and sending.

    MBX_SET_SERIAL_NUMBER((byte)0x0A,
            (byte) 0x53, (byte) 0x4D),

    MBX_SET_EXTERNAL_NAME(MBX_SET_SERIAL_NUMBER.identifierCode,
            (byte) 0xAB, (byte) 0x21),

    /**
     * Event codes related to a reading operation
     */
    MBX_SYS_GET_STATUS((byte)0x08),   // allows to retrieve to system global status
    MBX_GET_EEG_CONFIG((byte)0x0E),      // Get the current configuration of the Notch filter, the bandpass filter, and the amplifier gain.
    GET_EEG_CONFIG_ADDITIONAL((byte) 0x00, (byte)0x00), // Additional info to get the current configuration .

    CMD_GET_BATTERY_VALUE ((byte)0x20),
    CMD_START_EEG_ACQUISITION((byte)0x24),
    CMD_STOP_EEG_ACQUISITION((byte)0x25),
    CMD_GET_DEVICE_CONFIG ((byte)0x50),
    CMD_GET_PSM_CONFIG((byte)0x51),
    CMD_GET_IMS_CONFIG((byte)0x52),
    CMD_SET_DEVICE_CONFIG((byte)0x53),
    CMD_GET_DEVICE_INFO((byte)0x54),

    START_FRAME((byte) 0x3C),
    PAYLOAD_LENGTH( (byte)0x00, (byte)0x00 ),
    COMPRESS((byte) 0x00),
    PACKET_ID((byte) 0x00 ),
    PAYLOAD((byte) 0x00), // Additional info to get the current configuration .

    /**
     * Event codes related to Audio Connection Events
     */
    MBX_CONNECT_IN_A2DP((byte)0x11,
            new byte[]{(byte) 0x25,(byte) 0xA2},
            new HashMap<String, Byte>() {{
                put(CMD_CODE_CONNECT_IN_A2DP_IN_PROGRESS,(byte)0x01);
                put(CMD_CODE_CONNECT_IN_A2DP_FAILED_BAD_BDADDR,(byte)0x02);
                put(CMD_CODE_CONNECT_IN_A2DP_FAILED_ALREADY_CONNECTED,(byte)0x04);
                put(CMD_CODE_CONNECT_IN_A2DP_FAILED_TIMEOUT,(byte)0x08);
                put(CMD_CODE_CONNECT_IN_A2DP_LINKKEY_INVALID,(byte)0x10);
                put(CMD_CODE_CONNECT_IN_A2DP_JACK_CONNECTED,(byte)0x20);
                put(CMD_CODE_CONNECT_IN_A2DP_SUCCESS,(byte)0x80);
                }
            }
        ),

    /**
     * Event codes related to Audio Reconnection Events
     */
    MBX_SYS_REBOOT_EVT((byte)0x09,
            (byte) 0x29,(byte) 0x08),

    /**
     * Event codes related to Audio Disconnection Events
     */
    MBX_DISCONNECT_IN_A2DP((byte)0x12,
        new byte[]{(byte) 0x85, (byte) 0x11},
        new HashMap<String, Byte>() {{
                put(CMD_CODE_DISCONNECT_IN_A2DP_FAILED, (byte) 0x01);
                put(CMD_CODE_DISCONNECT_IN_A2DP_SUCCESS, (byte) 0xFF);
            }
        }
    ),

    /**
     * Unused codes
     */
    MBX_LEAD_OFF_EVT((byte)0x04),            // Notifies app of a lead off modification
    MBX_BAD_EVT((byte)0xFF);

    public static final String CMD_CODE_CONNECT_IN_A2DP_IN_PROGRESS = "CMD_CODE_CONNECT_IN_A2DP_IN_PROGRESS";
    public static final String CMD_CODE_CONNECT_IN_A2DP_FAILED_BAD_BDADDR = "CMD_CODE_CONNECT_IN_A2DP_FAILED_BAD_BDADDR";
    public static final String CMD_CODE_CONNECT_IN_A2DP_FAILED_ALREADY_CONNECTED = "CMD_CODE_CONNECT_IN_A2DP_FAILED_ALREADY_CONNECTED";
    public static final String CMD_CODE_CONNECT_IN_A2DP_FAILED_TIMEOUT = "CMD_CODE_CONNECT_IN_A2DP_FAILED_TIMEOUT";
    public static final String CMD_CODE_CONNECT_IN_A2DP_LINKKEY_INVALID = "CMD_CODE_CONNECT_IN_A2DP_LINKKEY_INVALID";
    public static final String CMD_CODE_CONNECT_IN_A2DP_JACK_CONNECTED = "CMD_CODE_CONNECT_IN_A2DP_JACK_CONNECTED";
    public static final String CMD_CODE_CONNECT_IN_A2DP_SUCCESS = "CMD_CODE_CONNECT_IN_A2DP_SUCCESS";

    public static final String CMD_CODE_DISCONNECT_IN_A2DP_FAILED = "CMD_CODE_DISCONNECT_IN_A2DP_FAILED";
    public static final String CMD_CODE_DISCONNECT_IN_A2DP_SUCCESS = "CMD_CODE_DISCONNECT_IN_A2DP_SUCCESS";

    public static final String CMD_CODE_OTA_MODE_EVT_FAILED = "CMD_CODE_OTA_MODE_EVT_FAILED";
    public static final String CMD_CODE_OTA_MODE_EVT_SUCCESS = "CMD_CODE_OTA_MODE_EVT_SUCCESS";

    /**
     * Unique identifier of the event.
     */
    private byte identifierCode;

    /**
     * Optional additional code associated to the event
     * to add security and avoid requests sent by hackers.
     */
    private byte[] additionalCodes;

    /**
     * Optional additional codes associated to the event
     * that holds the possible responses returned once the device command has been sent.
     */
    private HashMap<String,Byte> responseCodesMap;

    DeviceCommandEvent(byte identifierCode, byte[] additionalCodes, HashMap<String, Byte> responseCodesMap) {
        this.identifierCode = identifierCode;
        this.additionalCodes = additionalCodes;
        this.responseCodesMap = responseCodesMap;
    }

    DeviceCommandEvent(byte identifierCode, HashMap<String, Byte> responseCodesMap) {
        this.identifierCode = identifierCode;
        this.responseCodesMap = responseCodesMap;
    }

    DeviceCommandEvent(byte identifierCode, byte... additionalCodes) {
        this.identifierCode = identifierCode;
        this.additionalCodes = additionalCodes;
    }


    DeviceCommandEvent(byte identifierCode) {
        this.identifierCode = identifierCode;
    }

    /**
     * Returns the unique identifier of the command
     * @return the unique identifier of the command
     */
    public byte getIdentifierCode() {
        return identifierCode;
    }

    public byte[] getAssembledCodes() {
        return assembleCodes(new byte[]{identifierCode}, additionalCodes);
    }

    /**
     * Returns the optional additional code associated to the event
     * to add security and avoid requests sent by hackers.
     */
    public byte[] getAdditionalCodes() {
        return additionalCodes;
    }

    /**
     * Returns the ptional additional codes associated to the event
     * that holds the possible responses returned once the device command has been sent.
     */
    public Byte getResponseCodeForKey(String key) {
        return (responseCodesMap.containsKey(key) ? responseCodesMap.get(key) : null);
    }

    /**
     * Returns the event that has an identifier that matchs the identifier code
     * @param identifierCode the identifier code
     * @return the event that has an identifier that matchs the identifier code
     */
    public static DeviceCommandEvent getEventFromIdentifierCode(byte identifierCode){
        for (DeviceCommandEvent event : DeviceCommandEvent.values()) {
            if(identifierCode == event.identifierCode)
                return event;
        }
        return null;
    }

    public static byte[] assembleCodes(byte[]... codes){
        int bufferLength = 0;
        for (byte[] code : codes) {
            if(code != null){
                for (byte codeByte : code) {
                    bufferLength++;
                }
            }
        }
        ByteBuffer buffer = ByteBuffer.allocate(bufferLength);
        for (byte[] code : codes) {
            if (code != null) {
                for (byte codeByte : code) {
                    buffer.put(codeByte);
                }
            }
        }
        return buffer.array();
    }
}
