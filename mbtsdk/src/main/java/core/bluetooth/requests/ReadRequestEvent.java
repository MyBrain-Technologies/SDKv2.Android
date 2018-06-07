package core.bluetooth.requests;

import core.bluetooth.requests.BluetoothRequests;
import core.recordingsession.metadata.DeviceInfo;

public class ReadRequestEvent extends BluetoothRequests {

    private DeviceInfo deviceInfo;

    public ReadRequestEvent(DeviceInfo deviceInfo){
        this.deviceInfo = deviceInfo;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }
}
