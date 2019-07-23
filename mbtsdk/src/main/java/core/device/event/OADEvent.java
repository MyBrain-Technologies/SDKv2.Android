package core.device.event;

import android.os.Bundle;

import java.io.Serializable;

import command.DeviceCommandEvent;
import core.device.oad.OADManager;

import static command.DeviceCommandEvent.MBX_OTA_IDX_RESET_EVT;

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

    FIRMWARE_VALIDATION_REQUEST(OADManager.VALIDATION_STATUS),

    /**
     * Event triggered when the Bluetooth unit receives a response
     * to its OAD validation request from the headset device.
     * The response returns :
     *  - true if the headset device accepts the OAD update.
     * - false if the headset device rejects the OAD update.
     *
     * This event is associated to a boolean value that is true if the headset device accepts the OAD update, false otherwise
     */
    FIRMWARE_VALIDATION_RESPONSE(OADManager.VALIDATION_STATUS),

    /**
     * Event triggered when the Bluetooth unit is informed
     * that a sent packet has not been received by the headset device
     *
     * This event is associated with a integer "packet" value that is the OAD packet
     */
    TRANSFER_PACKET(OADManager.PACKET),

    /**
     * Event triggered when the Device unit is informed
     * that a packet has been sent
     */
    PACKET_TRANSFERRED(),

    /**
     * Event triggered when the Bluetooth unit is informed
     * that a sent packet has not been received by the headset device
     *
     * This event is associated with a integer "packetIndex" value that is the identifier of the packet,
     *                    that allow the SDK to resend the corresponding packet     */
    LOST_PACKET(MBX_OTA_IDX_RESET_EVT, OADManager.LOST_PACKET),

    /**
     * Event triggered when the current firmware has checked the CRC (Cyclic Redundancy Check)
     * in order to make sure that all the packets have been received and
     * that any corruption occurred while transferring the binary file.
     *
     * This event is associated with a boolean value "isTransferSuccess" that is :
     *                          - true if all the packets have been well transferred and no corruption occurred.
     *                          - false if all the packets have been well transferred and no corruption occurred
     */
    CRC_READBACK(OADManager.READBACK_STATUS),


    /**
     * Event triggered when the headset device has disconnected after sending the CRC readback
     */
    DISCONNECTED_FOR_REBOOT(),

    /**
     * Event triggered when the current headset device has been reconnected or has failed to reconnect.
     *
     * This event is associated with a boolean value "isReconnectionSuccess" that is :
     *                          - true if the connection succeeded.
     *                          - false if the connection failed.
     */
    RECONNECTION_PERFORMED(OADManager.RECONNECTION_STATUS),

    /**
     * Event triggered when the OAD firmware update is complete and succeeded.
     */
    UPDATE_COMPLETE();

    /**
     * Most OAD event (not all) are triggered by a mailbox response from the headset device
     * so we associate the corresponding mailbox identifier for these ones.
     */
    private DeviceCommandEvent mailboxEvent;


    private String key;

    /**
     * Bundle that stores data/informations related to the current event associated keys
     */
    private Bundle eventData;

    OADEvent() { }

    OADEvent( String key) {
        this.key = key;
    }

    OADEvent(DeviceCommandEvent mailboxEvent, String key) {
        this.mailboxEvent = mailboxEvent;
        this.key = key;
    }

    /**
     * Set the bundle that stores values related to the current event associated keys
     */
    public void setEventData(Bundle eventData) {
        this.eventData = eventData;
    }

    /**
     * Return the bundle that stores values related to the current event associated keys
     */
    public Bundle getEventData() {
        return eventData;
    }

    /**
     * Return the object
     * read from the bundle that contains all the values related to the current event
     */
    public boolean getEventDataAsBoolean() {
        return eventData.getBoolean(key);
    }

    /**
     * Return the object
     * read from the bundle that contains all the values related to the current event
     */
    public String getEventDataAsString() {
        return eventData.getString(key);
    }

    /**
     * Return the object
     * read from the bundle that contains all the values related to the current event
     */
    public int getEventDataAsInteger() {
        return eventData.getInt(key);
    }

    /**
     * Return the object
     * read from the bundle that contains all the values related to the current event
     */
    public byte[] getEventDataAsByteArray() {
        return eventData.getByteArray(key);
    }

    /**
     * Return the object
     * read from the bundle that contains all the values related to the current event
     */
    public Serializable getEventDataAsSerializable() {
        return eventData.getSerializable(key);
    }

    public String getKey() {
        return key;
    }

    public OADEvent getEventWithData(Serializable eventData){
        Bundle bundle = new Bundle();
        bundle.putSerializable(key, eventData);
        setEventData(bundle);
        return this;
    }

    public OADEvent getEventWithData(byte[] eventData){
        Bundle bundle = new Bundle();
        bundle.putByteArray(key, eventData);
        setEventData(bundle);
        return this;
    }

    public OADEvent getEventWithData(Boolean eventData){
        Bundle bundle = new Bundle();
        bundle.putBoolean(key, eventData);
        setEventData(bundle);
        return this;
    }

    public OADEvent getEventWithData(String eventData){
        Bundle bundle = new Bundle();
        bundle.putString(key, eventData);
        setEventData(bundle);
        return this;
    }

    public OADEvent getEventWithData(int eventData){
        Bundle bundle = new Bundle();
        bundle.putInt(key, eventData);
        setEventData(bundle);
        return this;
    }

    /**
     * Return the OAD event associated to the mailbox command passed in input
     * @param mailboxIdentifier the mailbox command identifier
     *                          (All the mailbox command identifiers are listed in the {@link DeviceCommandEvent} class
     *                          or can be accessed through the getCode() getter available for any command class that extends {@link command.DeviceCommand}
     * @return the OAD event associated to the mailbox command
     */
    public static OADEvent getEventFromMailboxCommand(DeviceCommandEvent mailboxIdentifier){
        OADEvent event = null;
        for (OADEvent value : OADEvent.values()){
            if(value.mailboxEvent.getIdentifierCode() == mailboxIdentifier.getIdentifierCode())
                event = value;
        }
        return event;
    }
}
