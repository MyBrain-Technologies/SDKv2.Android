package engine.clientevents;

import android.support.annotation.Keep;

@Keep
public class HeadsetDeviceError extends BaseError {

    private static final String DOMAIN = "HeadsetDevice";

    private static int CODE_RANGE = 1200;

    public static String ERROR_LABEL = DOMAIN + " Error :";

    public static HeadsetDeviceError ERROR_TIMEOUT                          = new HeadsetDeviceError(CODE_RANGE, "No response received from the headset within the permitted time.");
    public static HeadsetDeviceError ERROR_TIMEOUT_DEVICE_INFO              = new HeadsetDeviceError( CODE_RANGE+1,  "No received Device Info value within the permitted time.");
    public static HeadsetDeviceError ERROR_TIMEOUT_FIRMWARE_VERSION         = new HeadsetDeviceError( CODE_RANGE+2,  "No received Firmware version value within the permitted time.");
    public static HeadsetDeviceError ERROR_TIMEOUT_HARDWARE_VERSION         = new HeadsetDeviceError( CODE_RANGE+3,  "No received Hardware version value within the permitted time.");
    public static HeadsetDeviceError ERROR_TIMEOUT_SERIAL_NUMBER            = new HeadsetDeviceError( CODE_RANGE+4,  "No received Serial Number value within the permitted time.");
    public static HeadsetDeviceError ERROR_TIMEOUT_BATTERY                  = new HeadsetDeviceError( CODE_RANGE+5,  "No received Battery Level value within the permitted time.");
    public static HeadsetDeviceError ERROR_TIMEOUT_MODEL_NUMBER                  = new HeadsetDeviceError( CODE_RANGE+6,  "No received Model Number value within the permitted time.");
    public static HeadsetDeviceError ERROR_DECODE_BATTERY                   = new HeadsetDeviceError( CODE_RANGE+7,  "Failed to decode battery level value.");
    public static HeadsetDeviceError ERROR_TIMEOUT_SATURATION               = new HeadsetDeviceError( CODE_RANGE+8,  "No received Saturation value within the permitted time.");
    public static HeadsetDeviceError ERROR_TIMEOUT_OFFSET                   = new HeadsetDeviceError( CODE_RANGE+9,  "No received DC Offset value within the permitted time.");
    public static HeadsetDeviceError ERROR_PREFIX_NAME                      = new HeadsetDeviceError( CODE_RANGE+10,  "Invalid headset name : it must start with melo_ prefix.");
    public static HeadsetDeviceError ERROR_MELOMIND_INCOMPATIBLE            = new HeadsetDeviceError( CODE_RANGE+11,  "Feature not available for Melomind headset.");
    public static HeadsetDeviceError ERROR_VPRO_INCOMPATIBLE                = new HeadsetDeviceError( CODE_RANGE+12,  "Feature not available for VPro headset.");
    public static HeadsetDeviceError ERROR_SENSORS                          = new HeadsetDeviceError( CODE_RANGE+13,  "This operation encountered a problem : check that the electrodes are plugged.");

    private HeadsetDeviceError(int code, String exception){
        super(DOMAIN, code, exception);
    }
}


