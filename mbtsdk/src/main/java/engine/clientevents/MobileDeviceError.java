package engine.clientevents;

import androidx.annotation.Keep;

@Keep
public class MobileDeviceError extends BaseError {

    private static final String DOMAIN = "MobileDevice";

    private static int CODE_RANGE = 1500;

    public static String ERROR_LABEL = DOMAIN + " Error :";

    public static MobileDeviceError ERROR_GPS_DISABLED                  = new MobileDeviceError( CODE_RANGE,  "This operation could not be started : GPS disabled.");
    public static MobileDeviceError ERROR_LOCATION_PERMISSION           = new MobileDeviceError( CODE_RANGE+1,  "This operation could not be started : Location permission not granted.");
    public static MobileDeviceError ERROR_BLUETOOTH_DISABLED            = new MobileDeviceError( CODE_RANGE+2,  "This operation could not be started : Bluetooth is disabled.");
    public static MobileDeviceError ERROR_STORAGE_PERMISSION            = new MobileDeviceError( CODE_RANGE+3,  "This operation could not be started : Storage permission not granted.");
    public static MobileDeviceError ERROR_NO_FREE_SPACE                 = new MobileDeviceError( CODE_RANGE+4,  "No Free Space on the Mobile Device : impossible to store data.");

    private MobileDeviceError(int code, String exception){
        super(DOMAIN, code, exception);
    }
}


