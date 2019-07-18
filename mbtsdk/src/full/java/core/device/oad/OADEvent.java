package core.device.oad;

import static command.DeviceCommandEvents.MBX_OTA_IDX_RESET_EVT;
import static command.DeviceCommandEvents.MBX_OTA_MODE_EVT;
import static command.DeviceCommandEvents.MBX_OTA_STATUS_EVT;

/**
 * Created by Etienne on 14/10/2016.
 * OAD Event lists all the possible events that can occur during an OAD firwmare update
 */

public enum OADEvent {

    /**
     * Event triggered when the client request an OAD update.
     * Initialize the OAD process.
     */
    INIT(),

    /**
     * Event triggered when the Bluetooth unit receives a response
     * to its OAD validation request from the headset device.
     * The response returns :
     *  - true if the headset device accepts the OAD update.
     * - false if the headset device rejects the OAD update.
     *
     * This event is associated to a boolean value that is true if the headset device accepts the OAD update, false otherwise
     */
    FIRMWARE_VALIDATION(MBX_OTA_MODE_EVT),


    /**
     * Event triggered when the Bluetooth unit is informed
     * that a sent packet has not been received by the headset device
     *
     * This event is associated with a integer "packetIndex" value that is the identifier of the packet,
     *                    that allow the SDK to resend the corresponding packet     */
    LOST_PACKET(MBX_OTA_IDX_RESET_EVT),


    /**
     * Event triggered when the current firmware has checked the CRC (Cyclic Redundancy Check)
     * in order to make sure that all the packets have been received and
     * that any corruption occurred while transferring the binary file.
     *
     * This event is associated with a boolean value "isTransferSuccess" that is :
     *                          - true if all the packets have been well transferred and no corruption occurred.
     *                          - false if all the packets have been well transferred and no corruption occurred
     */
    CRC_READBACK(MBX_OTA_STATUS_EVT),


    /**
     * Event triggered when the headset device has disconnected after sending the CRC readback
     */
    DISCONNECTED,

    /**
     * Event triggered when the current headset device has been reconnected or has failed to reconnect.
     *
     * This event is associated with a boolean value "isReconnectionSuccess" that is :
     *                          - true if the connection succeeded.
     *                          - false if the connection failed.
     */
    RECONNECTION_PERFORMED,

    /**
     * Event triggered when the OAD firmware update is complete and succeeded.
     */
    UPDATE_COMPLETE;

    /**
     * Most OAD event (not all) are triggered by a mailbox response from the headset device
     * so we associate the corresponding mailbox identifier for these ones.
     */
    private byte mailboxEvent;

    private Object associatedValue;

    OADEvent() { }

    OADEvent(byte mailboxEvent) {
        this.mailboxEvent = mailboxEvent;
    }

    public void setAssociatedValue(Object associatedValue) {
        this.associatedValue = associatedValue;
    }

    public Object getAssociatedValue() {
        return associatedValue;
    }

    public static OADEvent getEventFromMailboxCommand(int mailboxIdentifier){
        OADEvent event = null;
        for (OADEvent value : OADEvent.values()){
            if(value.mailboxEvent == mailboxIdentifier)
                event = value;
        }
        return event;
    }
}
