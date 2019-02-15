package core.bluetooth.requests;

/**
 * An event class when a connection request is being sent by the user.
 */
public class ScanAndConnectRequestEvent extends BluetoothRequests {
    private String name;

    public ScanAndConnectRequestEvent(String name){
        this.name = name;
    }

    public String getName(){
        return this.name;
    }

}