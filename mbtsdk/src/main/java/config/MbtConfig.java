package config;

public final class MbtConfig {

    /**
     * This enum contains all MBT devices that can be scanned by this SDK. By default, ALL is selected
     */
    public enum ScannableDevices{
        MELOMIND,
        VPRO,
        ALL
    }

    public static ScannableDevices scannableDevices = ScannableDevices.ALL;

    public static final String MELOMIND_DEVICE_NAME_PREFIX = "melo_";

    public static final String MELOMIND_DEVICE_NAME = "melomind";
    public static final String VPRO_DEVICE_NAME = "VPro";
    public static final String ALL_DEVICE_NAME = "All";
    public static String CURRENT_DEVICE_NAME;

    public final static int SAMPLE_RATE = 250;
    public final static int SAMPLE_PER_NOTIF = 4;

    public final static int MELOMIND_NB_CHANNELS = 2;
    public final static int VPRO_NB_CHANNELS = 8;
    public static int CURRENT_NB_CHANNELS ;

    public final static int eegPacketLength = 250;

    public static final int MELOMIND_STATUS_SIZE = 0;
    public static final int VPRO_STATUS_SIZE = 3;

    public static final int MELOMIND_DEVICE_NAME_MAX_LENGTH = 10;

    private static boolean batteryEventsLogsEnabled;

    private static int batteryCheckTimer;

    private static boolean landscapeModeEnabled;

    private static boolean offlineModeEnabled;

    private static int maxIdleDurationForDisconnecting;

    private static boolean connectionEnabledLowBattery;

    private static boolean acquisitionEnabledLowBattery;

    private static boolean p300Enabled;

    private static int bluetoothConnectionTimeout;

    private static String serverURL;

    private static String versionNameSuffix;

    private static String fwVersion;

    private static String algoVersion;

    private static Byte[] EEGConfiguration;

    /*public MbtConfig(ScannableDevices scannableDevices, boolean batteryEventsLogsEnabled, int batteryCheckTimer, boolean landscapeModeEnabled, boolean offlineModeEnabled, int maxIdleDurationForDisconnecting, boolean connectionEnabledLowBattery, boolean acquisitionEnabledLowBattery, boolean p300Enabled, int bluetoothConnectionTimeout, String serverURL, String versionNameSuffix, String fwVersion, String algoVersion, int EEGPacketsLength, Byte[] EEGConfiguration, int nbChannels, int sampRate){
        this.scannableDevices=scannableDevices;
        this.batteryEventsLogsEnabled=batteryEventsLogsEnabled;
        this.batteryCheckTimer=batteryCheckTimer;
        this.landscapeModeEnabled=landscapeModeEnabled;
        this.offlineModeEnabled=offlineModeEnabled;
        this.maxIdleDurationForDisconnecting=maxIdleDurationForDisconnecting;
        this.connectionEnabledLowBattery=connectionEnabledLowBattery;
        this.acquisitionEnabledLowBattery=acquisitionEnabledLowBattery;
        this.p300Enabled=p300Enabled;
        this.bluetoothConnectionTimeout=bluetoothConnectionTimeout;
        this.serverURL=serverURL;
        this.versionNameSuffix=versionNameSuffix;
        this.fwVersion=fwVersion;
        this.algoVersion=algoVersion;
        this.EEGConfiguration=EEGConfiguration;
    }*/

    public void init() {
        if(scannableDevices!=null){
            switch (scannableDevices){
                case ALL:
                    CURRENT_DEVICE_NAME=ALL_DEVICE_NAME;
                    CURRENT_NB_CHANNELS=VPRO_NB_CHANNELS;
                    break;

                case VPRO:
                    CURRENT_DEVICE_NAME=VPRO_DEVICE_NAME;
                    CURRENT_NB_CHANNELS=VPRO_NB_CHANNELS;
                    break;

                case MELOMIND:
                    CURRENT_DEVICE_NAME=MELOMIND_DEVICE_NAME;
                    CURRENT_NB_CHANNELS=MELOMIND_NB_CHANNELS;
                    break;

                default:
                    CURRENT_DEVICE_NAME=ALL_DEVICE_NAME;
                    CURRENT_NB_CHANNELS=VPRO_NB_CHANNELS;
                    break;
            }
        }
    }

    public static boolean isBatteryEventsLogsEnabled() {
        return batteryEventsLogsEnabled;
    }

    public static int getBatteryCheckTimer() {
        return batteryCheckTimer;
    }

    public static boolean isLandscapeModeEnabled() {
        return landscapeModeEnabled;
    }

    public static boolean isOfflineModeEnabled() {
        return offlineModeEnabled;
    }

    public static int getMaxIdleDurationForDisconnecting() {
        return maxIdleDurationForDisconnecting;
    }

    public static boolean isConnectionEnabledLowBattery() {
        return connectionEnabledLowBattery;
    }

    public static boolean isAcquisitionEnabledLowBattery() {
        return acquisitionEnabledLowBattery;
    }

    public static boolean isP300Enabled() {
        return p300Enabled;
    }

    public static int getBluetoothConnectionTimeout() {
        return bluetoothConnectionTimeout;
    }

    public static String getServerURL() {
        return serverURL;
    }

    public static String getVersionNameSuffix() {
        return versionNameSuffix;
    }

    public static String getFwVersion() {
        return fwVersion;
    }

    public static String getAlgoVersion() {
        return algoVersion;
    }

    public static int getEEGPacketsLength() {
        return eegPacketLength;
    }

    public static Byte[] getEEGConfiguration() {
        return EEGConfiguration;
    }


    public static String getCurrentDeviceName(){ return CURRENT_DEVICE_NAME; }

    public static String getCurrentNbChannel(){ return CURRENT_DEVICE_NAME; }


    public static class MbtConfigBuilder {

        private boolean batteryEventsLogsEnabled;

        private int batteryCheckTimer;

        private boolean landscapeModeEnabled;

        private boolean offlineModeEnabled;

        private int maxIdleDurationForDisconnecting;

        private boolean connectionEnabledLowBattery;

        private boolean acquisitionEnabledLowBattery;

        private boolean p300Enabled;

        private int bluetoothConnectionTimeout;

        private String serverURL;

        private String versionNameSuffix;

        private String fwVersion;

        private String algoVersion;

        private Byte[] EEGConfiguration;

        public MbtConfigBuilder setBatteryEventsLogsEnabled(final boolean batteryEventsLogsEnabled) {
            this.batteryEventsLogsEnabled = batteryEventsLogsEnabled;
            return this;
        }

        public MbtConfigBuilder setBatteryCheckTimer(final int batteryCheckTimer) {
            this.batteryCheckTimer = batteryCheckTimer;
            return this;
        }

        public MbtConfigBuilder setLandscapeModeEnabled(final boolean landscapeModeEnabled) {
            this.landscapeModeEnabled = landscapeModeEnabled;
            return this;
        }

        public MbtConfigBuilder setOfflineModeEnabled(final boolean offlineModeEnabled) {
            this.offlineModeEnabled = offlineModeEnabled;
            return this;
        }

        public MbtConfigBuilder setMaxIdleDurationForDisconnecting(final int maxIdleDurationForDisconnecting) {
            this.maxIdleDurationForDisconnecting = maxIdleDurationForDisconnecting;
            return this;
        }

        public MbtConfigBuilder setConnectionEnabledLowBattery(final boolean connectionEnabledLowBattery) {
            this.connectionEnabledLowBattery = connectionEnabledLowBattery;
            return this;
        }

        public MbtConfigBuilder setAcquisitionEnabledLowBattery(final boolean acquisitionEnabledLowBattery) {
            this.acquisitionEnabledLowBattery = acquisitionEnabledLowBattery;
            return this;
        }

        public MbtConfigBuilder setP300Enabled(final boolean p300Enabled) {
            this.p300Enabled = p300Enabled;
            return this;
        }

        public MbtConfigBuilder setBluetoothConnectionTimeout(final int bluetoothConnectionTimeout) {
            this.bluetoothConnectionTimeout = bluetoothConnectionTimeout;
            return this;
        }

        public MbtConfigBuilder setServerURL(final String serverURL) {
            this.serverURL = serverURL;
            return this;
        }

        public MbtConfigBuilder setVersionNameSuffix(final String versionNameSuffix) {
            this.versionNameSuffix = versionNameSuffix;
            return this;
        }

        public MbtConfigBuilder setFwVersion(final String fwVersion) {
            this.fwVersion = fwVersion;
            return this;
        }

        public MbtConfigBuilder setAlgoVersion(final String algoVersion) {
            this.algoVersion = algoVersion;
            return this;
        }


        public MbtConfigBuilder setEEGConfiguration(final Byte[] EEGConfiguration) {
            this.EEGConfiguration = EEGConfiguration;
            return this;
        }

        public MbtConfig create() {
            return new MbtConfig(this);
        }
    }

    private MbtConfig(final MbtConfigBuilder builder) {
        batteryEventsLogsEnabled = builder.batteryEventsLogsEnabled;
        batteryCheckTimer = builder.batteryCheckTimer;
        landscapeModeEnabled = builder.landscapeModeEnabled;
        offlineModeEnabled = builder.offlineModeEnabled;
        maxIdleDurationForDisconnecting = builder.maxIdleDurationForDisconnecting;
        connectionEnabledLowBattery = builder.connectionEnabledLowBattery;
        acquisitionEnabledLowBattery = builder.acquisitionEnabledLowBattery;
        p300Enabled = builder.p300Enabled;
        bluetoothConnectionTimeout = builder.bluetoothConnectionTimeout;
        serverURL = builder.serverURL;
        versionNameSuffix = builder.versionNameSuffix;
        fwVersion = builder.fwVersion;
        algoVersion = builder.algoVersion;
        EEGConfiguration = builder.EEGConfiguration;
    }




}
