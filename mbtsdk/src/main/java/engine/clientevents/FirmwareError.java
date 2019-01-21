package engine.clientevents;

import android.support.annotation.Keep;

@Keep
public final class FirmwareError extends BaseError{

    private static String DOMAIN = "Firmware";

    private static int CODE_RANGE = 1000;

    public static String ERROR_LABEL = DOMAIN + " Error :";

    public static FirmwareError ERROR_INCOMPATIBLE_VERSION          = new FirmwareError(CODE_RANGE, "Incompatible firmware version : update is necessary.");
    public static FirmwareError ERROR_RECONNECT_FAILED              = new FirmwareError(CODE_RANGE+1, "Impossible to reconnect the headset after firmware update.");
    public static FirmwareError ERROR_ALREADY_UPTODATE              = new FirmwareError(CODE_RANGE+2, "Firmware already up-to-date.");
    public static FirmwareError ERROR_TIMEOUT_UPDATE                = new FirmwareError(CODE_RANGE+3, "Firmware update could not be completed within the permitted time.");
    public static FirmwareError ERROR_PREPARING_REQUEST_FAILED      = new FirmwareError(CODE_RANGE+4, "Preparing OAD Transfer request failed.");
    public static FirmwareError ERROR_UNCOMPLETE_UPDATE             = new FirmwareError(CODE_RANGE+5, "OAD Transfer is not complete");
    public static FirmwareError ERROR_WRONG_FIRMWARE_VERSION        = new FirmwareError(CODE_RANGE+6, "Current firmware version does not match the update version");

    private FirmwareError(int code, String exception) {
        super(DOMAIN, code, exception);
    }
}
