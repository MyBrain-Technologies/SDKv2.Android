package command;

import android.support.annotation.Keep;
import android.util.Log;

import engine.clientevents.BaseError;
import engine.clientevents.BaseErrorEvent;
import engine.clientevents.ConfigError;

/**
 * Created by Etienne on 08/02/2018.
 */

@Keep
public interface CommandInterface<E extends BaseError> extends BaseErrorEvent<E> {
    /**
     * A MbtRequest implementation object is a request
     * that is sent in suitables conditions to define when you extend this interface.
     * It means that the request can fail to be sent to a receiver (peripheral object/device/class).
     */
    interface MbtRequest {

        void onRequestSent(CommandInterface.MbtCommand request);
    }

    @Keep
    interface MbtResponse<N> {

        void onResponseReceived(MbtCommand request, N response);
    }

    @Keep
    interface CommandBaseErrorEvent {

        void onError(MbtCommand request, BaseError error, String additionalInfo);
    }

    /**
     * A SimpleCommandCallback implementation object is a request
     * that is sent in suitables conditions to define when you extend this interface.
     * It means that the request can fail to be sent to a receiver (peripheral object/device/class).
     * In case of failure, the onError callback is triggered to return the info associated to the failure
     */
    @Keep
    interface SimpleCommandCallback extends MbtRequest, CommandBaseErrorEvent {

    }

    /**
     * A CommandCallback implementation object is a request
     * that is sent in suitables conditions to define when you extend this interface.
     * It means that the request can fail to be sent to a receiver (peripheral object/device/class).
     * In case of failure, the onError callback is triggered to return the info associated to the failure
     * If it succeeded to be sent, the MbtRequest implementation object
     * receives in return a response sent by the receiver
     * @param <N> the response Object type returned by the receiver
     */
    @Keep
    interface CommandCallback<N> extends SimpleCommandCallback, MbtResponse<N> {

    }

    /**
     * Command abstact class that holds a callback used to
     * perform a request and notify the client when
     * the request has been sent
     * If the client is interested in getting the response associated to the request,
     * it also notify the client when this response is caught by the SDK.
     * A Command can be seen as a package that hold the request and its associated optional response.
     * @param <E> the expected Error Object
     */
    @Keep
    abstract class MbtCommand<E extends BaseError>  {

        private static final String TAG = MbtCommand.class.getName();

        CommandInterface.SimpleCommandCallback commandCallback;

        /**
         * Get the callback that returns the raw response of the headset to the SDK
         * @return the callback that returns the raw response of the headset to the SDK
         */
        public CommandInterface.SimpleCommandCallback getCommandCallback() {
            return commandCallback;
        }

        public void onError(E error, String additionalInfo) {
            Log.d(TAG, "Command not sent " + error.toString());
            if (commandCallback != null)
                commandCallback.onError(this, error, additionalInfo);
        }

        public void onRequestSent() {
            Log.d(TAG, "Command sent " + this);
            if (commandCallback != null)
                commandCallback.onRequestSent(this);
        }

        public void onResponseReceived(Object response) {
            Log.d(TAG, "Response received " + this);
            if (commandCallback != null && commandCallback instanceof CommandCallback)
                ((CommandCallback) commandCallback).onResponseReceived(this,response);
        }

        public boolean isResponseExpected() {
            return this.commandCallback instanceof CommandCallback;
        }

        /**
         * Init the command to send to the headset
         */
        protected void init() {
            init(true);
        }

        /**
         * Init the command to send to the headset
         * Init a command callback that handle responses if
         * @param responseExpected is true.
         * No response can be retrieved in the onResponseReceived callback if responseExcepted is false.
         */
        protected void init(boolean responseExpected) {
            if (commandCallback == null) {
                commandCallback = responseExpected ? //if a response is expected once the request is sent, we use a CommandCallback object (other object that extend MbtResponse)
                        new CommandCallback<Object>() {
                            @Override
                            public void onResponseReceived(MbtCommand request, Object response) { }
                            @Override
                            public void onError(MbtCommand request, BaseError error, String additionalInfo) { }
                            @Override
                            public void onRequestSent(MbtCommand request) { }
                        }

                        : new SimpleCommandCallback() { //if no response is expected once the request is sent, we use a SimpleCommandCallback object (other object that does not extend MbtResponse)
                    @Override
                    public void onError(MbtCommand request, BaseError error, String additionalInfo) { }
                    @Override
                    public void onRequestSent(MbtCommand request) { }
                };
            }

            if(!isValid())
                commandCallback.onError(this, ConfigError.ERROR_INVALID_PARAMS, getInvalidityError());
        }

        /**
         * Returns true if the client inputs
         * are valid for sending the command
         */
        public abstract boolean isValid();

        /**
         * Returns a String message that contain the reason of invalidity input.
         * Returns null if the input is valid (isValid returns true)
         * @return the reason of invalidity input as a String
         */
        public abstract String getInvalidityError();

        /**
         * Bundles the data to send to the headset
         * for the write characteristic operation / request
         * @return the bundled data in a object
         */
        public abstract Object serialize();

    }
}
