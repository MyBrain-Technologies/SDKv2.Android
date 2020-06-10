package core.bluetooth.requests

import command.CommandInterface.MbtCommand
import engine.clientevents.BaseError
import engine.clientevents.BluetoothError
import java.io.Serializable

/**
 * Event triggered when a Mailbox command request is sent from the SDK to the headset
 * in order to configure a parameter,
 * or get values stored by the headset
 * or ask the headset to perform an action. .
 */
class CommandRequestEvent
/**
 * Event triggered when a Mailbox command request is sent from the SDK to the headset
 * in order to configure a parameter,
 * or get values stored by the headset
 * or ask the headset to perform an action. .
 */(
    /**
     * Mailbox command sent from the SDK to the headset
     * in order to configure a parameter,
     * or get values stored by the headset
     * or ask the headset to perform an action. .
     */
    val command: MbtCommand<BaseError>) : BluetoothRequests(), Serializable {
  /**
   * Get the mailbox command sent from the SDK to the headset
   * in order to configure a parameter,
   * or get values stored by the headset
   * or ask the headset to perform an action. .
   */

}