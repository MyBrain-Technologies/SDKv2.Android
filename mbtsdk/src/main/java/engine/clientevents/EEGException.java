package engine.clientevents;


public final class EEGException extends BaseException {
    public static final String DEVICE_NOT_CONNECTED = "device not connected, impossible to start streaming";
    public static final String DEVICE_JUST_DISCONNECTED = "device has disconnected, streaming has aborted";
    public static final String STREAM_START_FAILED = "Couldn't start EEG acquisition, please try again";
    public static final String INVALID_PARAMETERS = "Invalid input parametersn, please check your configuration and try again";

    public EEGException(String exception){
        super(exception);
    }
}
