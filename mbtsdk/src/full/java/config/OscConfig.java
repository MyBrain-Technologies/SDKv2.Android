package config;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

@Keep
public final class OscConfig {

    private String ipAddress;

    private int port;

    public OscConfig(@NonNull String ipAddress, int port){

        if(ipAddress == null || ipAddress.isEmpty())
            throw new IllegalArgumentException("Impossible to stream data to a null or empty IP address");

        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

}
