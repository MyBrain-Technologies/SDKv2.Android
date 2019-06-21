package command;


import android.util.Log;

import java.nio.ByteBuffer;

import engine.clientevents.BaseError;
import engine.clientevents.BaseErrorEvent;
import engine.clientevents.ConfigError;
import engine.clientevents.MbtClientEvents;

/**
 * Mailbox command sent from the SDK to the headset
 * in order to configure a parameter,
 * or get values stored by the headset
 * or ask the headset to perform an action.
 * It provides a callback used to return a raw response sent by the headset to the SDK
 */
public abstract class BluetoothCommand <T,U extends BaseError> implements BaseErrorEvent<U> {

    private static final String TAG = BluetoothCommands.class.getName();

    /**
     * Callback that returns the raw response of the headset to the SDK
     * This raw response is a byte array that has be to converted to be readable.
     */
        MbtClientEvents.SimpleCommandCallback <BluetoothCommand, T> commandCallback;

    /**
     * Get the callback that returns the raw response of the headset to the SDK
     * @return the callback that returns the raw response of the headset to the SDK
     */
    public MbtClientEvents.SimpleCommandCallback <BluetoothCommand, T> getCommandCallback() {
        return commandCallback;
    }

    /**
     * Allocate a buffer that bundle all the data to send to the headset
     * for the write characteristic operation
     */
    private void allocateBuffer(){ }

    /**
     * Add the identifier code and the additional codes
     * to the raw data buffer to send to the headset
     */
    private void fillHeader(){ }

    /**
     * Add the optional data specific to the implemented class
     * to the raw data buffer to send to the headset
     * @return the complete buffer (identifier + additional codes + optional data)
     */
    private T fillPayload(){
        return null;
    }

    /**
     * Bundles the data to send to the headset
     * for the write characteristic operation / request
     * @return the bundled data in a byte array
     */
    public T serialize(){
        allocateBuffer();
        fillHeader();
        return fillPayload();
    }

    /**
     * Init the device command to send to the headset
     */
    public void init() {
        if(!isValid() && commandCallback != null)
            commandCallback.onError(this, ConfigError.ERROR_INVALID_PARAMS, "Invalid parameter : the input must not be null and/or empty.");
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
    public abstract T getData();

    @Override
    public void onError(U error, String additionnalInfo) {
        if (commandCallback != null)
            commandCallback.onError(this, error, additionnalInfo);
    }

    public void onRequestSent() {
        Log.d(TAG, "Device command sent " + this);
        if (commandCallback != null)
            commandCallback.onRequestSent(this);
    }

    public void onResponseReceived(byte[] response) {
        Log.d(TAG, "Device response received " + this);
        if (commandCallback != null && commandCallback instanceof MbtClientEvents.CommandCallback)
            ((MbtClientEvents.CommandCallback) commandCallback).onResponseReceived(this,response);
    }

    public boolean isResponseExpected() {
        return this.commandCallback instanceof MbtClientEvents.CommandCallback;
    }
}



