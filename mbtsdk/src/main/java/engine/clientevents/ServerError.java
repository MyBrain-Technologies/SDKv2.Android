package engine.clientevents;

import android.support.annotation.Keep;

@Keep
public final class ServerError extends BaseError{

    private static String DOMAIN = "Server";

    private static int CODE_RANGE = 1600;

    public static String ERROR_LABEL = DOMAIN + " Error :";

    public static ServerError ERROR_CONNECTION                  = new ServerError(CODE_RANGE, "Server operation failed");
    public static ServerError ERROR_AUTHENTICATION              = new ServerError(CODE_RANGE+1, "Authentication failed.");
    public static ServerError ERROR_TOKEN                       = new ServerError(CODE_RANGE+2, "Invalid or expired authentication token.");
    public static ServerError ERROR_INTERNET_CONNECTION         = new ServerError(CODE_RANGE+3, "Cannot sync : no internet connection.");
    public static ServerError ERROR_INVALID_REQUEST             = new ServerError(CODE_RANGE+4, "Invalid request.");
    public static ServerError ERROR_FAILED_SEND_REQUEST         = new ServerError(CODE_RANGE+5, "Failed to send request to the server.");
    public static ServerError ERROR_TIMEOUT                     = new ServerError(CODE_RANGE+6, "No response from the server : request could not be completed within the permitted time.");
    public static ServerError ERROR_INTERNAL_ERROR              = new ServerError(CODE_RANGE+7, "The server encountered an internal error.");
    public static ServerError ERROR_ALREADY_SENT                = new ServerError(CODE_RANGE+8, "Request already sent to the server.");

    private ServerError(int code, String exception) {
        super(DOMAIN, code, exception);
    }
}
