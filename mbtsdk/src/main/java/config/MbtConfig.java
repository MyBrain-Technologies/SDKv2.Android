package config;

import java.util.ArrayList;

import features.MbtAcquisitionLocations;
import features.ScannableDevices;

public final class MbtConfig {

    public static ScannableDevices scannableDevices = ScannableDevices.ALL;

    public static int eegPacketLength = 250;

    private static boolean batteryEventsLogsEnabled;

    private static int batteryReadPeriod;

    private static boolean offlineModeEnabled;

    private static int maxIdleDurationForDisconnecting;

    private static boolean acquisitionEnabledLowBattery;

    private static int bluetoothConnectionTimeout;

    private static int bluetoothScanTimeout;

    private static int bluetoothPairingTimeout;

    private static String serverURL;

    private static Byte[] EEGConfiguration;


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

    /*public static Byte[] getEEGConfiguration() {
        return EEGConfiguration;
    }*/

    public static ScannableDevices getScannableDevices() {
        return scannableDevices;
    }

    public static class MbtConfigBuilder {

        private boolean batteryEventsLogsEnabled;

        private int batteryReadPeriod;

        private boolean offlineModeEnabled;

        private int maxIdleDurationForDisconnecting;

        private boolean acquisitionEnabledLowBattery;

        private int bluetoothConnectionTimeout;

        private int bluetoothScanTimeout;

        private int bluetoothPairingTimeout;

        private String serverURL;

        private Byte[] EEGConfiguration;

        public MbtConfigBuilder setBatteryEventsLogsEnabled(final boolean batteryEventsLogsEnabled) {
            this.batteryEventsLogsEnabled = batteryEventsLogsEnabled;
            return this;
        }

        public MbtConfigBuilder setBatteryReadPeriod(final int batteryReadPeriod) {
            this.batteryReadPeriod = batteryReadPeriod;
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

        public MbtConfigBuilder setAcquisitionEnabledLowBattery(final boolean acquisitionEnabledLowBattery) {
            this.acquisitionEnabledLowBattery = acquisitionEnabledLowBattery;
            return this;
        }

        public MbtConfigBuilder setBluetoothConnectionTimeout(final int bluetoothConnectionTimeout) {
            this.bluetoothConnectionTimeout = bluetoothConnectionTimeout;
            return this;
        }

        public MbtConfigBuilder setBluetoothScanTimeout(final int bluetoothScanTimeout) {
            this.bluetoothScanTimeout = bluetoothScanTimeout;
            return this;
        }

        public MbtConfigBuilder setBluetoothPairingTimeout(final int bluetoothPairingTimeout) {
            this.bluetoothPairingTimeout = bluetoothPairingTimeout;
            return this;
        }

        public MbtConfigBuilder setServerURL(final String serverURL) {
            this.serverURL = serverURL;
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
        batteryReadPeriod = builder.batteryReadPeriod;
        offlineModeEnabled = builder.offlineModeEnabled;
        maxIdleDurationForDisconnecting = builder.maxIdleDurationForDisconnecting;
        acquisitionEnabledLowBattery = builder.acquisitionEnabledLowBattery;
        bluetoothConnectionTimeout = builder.bluetoothConnectionTimeout;
        bluetoothScanTimeout = builder.bluetoothScanTimeout;
        bluetoothPairingTimeout = builder.bluetoothPairingTimeout;
        serverURL = builder.serverURL;
        EEGConfiguration = builder.EEGConfiguration;
    }




}
