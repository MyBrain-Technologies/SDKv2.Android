package command;


import engine.SimpleRequestCallback;

/**
 * Mailbox command sent from the SDK to the headset
 * in order to configure a parameter,
 * or get values stored by the headset
 * or ask the headset to perform an action.
 * It provides a callback used to return a raw response sent by the headset to the SDK
 */
public abstract class DeviceCommand {

    /**
     * Callback that returns the raw response of the headset to the SDK
     * This raw response is a byte array that has be to converted to be readable.
     */
    SimpleRequestCallback<byte[]> responseCallback;

    /**
     * Get the callback that returns the raw response of the headset to the SDK
     * @return the callback that returns the raw response of the headset to the SDK
     */
    public SimpleRequestCallback<byte[]> getResponseCallback() {
        return responseCallback;
    }

    }



