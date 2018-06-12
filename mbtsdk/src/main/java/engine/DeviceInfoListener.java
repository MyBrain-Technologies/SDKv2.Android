package engine;

public interface DeviceInfoListener extends MbtClientEvents{
    void onBatteryChanged(String newLevel);
    void onFwVersionReceived(String fwVersion);
    void onHwVersionReceived(String hwVersion);
    void onSerialNumberReceived(String serialNumber);
}