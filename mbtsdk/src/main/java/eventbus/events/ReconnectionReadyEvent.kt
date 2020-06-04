package eventbus.events

/**
 * Event posted when the OAD firmware update request to the Bluetooth unit to reconnect the updated device
 *
 * @author Sophie Zecri on 24/07/2019
 */
class ReconnectionReadyEvent(val deviceName: String) : IEvent