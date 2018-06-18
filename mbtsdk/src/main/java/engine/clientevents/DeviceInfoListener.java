package engine.clientevents;

public interface DeviceInfoListener<U extends BaseException> extends BaseErrorEvent<U>{
    void onBatteryChanged(String newLevel);
    void onFwVersionReceived(String fwVersion);
    void onHwVersionReceived(String hwVersion);
    void onSerialNumberReceived(String serialNumber);
}