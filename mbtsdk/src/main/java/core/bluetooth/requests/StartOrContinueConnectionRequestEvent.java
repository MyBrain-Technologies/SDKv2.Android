package core.bluetooth.requests;


import features.MbtDeviceType;

/**
 * An event class when a connection request is being sent
 */
public class StartOrContinueConnectionRequestEvent extends BluetoothRequests {

    private boolean isClientUserRequest;
    private String nameOfDeviceRequested;
    private MbtDeviceType typeOfDeviceRequested;

    public StartOrContinueConnectionRequestEvent(boolean isClientUserRequest, String nameOfDeviceRequested, MbtDeviceType typeOfDeviceRequested){
            this.isClientUserRequest = isClientUserRequest;
            this.nameOfDeviceRequested = nameOfDeviceRequested;
            this.typeOfDeviceRequested = typeOfDeviceRequested;
    }

    public boolean isClientUserRequest() {
            return isClientUserRequest;
    }

    public String getNameOfDeviceRequested() {
        return nameOfDeviceRequested;
    }

    public MbtDeviceType getTypeOfDeviceRequested() {
        return typeOfDeviceRequested;
    }
}