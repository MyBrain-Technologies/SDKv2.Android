package engine.clientevents;

import android.support.annotation.Keep;

import features.MbtFeatures;

@Keep
public final class ConfigError extends BaseError{

    private static String DOMAIN = "Configuration";

    private static int CODE_RANGE = 1300;

    public static String ERROR_LABEL = DOMAIN + " Error :";

    public static String SCANNING_MINIMUM_DURATION = "Scanning duration must be higher than 10 seconds";
    public static String NOTIFICATION_PERIOD_RANGE = "Notification period must be included in the following range : [" + MbtFeatures.MIN_CLIENT_NOTIFICATION_PERIOD_IN_MILLIS + ","+ MbtFeatures.MAX_CLIENT_NOTIFICATION_PERIOD_IN_MILLIS + "]";
    public static String NOTIFICATION_PERIOD_RANGE_QUALITIES = "Notification period must be included in the following range : [" + MbtFeatures.MIN_CLIENT_NOTIFICATION_PERIOD_WITH_QUALITIES_IN_MILLIS + ","+ MbtFeatures.MAX_CLIENT_NOTIFICATION_PERIOD_WITH_QUALITIES_IN_MILLIS + "]";

    public static ConfigError ERROR_INVALID_PARAMS          = new ConfigError(CODE_RANGE, "Invalid configuration parameters.");
    public static ConfigError ERROR_FILTER                  = new ConfigError(CODE_RANGE+1, "Failed to apply filter configuration.");
    public static ConfigError ERROR_OFFSET                  = new ConfigError(CODE_RANGE+2, "Failed to enable DC offset.");
    public static ConfigError ERROR_SATURATION              = new ConfigError(CODE_RANGE+3, "Failed to enable saturation.");
    public static ConfigError ERROR_P300                    = new ConfigError(CODE_RANGE+4, "Failed to enable P300.");
    public static ConfigError ERROR_MTU                     = new ConfigError(CODE_RANGE+5, "Failed to change MTU configuration value.");
    public static ConfigError ERROR_GAIN                    = new ConfigError(CODE_RANGE+6, "Failed to change amplifier gain configuration value.");
    public static ConfigError ERROR_CALLBACK                = new ConfigError(CODE_RANGE+6, "Failed to change amplifier gain configuration value.");

    private ConfigError(int code, String exception) {
        super(DOMAIN, code, exception);
    }
}
