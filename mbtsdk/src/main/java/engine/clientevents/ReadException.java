package engine.clientevents;

public final class ReadException extends BaseException {

    public static final String DEVICE_NOT_CONNECTED = "device not connected, impossible to perform read operation";
    public static final String READ_TIMEOUT = "read operation has failed to start or has timed out.";

    public ReadException(String exception){
        super(exception);
    }

}
