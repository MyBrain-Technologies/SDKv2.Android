package eventbus.events;
/**
 * Event posted when the OAD firmware update request to the Bluetooth unit to reconnect the updated device
 *
 * @author Sophie Zecri on 24/07/2019
 */
public class ReconnectionReadyEvent {

    private final String deviceName;

    public ReconnectionReadyEvent(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceName() {
        return deviceName;
    }
}
