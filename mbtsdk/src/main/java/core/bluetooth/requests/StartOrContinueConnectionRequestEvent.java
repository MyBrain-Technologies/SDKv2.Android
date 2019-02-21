package core.bluetooth.requests;

/**
 * An event class when a connection request is being sent
 */
public class StartOrContinueConnectionRequestEvent extends BluetoothRequests {

    private boolean isClientUserRequest;
    private String nameOfDeviceRequested;

    public StartOrContinueConnectionRequestEvent(boolean isClientUserRequest, String nameOfDeviceRequested){
            this.isClientUserRequest = isClientUserRequest;
            this.nameOfDeviceRequested = nameOfDeviceRequested;
    }

    public boolean isClientUserRequest() {
            return isClientUserRequest;
    }

    public String getNameOfDeviceRequested() {
        return nameOfDeviceRequested;
    }
}