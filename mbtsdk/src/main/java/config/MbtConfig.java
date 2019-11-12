package config;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import features.MbtDeviceType;
import features.MbtFeatures;

@Keep
public final class MbtConfig { //todo remove this class and stop using static variables in config classes

    private static MbtDeviceType deviceType = MbtDeviceType.MELOMIND;

    private static int samplePerNotification = 4;

    /**
     * The size of the temporary buffer where the MbtEEGPackets are stored.
     * When this buffer is full, a notification is sent to the UI/Activity
     * so that the user can do what he wants with this new packets
     * (for example plot the values on a chart).
     */
    private static int eegBufferLengthClientNotif = MbtFeatures.DEFAULT_EEG_PACKET_LENGTH; //number of MbtEEGPackets to store in the buffer before notifying the client

    private static boolean batteryEventsLogsEnabled;

    private static int batteryReadPeriod;

    private static boolean offlineModeEnabled;

    private static int maxIdleDurationForDisconnecting;

    private static boolean acquisitionEnabledLowBattery;

    private final static int BLUETOOTH_CONNECTION_TIMEOUT = 30000;

    private static int bluetoothScanTimeout;

    private final static int BLUETOOTH_READING_TIMEOUT = 5000;

    private final static int BLUETOOTH_DISCOVER_TIMEOUT = 6000;

    private final static int BLUETOOTH_BONDING_TIMEOUT = 5000;

    private final static int BLUETOOTH_A2DP_CONNECTION_TIMEOUT = 10000;

    private static String serverURL;

    public static int getEegBufferLengthClientNotif(int sampRate) {
        return eegBufferLengthClientNotif * sampRate /1000;
    }

    public static void setEegBufferLengthClientNotif(int notificationPeriod) {
        eegBufferLengthClientNotif = notificationPeriod;
    }

    public static boolean isBatteryEventsLogsEnabled() {
        return batteryEventsLogsEnabled;
    }

    public static int getBatteryReadPeriod() {
        return batteryReadPeriod;
    }

    public static boolean isOfflineModeEnabled() {
        return offlineModeEnabled;
    }

    public static int getMaxIdleDurationForDisconnecting() {
        return maxIdleDurationForDisconnecting;
    }

    public static boolean isAcquisitionEnabledLowBattery() {
        return acquisitionEnabledLowBattery;
    }

    public static int getBluetoothConnectionTimeout() {
        return BLUETOOTH_CONNECTION_TIMEOUT;
    }


    public static int getBluetoothScanTimeout() {
        return bluetoothScanTimeout;
    }

    public static int getBluetoothDiscoverTimeout() {
        return BLUETOOTH_DISCOVER_TIMEOUT;
    }

    public static String getServerURL() {
        return serverURL;
    }

    public static int getSamplePerNotification() {
        return samplePerNotification;
    }

    public static MbtDeviceType getDeviceType() {
        return deviceType;
    }

    public static int getBluetoothA2DpConnectionTimeout() {
        return BLUETOOTH_A2DP_CONNECTION_TIMEOUT;
    }

    public static void setBluetoothScanTimeout(int maxScanDuration) {
        bluetoothScanTimeout = maxScanDuration;
    }

    public static int getBluetoothBondingTimeout() {
        return BLUETOOTH_BONDING_TIMEOUT;
    }


    public static int getBluetoothReadingTimeout() {
        return BLUETOOTH_READING_TIMEOUT;
    }

    public static class MbtConfigBuilder {

        private int eegPacketLength;

        private int samplePerNotification;

        private int eegBufferLengthClientNotif;

        private boolean batteryEventsLogsEnabled;

        private int batteryReadPeriod;

        private boolean offlineModeEnabled;

        private int maxIdleDurationForDisconnecting;

        private boolean acquisitionEnabledLowBattery;

        private int bluetoothScanTimeout;

        private String serverURL;

        private boolean connectAudioIfDeviceCompatible;

        @NonNull
        public MbtConfigBuilder setEegPacketLength(final int eegPacketLength) {
            this.eegPacketLength = eegPacketLength;
            return this;
        }


        @NonNull
        public MbtConfigBuilder setSamplePerNotification(final int samplePerNotification) {
            this.samplePerNotification = samplePerNotification;
            return this;
        }


        @NonNull
        public MbtConfigBuilder setEegBufferLengthNotification(final int eegBufferLengthNotification) {
            this.eegBufferLengthClientNotif = eegBufferLengthNotification;
            return this;
        }


        @NonNull
        public MbtConfigBuilder setBatteryEventsLogsEnabled(final boolean batteryEventsLogsEnabled) {
            this.batteryEventsLogsEnabled = batteryEventsLogsEnabled;
            return this;
        }

        @NonNull
        public MbtConfigBuilder setBatteryReadPeriod(final int batteryReadPeriod) {
            this.batteryReadPeriod = batteryReadPeriod;
            return this;
        }

        @NonNull
        public MbtConfigBuilder setOfflineModeEnabled(final boolean offlineModeEnabled) {
            this.offlineModeEnabled = offlineModeEnabled;
            return this;
        }

        @NonNull
        public MbtConfigBuilder setMaxIdleDurationForDisconnecting(final int maxIdleDurationForDisconnecting) {
            this.maxIdleDurationForDisconnecting = maxIdleDurationForDisconnecting;
            return this;
        }

        @NonNull
        public MbtConfigBuilder setAcquisitionEnabledLowBattery(final boolean acquisitionEnabledLowBattery) {
            this.acquisitionEnabledLowBattery = acquisitionEnabledLowBattery;
            return this;
        }

        @NonNull
        public MbtConfigBuilder setBluetoothScanTimeout(final int bluetoothScanTimeout) {
            this.bluetoothScanTimeout = bluetoothScanTimeout;
            return this;
        }

        @NonNull
        public MbtConfigBuilder setServerURL(final String serverURL) {
            this.serverURL = serverURL;
            return this;
        }

        @NonNull
        public MbtConfig create() {
            return new MbtConfig(this);
        }
    }

    private MbtConfig(final MbtConfigBuilder builder) {
        samplePerNotification = builder.samplePerNotification;
        eegBufferLengthClientNotif = builder.eegBufferLengthClientNotif;
        batteryEventsLogsEnabled = builder.batteryEventsLogsEnabled;
        batteryReadPeriod = builder.batteryReadPeriod;
        offlineModeEnabled = builder.offlineModeEnabled;
        maxIdleDurationForDisconnecting = builder.maxIdleDurationForDisconnecting;
        acquisitionEnabledLowBattery = builder.acquisitionEnabledLowBattery;
        bluetoothScanTimeout = builder.bluetoothScanTimeout;
        serverURL = builder.serverURL;
    }

    public static void setDeviceType(MbtDeviceType deviceType) {
        MbtConfig.deviceType = deviceType;
    }

    public static boolean isCurrentDeviceAMelomind() {
        return deviceType.equals(MbtDeviceType.MELOMIND);
    }

    public static boolean isCurrentDeviceAVpro() {
        return deviceType.equals(MbtDeviceType.VPRO);
    }

    public static void setSamplePerNotification(int samplePerNotification) {
        MbtConfig.samplePerNotification = samplePerNotification;
    }

}
