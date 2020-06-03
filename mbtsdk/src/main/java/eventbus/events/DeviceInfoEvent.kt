package eventbus.events

import core.device.model.DeviceInfo
import java.io.Serializable
/**
 * Gets information stored in this event class
 * @return the information
 */
class DeviceInfoEvent<T>(val deviceInfo: DeviceInfo, val info: T?) : Serializable, IEvent {

}