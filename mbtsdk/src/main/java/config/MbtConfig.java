package config;

public final class MbtConfig {

    public static ScannableDevices scannableDevices = ScannableDevices.ALL;

    /**
     * This enum contains all MBT devices that can be scanned by this SDK. By default, ALL is selected
     */
    public enum ScannableDevices{
        MELOMIND,
        VPRO,
        ALL
    }
}
