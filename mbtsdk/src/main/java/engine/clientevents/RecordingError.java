package engine.clientevents;

import android.support.annotation.Keep;

@Keep
public final class RecordingError extends BaseError{

    private static String DOMAIN = "Recording";

    private static int CODE_RANGE = 1400;

    public static String ERROR_LABEL = DOMAIN + " Error :";

    public static RecordingError ERROR_CREATE           = new RecordingError(CODE_RANGE, "Failed to createForDevice Recording / JSON file.");
    public static RecordingError ERROR_WRITE            = new RecordingError(CODE_RANGE+1, "Failed to write JSON file.");
    public static RecordingError ERROR_READ             = new RecordingError(CODE_RANGE+2, "Failed to read Recording / JSON file.");
    public static RecordingError ERROR_DELETE           = new RecordingError(CODE_RANGE+3, "Failed to delete Recording / JSON file.");
    public static RecordingError ERROR_UPDATE           = new RecordingError(CODE_RANGE+4, "Failed to update Recording / JSON file.");
    public static RecordingError ERROR_INVALID_PATH     = new RecordingError(CODE_RANGE+5, "Invalid Recording / JSON file path.");
    public static RecordingError ERROR_TOO_BIG          = new RecordingError(CODE_RANGE+6, "Maximum Recording size reached : the Recording has been stopped.");

    private RecordingError(int code, String exception) {
        super(DOMAIN, code, exception);
    }
}
