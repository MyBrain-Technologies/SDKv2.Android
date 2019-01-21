package engine.clientevents;

import android.support.annotation.Keep;

@Keep
public interface DeviceInfoListener<U extends BaseError> extends BaseErrorEvent<U>{
    void onBatteryChanged(String newLevel);
    void onFwVersionReceived(String fwVersion);
    void onHwVersionReceived(String hwVersion);
    void onSerialNumberReceived(String serialNumber);
}