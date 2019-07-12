package engine.clientevents;

import android.support.annotation.Keep;

@Keep
public final class OADError extends BaseError{

    private static String DOMAIN = "Firmware";

    private static int CODE_RANGE = 1000;

    public static String ERROR_LABEL = DOMAIN + " Error :";

    public static OADError ERROR_INCOMPATIBLE_VERSION          = new OADError(CODE_RANGE, "Incompatible firmware version : update is necessary.");
    public static OADError ERROR_RECONNECT_FAILED              = new OADError(CODE_RANGE+1, "Impossible to reconnect the headset after firmware update.");
    public static OADError ERROR_ALREADY_UPTODATE              = new OADError(CODE_RANGE+2, "Firmware already up-to-date.");
    public static OADError ERROR_TIMEOUT_UPDATE                = new OADError(CODE_RANGE+3, "Firmware update could not be completed within the permitted time.");
    public static OADError ERROR_PREPARING_REQUEST_FAILED      = new OADError(CODE_RANGE+4, "Preparing OAD Transfer request failed.");
    public static OADError ERROR_UNCOMPLETE_UPDATE             = new OADError(CODE_RANGE+5, "OAD Transfer is not complete");
    public static OADError ERROR_WRONG_FIRMWARE_VERSION        = new OADError(CODE_RANGE+6, "Current firmware version does not match the update version");
    public static OADError ERROR_FIRMWARE_UPGRADE_FAILED        = new OADError(CODE_RANGE+7, "Firmware upgrading failed or could not be completed within the permitted time.");

    private OADError(int code, String exception) {
        super(DOMAIN, code, exception);
    }
}
