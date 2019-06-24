package command;

import android.support.annotation.Keep;
import android.util.Log;

import core.oad.OADEvent;
import engine.clientevents.BaseError;
import engine.clientevents.BaseErrorEvent;
import engine.clientevents.ConfigError;

/**
 * Created by Etienne on 08/02/2018.
 */

@Keep
public interface CommandInterface<E extends BaseError> extends BaseErrorEvent<E> {
    /**
     * A MbtRequestable implementation object is a request
     * that is sent in suitables conditions to define when you extend this interface.
     * It means that the request can fail to be sent to a receiver (peripheral object/device/class).
     */
    interface MbtRequestable {

        void onRequestSent(CommandInterface.MbtCommand request);
    }

    /**
     * A SimpleCommandCallback implementation object is a request
     * that is sent in suitables conditions to define when you extend this interface.
     * It means that the request can fail to be sent to a receiver (peripheral object/device/class).
     * In case of failure, the onError callback is triggered to return the info associated to the failure
     */
    @Keep
    interface SimpleCommandCallback extends MbtRequestable {

        void onError(MbtCommand request, BaseError error, String additionnalInfo);
    }

    /**
     * A CommandCallback implementation object is a request
     * that is sent in suitables conditions to define when you extend this interface.
     * It means that the request can fail to be sent to a receiver (peripheral object/device/class).
     * In case of failure, the onError callback is triggered to return the info associated to the failure
     * If it succeeded to be sent, the MbtRequestable implementation object
     * receives in return a response sent by the receiver
     * @param <U> the response Object type returned by the receiver
     */
    @Keep
    interface CommandCallback<U> extends SimpleCommandCallback {

        void onResponseReceived(MbtCommand request, U response);
    }

    /**
     * Command abstact class that holds a callback used to
     * perform a request and notify the client when
     * the request has been sent
     * If the client is interested in getting the response associated to the request,
     * it also notify the client when this response is caught by the SDK
     * @param <U> the response Object type
     * @param <E> the expected Error Object
     */
    @Keep
    abstract class MbtCommand< U, E extends BaseError>  {

        private static final String TAG = MbtCommand.class.getName();

        CommandInterface.SimpleCommandCallback commandCallback;

        /**
         * Get the callback that returns the raw response of the headset to the SDK
         * @return the callback that returns the raw response of the headset to the SDK
         */
        public CommandInterface.SimpleCommandCallback getCommandCallback() {
            return commandCallback;
        }

        public void onError(E error, String additionnalInfo) {
            if (commandCallback != null)
                commandCallback.onError(this, error, additionnalInfo);
        }

        public void onRequestSent() {
            Log.d(TAG, "Device command sent " + this);
            if (commandCallback != null)
                commandCallback.onRequestSent(this);
        }

        public void onResponseReceived(U response) {
            Log.d(TAG, "Device response received " + this);
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
            if (commandCallback == null)
                commandCallback = new CommandCallback<U>() {
                    @Override
                    public void onResponseReceived(MbtCommand request, U response) { }
                    @Override
                    public void onError(MbtCommand request, BaseError error, String additionnalInfo) { }
                    @Override
                    public void onRequestSent(MbtCommand request) { }
                };

            if(!isValid() && commandCallback != null)
                commandCallback.onError(this, ConfigError.ERROR_INVALID_PARAMS, "Invalid parameter : the input must not be null and/or empty.");
        }

        /**
         * Returns true if the client inputs
         * are valid for sending the command
         */
        public abstract boolean isValid();

        /**
         * Bundles the data to send to the headset
         * for the write characteristic operation / request
         * @return the bundled data in a object
         */
        public abstract U serialize();

    }
}
