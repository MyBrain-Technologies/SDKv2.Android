package engine.clientevents;

import androidx.annotation.Keep;

@Keep
public final class EegError extends BaseError{

    private static String DOMAIN = "Eeg";

    private static int CODE_RANGE = 1700;

    public static String ERROR_LABEL = DOMAIN + " Error :";

    public static String STREAMING_ALREADY_STARTED = " Streaming already started.";

    public static EegError ERROR_TIMEOUT                            = new EegError(CODE_RANGE, "Acquisition operation could not be completed within the permitted time.");
    public static EegError ERROR_CONVERT                            = new EegError(CODE_RANGE+1, "Failed to convert raw EEG data.");
    public static EegError ERROR_FAIL_START_STREAMING               = new EegError(CODE_RANGE+2, "Failed to start streaming.");
    public static EegError ERROR_ALREADY_STOPPED_STREAMING          = new EegError(CODE_RANGE+3, "Failed to stop streaming : already stopped.");
    public static EegError ERROR_QUALITIES_DISABLED_CONFIG          = new EegError(CODE_RANGE+4, "Impossible to get qualities : you didn't enable it on your stream configuration.");
    public static EegError ERROR_LOST_PACKETS                       = new EegError(CODE_RANGE+5, "Too many lost EEG Packets detected.");
    public static EegError ERROR_FEATURES                           = new EegError(CODE_RANGE+6, "Failed to extract features from EEG signal.");
    public static EegError ERROR_FAILED_COMPUTE_QUALITIES           = new EegError(CODE_RANGE+7, "Failed to compute EEG signal quality.");
    public static EegError ERROR_RELAXATION_INDEXES                 = new EegError(CODE_RANGE+8, "Failed to compute relaxation index.");
    public static EegError ERROR_CALIBRATION_MISSING                = new EegError(CODE_RANGE+9, "Impossible to perform this operation if no calibration has be done before.");
    public static EegError ERROR_MATRIX_INVERTED                    = new EegError(CODE_RANGE+10, "This processing operation failed : EEG data Matrix must be inverted.");
    public static EegError ERROR_ALREADY_STREAMING                          = new EegError(CODE_RANGE+11, "Already streaming");

    private EegError(int code, String exception) {
        super(DOMAIN, code, exception);
    }
}
