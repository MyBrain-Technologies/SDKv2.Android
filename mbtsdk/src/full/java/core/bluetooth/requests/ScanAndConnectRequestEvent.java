package core.bluetooth.requests;

/**
 * An event class when a connection request is being sent by the user.
 */
public class ScanAndConnectRequestEvent extends BluetoothRequests {
    private String name;
    private boolean connectAudioInA2DP;

    public ScanAndConnectRequestEvent(String name, boolean connectAudioInA2DP){
        this.name = name;
        this.connectAudioInA2DP = connectAudioInA2DP;
    }

    public String getName(){
        return this.name;
    }

    public boolean connectAudioInA2DP(){
        return this.connectAudioInA2DP;
    }
}