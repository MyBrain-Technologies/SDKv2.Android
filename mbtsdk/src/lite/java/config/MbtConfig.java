package config;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import features.MbtFeatures;
import features.ScannableDevices;

public final class MbtConfig {

    public static ScannableDevices scannableDevices = ScannableDevices.ALL;

    private static int eegPacketLength = 250;

    public static int sampleRate = 250;

    private static int samplePerNotification = 4;

    /**
     * The size of the temporary buffer where the MbtEEGPackets are stored.
     * When this buffer is full, a notification is sent to the UI/Activity
     * so that the user can do what he wants with this new packets
     * (for example plot the values on a chart).
     */
    public static int eegBufferLengthClientNotif = MbtFeatures.DEFAULT_EEG_PACKET_LENGTH; //number of MbtEEGPackets to store in the buffer before notifying the client

    private static boolean batteryEventsLogsEnabled;

    private static int batteryReadPeriod;

    private static boolean offlineModeEnabled;

    private static int maxIdleDurationForDisconnecting;

    private static boolean acquisitionEnabledLowBattery;

    public static int bluetoothConnectionTimeout;

    public static int bluetoothScanTimeout;

    private static int bluetoothPairingTimeout;

    private static String serverURL;

    public static int getEegBufferLengthClientNotif() {
        return eegBufferLengthClientNotif;
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
        return bluetoothConnectionTimeout;
    }

    public static int getEegPacketLength() {
        return eegPacketLength;
    }

    public static int getBluetoothScanTimeout() {
        return bluetoothScanTimeout;
    }

    public static int getBluetoothPairingTimeout() {
        return bluetoothPairingTimeout;
    }

    public static String getServerURL() {
        return serverURL;
    }

    public static int getSampleRate() {
        return sampleRate;
    }

    public static int getSamplePerNotification() {
        return samplePerNotification;
    }

    public static ScannableDevices getScannableDevices() {
        return scannableDevices;
    }


    public static class MbtConfigBuilder {

        private int eegPacketLength;

        private int sampleRate;

        private int samplePerNotification;

        private int eegBufferLengthClientNotif;

        private boolean batteryEventsLogsEnabled;

        private int batteryReadPeriod;

        private boolean offlineModeEnabled;

        private int maxIdleDurationForDisconnecting;

        private boolean acquisitionEnabledLowBattery;

        private int bluetoothConnectionTimeout;

        private int bluetoothScanTimeout;

        private int bluetoothPairingTimeout;

        private String serverURL;

        @NonNull
        public MbtConfigBuilder setEegPacketLength(final int eegPacketLength) {
            this.eegPacketLength = eegPacketLength;
            return this;
        }

        @NonNull
        public MbtConfigBuilder setSampleRate(final int sampleRate) {
            this.sampleRate = sampleRate;
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
        public MbtConfigBuilder setBluetoothConnectionTimeout(final int bluetoothConnectionTimeout) {
            this.bluetoothConnectionTimeout = bluetoothConnectionTimeout;
            return this;
        }

        @NonNull
        public MbtConfigBuilder setBluetoothScanTimeout(final int bluetoothScanTimeout) {
            this.bluetoothScanTimeout = bluetoothScanTimeout;
            return this;
        }

        @NonNull
        public MbtConfigBuilder setBluetoothPairingTimeout(final int bluetoothPairingTimeout) {
            this.bluetoothPairingTimeout = bluetoothPairingTimeout;
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
        eegPacketLength = builder.eegPacketLength;
        sampleRate = builder.sampleRate;
        samplePerNotification = builder.samplePerNotification;
        eegBufferLengthClientNotif = builder.eegBufferLengthClientNotif;
        batteryEventsLogsEnabled = builder.batteryEventsLogsEnabled;
        batteryReadPeriod = builder.batteryReadPeriod;
        offlineModeEnabled = builder.offlineModeEnabled;
        maxIdleDurationForDisconnecting = builder.maxIdleDurationForDisconnecting;
        acquisitionEnabledLowBattery = builder.acquisitionEnabledLowBattery;
        bluetoothConnectionTimeout = builder.bluetoothConnectionTimeout;
        bluetoothScanTimeout = builder.bluetoothScanTimeout;
        bluetoothPairingTimeout = builder.bluetoothPairingTimeout;
        serverURL = builder.serverURL;
    }

    public static void setScannableDevices(ScannableDevices scannableDevices) {
        MbtConfig.scannableDevices = scannableDevices;
    }

    public static void setSamplePerNotification(int samplePerNotification) {
        MbtConfig.samplePerNotification = samplePerNotification;
    }

}
