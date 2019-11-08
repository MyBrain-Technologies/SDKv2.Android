package core.device.event;

import command.DeviceCommandEvent;

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
    FIRMWARE_VALIDATION_RESPONSE(DeviceCommandEvent.MBX_OTA_MODE_EVT),

    /**
     * Event triggered when the Bluetooth unit is informed
     * that a sent packet has not been received by the headset device
     *
     * This event is associated with a integer "packet" value that is the OAD packet
     */
    TRANSFER_PACKET(),

    /**
     * Event triggered when the Device unit is informed
     * that a packet has been sent
     */
    PACKET_TRANSFERRED(DeviceCommandEvent.OTA_STATUS_TRANSFER),

    /**
     * Event triggered when the Bluetooth unit is informed
     * that a sent packet has not been received by the headset device
     *
     * This event is associated with a integer "packetIndex" value that is the identifier of the packet,
     *                    that allow the SDK to resend the corresponding packet     */
    LOST_PACKET(DeviceCommandEvent.MBX_OTA_IDX_RESET_EVT),

    /**
     * Event triggered when the current firmware has checked the CRC (Cyclic Redundancy Check)
     * in order to make sure that all the packets have been received and
     * that any corruption occurred while transferring the binary file.
     *
     * This event is associated with a boolean value "isTransferSuccess" that is :
     *                          - true if all the packets have been well transferred and no corruption occurred.
     *                          - false if all the packets have been well transferred and no corruption occurred
     */
    CRC_READBACK(DeviceCommandEvent.MBX_OTA_STATUS_EVT),

    /**
     * Event triggered when the headset device has disconnected after sending the CRC readback
     */
    DISCONNECTED_FOR_REBOOT(),

    /**
     * Event triggered when the headset device has disconnected while it was not expected
     */
    DISCONNECTED(),

    /**
     * Event triggered when the current headset device has been reconnected or has failed to reconnect.
     *
     * This event is associated with a boolean value "isReconnectionSuccess" that is :
     *                          - true if the connection succeeded.
     *                          - false if the connection failed.
     */
    RECONNECTION_PERFORMED(),

    BLUETOOTH_CLEARED(DeviceCommandEvent.OTA_BLUETOOTH_RESET);


    /**
     * Most OAD event (not all) are triggered by a mailbox response from the headset device
     * so we associate the corresponding mailbox identifier for these ones.
     */
    private DeviceCommandEvent mailboxEvent;

    /**
     * Bundle that stores data/informations related to the current event associated keys
     */
    private Object eventData;

    private static final String TAG = OADEvent.class.getSimpleName();

    OADEvent() { }

    OADEvent(DeviceCommandEvent mailboxEvent) {
        this.mailboxEvent = mailboxEvent;
    }

    /**
     * Set the bundle that stores values related to the current event associated keys
     */
    public OADEvent setEventData(Object eventData) {
         this.eventData = eventData;
         return this;
    }

    /**
     * Return the bundle that stores values related to the current event associated keys
     */
    public Object getEventData() {
        return eventData;
    }

    /**
     * Return the object
     * read from the bundle that contains all the values related to the current event
     */
    public boolean getEventDataAsBoolean() {
        return (boolean)eventData;
    }

    /**
     * Return the object
     * read from the bundle that contains all the values related to the current event
     */
    public String getEventDataAsString() {
        return eventData.toString();
    }

    /**
     * Return the object
     * read from the bundle that contains all the values related to the current event
     */
    public int getEventDataAsInteger() {
        return (int)eventData;
    }

    /**
     * Return the object
     * read from the bundle that contains all the values related to the current event
     */
    public short getEventDataAsShort() {
        return (short)eventData;
    }

    /**
     * Return the object
     * read from the bundle that contains all the values related to the current event
     */
    public byte[] getEventDataAsByteArray() {
        return (byte[])eventData;
    }

    /**
     * Return the OAD event associated to the mailbox command passed in input
     * @param mailboxIdentifier the mailbox command identifier
     *                          (All the mailbox command identifiers are listed in the {@link DeviceCommandEvent} class
     *                          or can be accessed through the getIdentifier() getter available for any command class that extends {@link command.DeviceCommand}
     * @return the OAD event associated to the mailbox command
     */
    public static OADEvent getEventFromMailboxCommand(DeviceCommandEvent mailboxIdentifier){
        for (OADEvent event : OADEvent.values()){
            if(event.mailboxEvent != null && event.mailboxEvent.getIdentifierCode() == mailboxIdentifier.getIdentifierCode())
                return event;
        }
        return null;
    }

    public DeviceCommandEvent getMailboxEvent() {
        return mailboxEvent;
    }}
