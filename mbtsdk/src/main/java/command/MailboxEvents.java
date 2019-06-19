package command;

/**
 * A class that contains all currently used mailbox codes.
 * One code has one specific functionnality.
 */
public final class MailboxEvents{

            static final byte MBX_SET_ADS_CONFIG = 0;
            static final byte MBX_SET_AUDIO_CONFIG = 1;
            static final byte MBX_SET_PRODUCT_NAME = 2; // Product name configuration request
            static final byte MBX_START_OTA_TXF = 3;           // Used by appli to request an OTA update (provides software major and minor in payload)
            static final byte MBX_LEAD_OFF_EVT = 4;            // Notifies app of a lead off modification
            static final byte MBX_OTA_MODE_EVT = 5;            // Notifies appli that we switched to OTA mode
            static final byte MBX_OTA_IDX_RESET_EVT = 6;       // Notifies appli that we request a packet Idx reset
            static final byte MBX_OTA_STATUS_EVT = 7;   // Notifies appli with the status of the OTA transfer.
            static final byte MBX_SYS_GET_STATUS = 8;   // allows to retrieve to system global status
            static final byte MBX_SYS_REBOOT_EVT = 9;   // trigger a reboot event at disconnection
            static final byte MBX_SET_SERIAL_NUMBER = 10; // Set the melomind serial nb
            static final byte MBX_SET_SERIAL_NUMBER_ADDITIONAL_FIRST = (byte) 0x53;
            static final byte MBX_SET_SERIAL_NUMBER_ADDITIONAL_SECOND = (byte) 0x4D;

            static final byte MBX_SET_NOTCH_FILT = 11;      // allows to hotswap the filters' parameters
            static final byte MBX_SET_BANDPASS_FILT = 12;   // Set the signal bandwidth by changing the embedded bandpass filter
            static final byte MBX_SET_AMP_GAIN = 13;        // Set the eeg signal amplifier gain
            static final byte MBX_GET_EEG_CONFIG = 14;      // Get the current configuration of the Notch filter, the bandpass filter, and the amplifier gain.
            static final byte MBX_P300_ENABLE = 15;         // Enable or disable the p300 functionnality of the melomind.
            static final byte MBX_DC_OFFSET_ENABLE = 16;         // Enable or disable the DC offset measurement computation and sending.
            static final byte MBX_CONNECT_IN_A2DP = 17;         // Request the melomind to start an A2DP connection
            static final byte MBX_DISCONNECT_IN_A2DP = 18;         // Request the melomind to stop an A2DP connection

            static final byte MBX_BAD_EVT = (byte)0xFF;

            /**
             * Event codes related to #MBX_CONNECT_IN_A2DP event
             */
            static final byte CMD_CODE_CONNECT_IN_A2DP_IN_PROGRESS = 0x01;
            static final byte CMD_CODE_CONNECT_IN_A2DP_FAILED_BAD_BDADDR = 0x02;
            static final byte CMD_CODE_CONNECT_IN_A2DP_FAILED_ALREADY_CONNECTED = 0x04;
            static final byte CMD_CODE_CONNECT_IN_A2DP_FAILED_TIMEOUT = 0x08;
            static final byte CMD_CODE_CONNECT_IN_A2DP_LINKKEY_INVALID = 0x10;
            static final byte CMD_CODE_CONNECT_IN_A2DP_JACK_CONNECTED = 0x20;
            static final byte CMD_CODE_CONNECT_IN_A2DP_SUCCESS = (byte)0x80;

            /**
             * Event codes related to #MBX_DISCONNECT_IN_A2DP event
             */
            static final byte CMD_CODE_DISCONNECT_IN_A2DP_FAILED = 0x01;
            static final byte CMD_CODE_DISCONNECT_IN_A2DP_SUCCESS = (byte)0xFF;

}
