package command;

import android.support.annotation.Keep;

import java.nio.ByteBuffer;

import engine.clientevents.BaseError;

/**
 * Mailbox command sent from the SDK to the headset
 * in order to configure a parameter,
 * or get values stored by the headset
 * or ask the headset to perform an action.
 * It provides a callback used to return a raw response sent by the headset to the SDK
 */
@Keep
public abstract class DeviceCommand <T, U extends BaseError> extends CommandInterface.MbtCommand<U> {

    final byte ENABLE = 0x01;
    final byte DISABLE = 0x00;

    /**
     * Optional header codes
     */
    private byte[] headerCodes;

    /**
     * Unique identifier of the command.
     * This code is sent to the headset in the write characteristic operation.
     */
    private DeviceCommandEvent commandEvent;

    /**
     * Buffer that hold the identifier code,
     * additional codes
     * and all the data specific to the implemented class
     */
    private ByteBuffer rawDataBuffer;

    DeviceCommand(DeviceCommandEvent mailboxCode) {
        this.commandEvent = mailboxCode;
    }

    /**
     * Return the unique identifier of the command
     * @return the unique identifier of the command
     */
    public DeviceCommandEvent getIdentifier() {
        return commandEvent;
    }

    /**
     * Change the unique identifier of the command
     */
    public void setCommandEvent(DeviceCommandEvent commandEvent) {
        this.commandEvent = commandEvent;
    }

    /**
     * Allocate a buffer that bundle all the data to send to the headset
     * for the write characteristic operation
     */
    private void allocateBuffer(){
        rawDataBuffer = null; //reset the temporary buffer

        int bufferSize = 0; //the buffer contains at least the identifier device command identifier code

        if(headerCodes != null)
            bufferSize += headerCodes.length;

        if(commandEvent != null)
            bufferSize +=1;

        if(commandEvent != null && commandEvent.getAdditionalCodes() != null)
            bufferSize += commandEvent.getAdditionalCodes().length;

        if(getData() != null) //get data returns the optional data specific to the implemented class
            bufferSize += getData().length;

        rawDataBuffer = ByteBuffer.allocate(bufferSize);
    }

    /**
     * Add the identifier code and the additional codes
     * to the raw data buffer to send to the headset
     */
    private void fillHeader(){
        if(headerCodes != null)
            for(byte singleCode : headerCodes){
                rawDataBuffer.put(singleCode);
            }

        if(commandEvent != null) {
            rawDataBuffer.put(commandEvent.getIdentifierCode());

            if (commandEvent.getAdditionalCodes() != null)
                for (byte singleCode : commandEvent.getAdditionalCodes()) {
                    rawDataBuffer.put(singleCode);
                }
        }
    }

    /**
     * Add the optional data specific to the implemented class
     * to the raw data buffer to send to the headset
     * @return the complete buffer (identifier + additional codes + optional data)
     */
    private T fillPayload(){
        if(getData() != null){
            for(byte singleData : getData()){
                rawDataBuffer.put(singleData);
            }
        }
        return ((T)rawDataBuffer.array());
    }

    /**
     * Bundles the data to send to the headset
     * for the write characteristic operation / request
     * @return the bundled data in a object
     */
    @Override
    public T serialize(){
        allocateBuffer();
        fillHeader();
        return fillPayload();
    }

    /**
     * Returns the optional data specific to the implemented class
     * @return the optional data specific to the implemented class
     */
    public abstract /*T*/byte[] getData();

}