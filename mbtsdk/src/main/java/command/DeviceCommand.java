package command;


import java.nio.ByteBuffer;

import engine.SimpleRequestCallback;
import engine.clientevents.BaseError;
import engine.clientevents.BaseErrorEvent;
import engine.clientevents.ConfigError;

/**
 * Mailbox command sent from the SDK to the headset
 * in order to configure a parameter,
 * or get values stored by the headset
 * or ask the headset to perform an action.
 * It provides a callback used to return a raw response sent by the headset to the SDK
 */
public abstract class DeviceCommand <U extends BaseError> implements BaseErrorEvent<U> {

    final byte ENABLE = 0x01;
    final byte DISABLE = 0x00;

    /**
     * Unique identifier of the command.
     * This code is sent to the headset in the write characteristic operation.
     */
    private byte identifierCode;

    /**
     * Optional additional code associated to the identifier code
     * to add security and avoid requests sent by hackers
     */
    private byte[] additionalCodes;

    /**
     * Buffer that hold the identifier code,
     * additional codes
     * and all the data specific to the implemented class
     */
    private ByteBuffer rawDataBuffer;


    /**
     * Callback that returns the raw response of the headset to the SDK
     * This raw response is a byte array that has be to converted to be readable.
     */
    SimpleRequestCallback<byte[]> responseCallback;

    DeviceCommand(byte mailboxCode) {
        this.identifierCode = mailboxCode;
    }

    DeviceCommand(byte mailboxCode, byte... additionalCodes) {
        this.identifierCode = mailboxCode;
        this.additionalCodes = additionalCodes;
    }

    /**
     * Get the callback that returns the raw response of the headset to the SDK
     * @return the callback that returns the raw response of the headset to the SDK
     */
    public SimpleRequestCallback<byte[]> getResponseCallback() {
        return responseCallback;
    }

    /**
     * Return the unique identifier of the command
     * @return the unique identifier of the command
     */
    public byte getCode() {
        return identifierCode;
    }

    /**
     * Allocate a buffer that bundle all the data to send to the headset
     * for the write characteristic operation
     */
    private void allocateBuffer(){
        rawDataBuffer = null; //reset the temporary buffer

        int bufferSize = 1; //the buffer contains at least the identifier device command identifier code

        if(additionalCodes != null)
            bufferSize += additionalCodes.length;

        if(getData() != null) //get data returns the optional data specific to the implemented class
            bufferSize += getData().length;

        rawDataBuffer = ByteBuffer.allocate(bufferSize);
    }

    /**
     * Add the identifier code and the additional codes
     * to the raw data buffer to send to the headset
     */
    private void fillHeader(){
        rawDataBuffer.put(identifierCode);

        if(additionalCodes != null)
            for(byte singleCode : additionalCodes){
                rawDataBuffer.put(singleCode);
            }
    }

    /**
     * Add the optional data specific to the implemented class
     * to the raw data buffer to send to the headset
     * @return the complete buffer (identifier + additional codes + optional data)
     */
    private byte[] fillPayload(){
        if(getData() != null){
            for(byte singleData : getData()){
                rawDataBuffer.put(singleData);
            }
        }
        return rawDataBuffer.array();
    }

    /**
     * Bundles the data to send to the headset
     * for the write characteristic operation / request
     * @return the bundled data in a byte array
     */
    public byte[] serialize(){
        allocateBuffer();
        fillHeader();
        return fillPayload();
    }

    /**
     * Init the device command to send to the headset
     */
    public void init(){
        if(!isValid())
            responseCallback.onError(ConfigError.ERROR_INVALID_PARAMS, "Invalid parameter : the input must not be null and/or empty.");
    }

    /**
     * Returns true if the client inputs
     * are valid for sending the command
     */
    public abstract boolean isValid();

    /**
     * Returns the optional data specific to the implemented class
     * @return the optional data specific to the implemented class
     */
    public abstract byte[] getData();

}



