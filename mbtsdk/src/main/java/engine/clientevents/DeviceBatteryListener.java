package engine.clientevents;

import android.support.annotation.Keep;

@Keep
public interface DeviceBatteryListener<U extends BaseError> extends BaseErrorEvent<U>{
    void onBatteryChanged(String newLevel);
//    void onFwVersionReceived(String fwVersion);
//    void onHwVersionReceived(String hwVersion);
//    void onSerialNumberReceived(String serialNumber);
//    void onModelNumberReceived(String modelNumber);
}