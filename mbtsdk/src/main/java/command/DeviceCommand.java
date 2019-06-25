package command;

import java.nio.ByteBuffer;

import engine.clientevents.BaseError;

/**
 * Mailbox command sent from the SDK to the headset
 * in order to configure a parameter,
 * or get values stored by the headset
 * or ask the headset to perform an action.
 * It provides a callback used to return a raw response sent by the headset to the SDK
 */
public abstract class DeviceCommand <T, U extends BaseError> extends CommandInterface.MbtCommand<U> {

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
    private /*T*/byte[] additionalCodes;

    /**
     * Buffer that hold the identifier code,
     * additional codes
     * and all the data specific to the implemented class
     */
    private ByteBuffer rawDataBuffer;

    DeviceCommand(byte mailboxCode) {
        this.identifierCode = mailboxCode;
    }

    DeviceCommand(byte mailboxCode, byte/*T*/... additionalCodes) {
        this.identifierCode = mailboxCode;
        this.additionalCodes = additionalCodes;
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



