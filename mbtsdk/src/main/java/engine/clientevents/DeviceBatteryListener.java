package engine.clientevents;

import androidx.annotation.Keep;

@Keep
public interface DeviceBatteryListener<U extends BaseError> extends BaseErrorEvent<U>{
    void onBatteryLevelReceived(String level);
//    void onFwVersionReceived(String fwVersion);
//    void onHwVersionReceived(String hwVersion);
//    void onSerialNumberReceived(String serialNumber);
//    void onModelNumberReceived(String modelNumber);
}