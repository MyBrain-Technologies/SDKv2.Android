package eventbus.events

import command.DeviceCommandEvent
import core.bluetooth.requests.ResponseEvent
import core.device.event.OADEvent

/**
 * Event class that holds a response received by the Bluetooth SDK unit, that need to be transferred to an other unit.
 * This event is sent using [org.greenrobot.eventbus.EventBus] framework.
 * The response consists of a message/notification associated to a nullable data value.
 * The data are bundled in a map where every data values are associated to a unique key.
 * String is the type of the data identifier (key)
 * Byte is the type of the data (value)
 */
class BluetoothResponseEvent(eventDataKey: DeviceCommandEvent?, eventDataValue: Any?) :
    ResponseEvent<DeviceCommandEvent?, Any?>(eventDataKey, eventDataValue),
    IEvent {
  val isDeviceCommandEvent: Boolean
    get() = OADEvent.getEventFromMailboxCommand(this.id) != null
}