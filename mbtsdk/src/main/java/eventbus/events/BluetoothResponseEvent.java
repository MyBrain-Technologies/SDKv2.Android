package eventbus.events;

import command.DeviceCommandEvent;
import core.bluetooth.requests.ResponseEvent;
import core.device.event.OADEvent;

/**
 * Event class that holds a response received by the Bluetooth SDK unit, that need to be transferred to an other unit.
 * This event is sent using {@link org.greenrobot.eventbus.EventBus} framework.
 * The response consists of a message/notification associated to a nullable data value.
 * The data are bundled in a map where every data values are associated to a unique key.
 * String is the type of the data identifier (key)
 * Byte is the type of the data (value)
 */
public class BluetoothResponseEvent extends ResponseEvent<DeviceCommandEvent, Object> {

    public BluetoothResponseEvent(DeviceCommandEvent eventDataKey, Object eventDataValue) {
        super(eventDataKey, eventDataValue);
    }

    public boolean isDeviceCommandEvent(){
        return OADEvent.getEventFromMailboxCommand(this.getId()) != null;
    }
    
}
