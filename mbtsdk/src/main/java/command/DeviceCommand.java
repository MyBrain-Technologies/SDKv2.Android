package command;


import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;

import engine.SimpleRequestCallback;
import engine.clientevents.BaseError;

/**
 * Mailbox command sent from the SDK to the headset
 * in order to configure a parameter,
 * or get values stored by the headset
 * or ask the headset to perform an action.
 * It provides a callback used to return a raw response sent by the headset to the SDK
 */
public abstract class DeviceCommand {

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
    private ByteBuffer rawDataArray;

    /**
     * Callback that returns the raw response of the headset to the SDK
     * This raw response is a byte array that has be to converted to be readable.
     */
    SimpleRequestCallback<byte[]> responseCallback;


    DeviceCommand() {
    }

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
     * @param dataToBuffer are optional data specific to the implemented class
     */
    private void allocateBuffer(@Nullable byte... dataToBuffer){
        rawDataArray = null;
        int bufferSize = 1;
        if(additionalCodes != null)
            bufferSize += additionalCodes.length;
        if(dataToBuffer != null)
            bufferSize += dataToBuffer.length;

        rawDataArray = ByteBuffer.allocate(bufferSize);
    }

    /**
     * Add the identifier code and the additional codes
     * to the raw data buffer to send to the headset
     */
    private void setHeader(){
        rawDataArray.put(identifierCode);
        if(additionalCodes != null)
            for(byte singleCode : additionalCodes){
                rawDataArray.put(singleCode);
            }
    }

    /**
     * Add the optional data specific to the implemented class
     * to the raw data buffer to send to the headset
     */
    private void fillBuffer(byte[] data ){
        if(data != null){
            for(byte singleData : data){
                rawDataArray.put(singleData);
            }
        }
    }

    /**
     * Bundles the data to send to the headset
     * for the write characteristic operation
     * @param data are the optional data specific to the implemented class
     * @return the bundled data in a byte array
     */
    public byte[] serialize(@Nullable byte... data){
        allocateBuffer(data);
        setHeader();
        fillBuffer(data);
        return rawDataArray.array();
    }

//    /**
//     *
//     * /!\ Method to override
//     */
//    public byte[] serialize(@Nullable byte... data){
//        allocateBuffer(data);
//        rawDataArray.put(identifierCode);
//        if(additionalCodes != null)
//            for(byte singleCode : additionalCodes){
//                rawDataArray.put(singleCode);
//            }
//        if(data != null){
//            for(byte singleData : data){
//                rawDataArray.put(singleData);
//            }
//        }
//        return rawDataArray.array();
//    }


    /**
     * Return the data sent by the headset
     * in response to a characteristic writing operation
     */
    public byte[] deserializeResponse(BluetoothGattCharacteristic characteristic){
        return Arrays.copyOfRange(characteristic.getValue(), 1 , characteristic.getValue().length);
    }


    /**
     * Init the device command to send to the headset
     */
    public abstract void init();

    /**
     * Returns true if the client inputs
     * are valid for sending the command
     */
    public abstract boolean isValid();

    /**
     * Returns an error code if
     * something went wrong while sending the command
     */
    public abstract  BaseError onError();

}



