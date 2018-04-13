package core.bluetooth.lowenergy;

import java.util.UUID;

public final class MelomindCharacteristics {
    // Services UUIDS

    /**
     * The Device Information Service UUID. Short UUID 0xB1A0
     * This service is advertised in the advertisement so it can be picked up in the LE Scan
     */
    final static UUID SERVICE_INFOS = UUID.fromString("0000b1a0-0000-1000-8000-00805f9b34fb");


    /**
     * The Device Information Service UUID. Short UUID 0x180A
     * This service is advertised in the advertisement so it can be picked up in the LE Scan
     */
    final static UUID SERVICE_DEVICE_INFOS = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");

    /**
     * The Device Measurement Service UUID. Short UUID 0xB2A0
     */
    final static UUID SERVICE_MEASUREMENT = UUID.fromString("0000b2a0-0000-1000-8000-00805f9b34fb");

    // Characteristics UUIDS

    /**
     * The Device Properties characteristic UUID from the Device Information Service. Short UUID 0xB1A1
     */
    final static UUID CHARAC_INFOS_PROPS = UUID.fromString("0000b1a1-0000-1000-8000-00805f9b34fb");

    /**
     * The Device Capabilities characteristic UUID from the Device Information Service. Short UUID 0xB1A2
     */
    final static UUID CHARAC_INFOS_CAPAB = UUID.fromString("0000b1a2-0000-1000-8000-00805f9b34fb");

    /**
     * The Device Configuration characteristic UUID from the Device Measurement Service. Short UUID 0xB2A1
     */
    final static UUID CHARAC_MEASUREMENT_CONFIG = UUID.fromString("0000b2a1-0000-1000-8000-00805f9b34fb");

    /**
     * The Device Mailbox characteristic UUID from the Device Measurement Service. Short UUID 0xB2A4
     */
    final static UUID CHARAC_HEADSET_STATUS = UUID.fromString("0000b2a3-0000-1000-8000-00805f9b34fb");

    /**
     * The Device Mailbox characteristic UUID from the Device Measurement Service. Short UUID 0xB2A4
     */
    final static UUID CHARAC_MEASUREMENT_MAILBOX = UUID.fromString("0000b2a4-0000-1000-8000-00805f9b34fb");

    /**
     * The Device Brain Activity characteristic UUID from the Device Measurement Service. Short UUID 0xB2A5
     */

    final static UUID CHARAC_MEASUREMENT_BRAIN_ACTIVITY = UUID.fromString("0000b2a5-0000-1000-8000-00805f9b34fb");

    /**
     * The Device Brain Activity characteristic UUID from the Device Measurement Service. Short UUID 0xB2A6
     */
    final static UUID CHARAC_MEASUREMENT_OAD_PACKETS_TRANSFER = UUID.fromString("0000b2a6-0000-1000-8000-00805f9b34fb");

    /**
     * The Device Battery Level characteristic UUID from the Device Measurement Service. Short UUID 0xB2A2
     */
    final static UUID CHARAC_MEASUREMENT_BATTERY_LEVEL = UUID.fromString("0000b2a2-0000-1000-8000-00805f9b34fb");

    /**
     * The Device Serial Number characteristic UUID from the Device Info Service. Short UUID 0x2A25
     */
    final static UUID CHARAC_INFO_SERIAL_NUMBER = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");


    /**
     * The Device Hardware Number characteristic UUID from the Device Info Service. Short UUID 0x2A27
     */
    final static UUID CHARAC_INFO_HARDWARE_VERSION = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");

    /**
     * The Device Firmware Version characteristic UUID from the Device Info Service. Short UUID 0x2A26
     */
    final static UUID CHARAC_INFO_FIRMWARE_VERSION = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");

    /**
     * The Device Software Version characteristic UUID from the Device Info Service. Short UUID 0x2A28
     */
    final static UUID CHARAC_INFO_SOFTWARE_VERSION= UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");

    /**
     * The UUID for the descriptor of a characteristic. It is the sasme for all characteristics
     */
    final static UUID NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

}
