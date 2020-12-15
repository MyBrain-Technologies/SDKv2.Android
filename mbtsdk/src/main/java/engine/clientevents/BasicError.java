package engine.clientevents;

import androidx.annotation.Keep;

@Keep
public final class BasicError extends BaseError{

    private static String DOMAIN = "Basic";

    private static int CODE_RANGE = 1800;

    private static String ERROR_LABEL = DOMAIN + " Error : ";

    public static BasicError ERROR_ACCESS_FORBIDDEN                      = new BasicError(CODE_RANGE, "Access forbidden.");
    public static BasicError ERROR_ENCRYPTION                            = new BasicError(CODE_RANGE+1, "Failed to decrypt or encrypt data.");
    public static BasicError ERROR_TIMEOUT                               = new BasicError(CODE_RANGE+2, "This operation could not be completed within the permitted time.");
    public static BasicError ERROR_ANDROID_INCOMPATIBLE                  = new BasicError(CODE_RANGE+3, "This operation could not be started : a minimum Android API 22 version is required.");
    public static BasicError ERROR_INIT                                  = new BasicError(CODE_RANGE+4, "MbtClient instance has not been initialized : please call MbtClient.init() inside the OnCreate() method of your application activity before trying to access to the SDK features.");
    public static BasicError ERROR_ALREADY_INIT                          = new BasicError(CODE_RANGE+5, "Client has already been initialized : please call MbtClient.getClientInstance() instead.");
    public static BasicError ERROR_UNIMPLEMENTED_FEATURE                 = new BasicError(CODE_RANGE+6, "This feature is not implemented yet.");
    public static BasicError ERROR_UNSUPPORTED_FEATURE                   = new BasicError(CODE_RANGE+7, "This feature is not supported.");
    public static BasicError ERROR_UNKNOWN                               = new BasicError(CODE_RANGE+8, "Unknown cause.");
    public static BasicError ERROR_CANCELED                              = new BasicError(CODE_RANGE+9, "Cancel operation.");
    public static BasicError NO_INTERNET                                 = new BasicError(CODE_RANGE+10, "Application is offline");

    public BasicError(Exception exception) {
        super(exception);
    }
    public BasicError(int code, String exception) {
        super(DOMAIN, code, exception);
    }
}
