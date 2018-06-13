package engine;

import android.support.annotation.NonNull;

import features.MbtFeatures;
import features.ScannableDevices;

public final class ConnectionConfig {

    private final String deviceName;

    private final int maxScanDuration;

    private final int connectionTimeout;

    private final boolean connectAudio;

    private final ScannableDevices deviceType;

    private final ConnectionStateListener connectionStateListener;

    private ConnectionConfig(String deviceName, int maxScanDuration, int connectionTimeout, boolean connectAudio, ScannableDevices deviceType, ConnectionStateListener connectionStateListener){
        this.deviceName = deviceName;
        this.maxScanDuration = maxScanDuration;
        this.connectionTimeout = connectionTimeout;
        this.deviceType = deviceType;
        this.connectAudio = (deviceType == ScannableDevices.MELOMIND && connectAudio);
        this.connectionStateListener = connectionStateListener;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public int getMaxScanDuration() {
        return maxScanDuration;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public boolean isConnectAudio() {
        return connectAudio;
    }

    public ScannableDevices getDeviceType() {
        return deviceType;
    }

    public ConnectionStateListener getConnectionStateListener() {
        return connectionStateListener;
    }


    public static class Builder{
        String deviceName = null;
        int maxScanDuration = MbtFeatures.DEFAULT_MAX_SCAN_DURATION_IN_MILLIS;
        int connectionTimeout = MbtFeatures.DEFAULT_MAX_CONNECTION_DURATION_IN_MILLIS;
        boolean connectAudio = false;
        ScannableDevices deviceType = ScannableDevices.ALL;
        final ConnectionStateListener connectionStateListener;


        public Builder(@NonNull ConnectionStateListener stateListener){
            this.connectionStateListener = stateListener;
        }

        public Builder deviceName(String deviceName){
            this.deviceName = deviceName;
            return this;
        }

        public Builder maxScanDuration(int maxScanDurationInMillis){
            this.maxScanDuration = maxScanDurationInMillis;
            return this;
        }

        public Builder connectionTimeout(int connectionTimeoutInMillis){
            this.connectionTimeout = connectionTimeoutInMillis;
            return this;
        }

        public Builder connectAudioIfDeviceCompatible(boolean shouldConnectAudio){
            this.connectAudio = shouldConnectAudio;
            return this;
        }

        public Builder scanDeviceType(ScannableDevices deviceType){
            this.deviceType = deviceType;
            return this;
        }

        public ConnectionConfig create(){
            return new ConnectionConfig(this.deviceName, this.maxScanDuration, this.connectionTimeout, this.connectAudio, this.deviceType, this.connectionStateListener);
        }




    }

}
