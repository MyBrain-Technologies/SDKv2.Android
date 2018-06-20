package engine.clientevents;

import android.support.annotation.Keep;

@Keep
public final class ConnectionException extends BaseException {
    public static final String BT_NOT_ACTIVATED = "Bluetooth adapter is disabled, please enable adapter first.";
    public static final String GPS_DISABLED = "LE Scanner needs access to GPS but GPS is disabled, please enable GPS and try again";
    public static final String GPS_PERMISSIONS_NOT_GRANTED = "LE Scanner needs access to GPS but permissions are not granted, please give permissions to GPS and try again";
    public static final String LE_SCAN_FAILURE = "LE Scan has failed to start";
    public static final String CONNECTION_FAILURE = "Connection operation has failed. Please try again";
    private static final String BASE_INVALID_PARAMETERS = "Invalid parameter: ";

    public static final String INVALID_NAME = BASE_INVALID_PARAMETERS + "Input name does not match the required format. Name must start with melo_ or vpro_";
    public static final String INVALID_SCAN_DURATION = BASE_INVALID_PARAMETERS + "Scan duration is too small. It must be at least 10sec. Please change duration and try again";
    public static final String INVALID_CONNECTION_DURATION = BASE_INVALID_PARAMETERS + "Connection duration is too small. It must be at least 10sec. Please change duration and try again";

    public ConnectionException(String exception){
        super(exception);
    }
}
