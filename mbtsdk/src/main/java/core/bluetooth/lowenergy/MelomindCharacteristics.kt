package core.bluetooth.lowenergy

import java.util.*

/**
 * This class holds the melomind custom bluetooth services and characteristics.
 */
class MelomindCharacteristics {

  companion object {
    // Services UUIDS
    /**
     * The Device Information Service UUID. Short UUID 0x180A
     * This service is advertised in the advertisement so it can be picked up in the LE Scan
     */
    val SERVICE_DEVICE_INFOS: UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")

    /**
     * The Device Measurement Service UUID. Short UUID 0xB2A0
     */
    val SERVICE_MEASUREMENT: UUID = UUID.fromString("0000b2a0-0000-1000-8000-00805f9b34fb")
    // Characteristics UUIDS
    /**
     * The Device Properties characteristic UUID from the Device Information Service. Short UUID 0xB1A1
     */
    val CHARAC_INFOS_PROPS: UUID = UUID.fromString("0000b1a1-0000-1000-8000-00805f9b34fb")

    /**
     * The Device Capabilities characteristic UUID from the Device Information Service. Short UUID 0xB1A2
     */
    val CHARAC_INFOS_CAPAB: UUID = UUID.fromString("0000b1a2-0000-1000-8000-00805f9b34fb")

    /**
     * The Device Configuration characteristic UUID from the Device Measurement Service. Short UUID 0xB2A1
     */
    val CHARAC_MEASUREMENT_CONFIG : UUID = UUID.fromString("0000b2a1-0000-1000-8000-00805f9b34fb")

    /**
     * The Device Mailbox characteristic UUID from the Device Measurement Service. Short UUID 0xB2A4
     */
    val CHARAC_HEADSET_STATUS : UUID = UUID.fromString("0000b2a3-0000-1000-8000-00805f9b34fb")

    /**
     * The Device Mailbox characteristic UUID from the Device Measurement Service. Short UUID 0xB2A4
     */
    val CHARAC_MEASUREMENT_MAILBOX : UUID = UUID.fromString("0000b2a4-0000-1000-8000-00805f9b34fb")

    /**
     * The Device Brain Activity characteristic UUID from the Device Measurement Service. Short UUID 0xB2A5
     */
    val CHARAC_MEASUREMENT_EEG : UUID = UUID.fromString("0000b2a5-0000-1000-8000-00805f9b34fb")
    //    final static UUID CHARAC_MEASUREMENT_ECG = UUID.fromString("0000b2a7");
    /**
     * The Device Brain Activity characteristic UUID from the Device Measurement Service. Short UUID 0xB2A6
     */
    val CHARAC_MEASUREMENT_OAD_PACKETS_TRANSFER : UUID = UUID.fromString("0000b2a6-0000-1000-8000-00805f9b34fb")

    /**
     * The Device Battery Level characteristic UUID from the Device Measurement Service. Short UUID 0xB2A2
     */
    val CHARAC_MEASUREMENT_BATTERY_LEVEL : UUID = UUID.fromString("0000b2a2-0000-1000-8000-00805f9b34fb")

    /**
     * The Device Serial Number characteristic UUID from the Device Info Service. Short UUID 0x2A25
     */
    val CHARAC_INFO_SERIAL_NUMBER : UUID = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")

    /**
     * The Device Hardware Number characteristic UUID from the Device Info Service. Short UUID 0x2A27
     */
    val CHARAC_INFO_HARDWARE_VERSION : UUID = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")

    /**
     * The Device Firmware Version characteristic UUID from the Device Info Service. Short UUID 0x2A26
     */
    val CHARAC_INFO_FIRMWARE_VERSION : UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")

    /**
     * The Device Firmware Version characteristic UUID from the Device Info Service. Short UUID 0x2A26
     */
    val CHARAC_INFO_MODEL_NUMBER : UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")

    /**
     * The Device Software Version characteristic UUID from the Device Info Service. Short UUID 0x2A28
     */
    val CHARAC_INFO_SOFTWARE_VERSION : UUID = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb")

    /**
     * The UUID for the descriptor of a characteristic. It is the sasme for all characteristics
     */
    val NOTIFICATION_DESCRIPTOR_UUID : UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
  }

}