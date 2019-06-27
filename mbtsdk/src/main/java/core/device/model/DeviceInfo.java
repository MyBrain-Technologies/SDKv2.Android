package core.device.model;

/**
 * Created by Etienne on 14/10/2016.
 */

public enum DeviceInfo {

    /**
     * Corresponds to a device battery measure
     */
    BATTERY,

    /**
     * Corresponds to a call to firmware version value.
     */
    FW_VERSION,

    /**
     * Corresponds to a call to hardware version value.
     */
    HW_VERSION,

    /**
     * Corresponds to a call to serial number value.
     */
    SERIAL_NUMBER,

    /**
     * Corresponds to a call to product name value
     * Its value matchs the Bluetooth Low Energy name
     */
    PRODUCT_NAME,

    /**
     * Correspond to a call to model number value, which is supposed to contain the external identifier (QR Code)
     * The associated characteristic might not be available for some headsets
     */
    MODEL_NUMBER;
}

