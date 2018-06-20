package core.bluetooth.lowenergy;

/**
 * A class that contains all currently used mailbox codes.
 * One code has one specific functionnality.
 */
final class MailboxEvents{

            static final byte MBX_SET_ADS_CONFIG = 0;
            static final byte MBX_SET_AUDIO_CONFIG = 1;
            static final byte MBX_SET_PRODUCT_NAME = 2; // Product name configuration request
            static final byte MBX_START_OTA_TXF = 3;           // Used by appli to request an OTA update (provides software major and minor in payload)
            static final byte MBX_LEAD_OFF_EVT = 4;            // Notifies app of a lead off modification
            static final byte MBX_OTA_MODE_EVT = 5;            // Notifies appli that we switched to OTA mode
            static final byte MBX_OTA_IDX_RESET_EVT = 6;       // Notifies appli that we request a packet Idx reset
            static final byte MBX_OTA_STATUS_EVT = 7;   // Notifies appli with the status of the OTA transfert.
            static final byte MBX_SYS_GET_STATUS = 8;   // allows to retrieve to system global status
            static final byte MBX_SYS_REBOOT_EVT = 9;   // trigger a reboot event at disconnection
            static final byte MBX_SET_SERIAL_NUMBER = 10; // Set the melomind serial nb
            static final byte MBX_SET_NOTCH_FILT = 11;      // allows to hotswap the filters' parameters
            static final byte MBX_SET_BANDPASS_FILT = 12;   // Set the signal bandwidth by changing the embedded bandpass filter
            static final byte MBX_SET_AMP_GAIN = 13;        // Set the eeg signal amplifier gain
            static final byte MBX_GET_EEG_CONFIG = 14;      // Get the current configuration of the Notch filter, the bandpass filter, and the amplifier gain.
            static final byte MBX_P300_ENABLE = 15;         // Enable or disable the p300 functionnality of the melomind.
            static final byte MBX_DC_OFFSET_ENABLE = 15;         // Enable or disable the DC offset measurement computation and sending.
            static final byte MBX_BAD_EVT = (byte)0xFF;
}
