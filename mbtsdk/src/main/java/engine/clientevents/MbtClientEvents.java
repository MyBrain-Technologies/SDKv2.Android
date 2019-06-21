package engine.clientevents;

import android.support.annotation.Keep;

import core.oad.OADEvent;

/**
 * Created by Etienne on 08/02/2018.
 */

@Keep
public interface MbtClientEvents extends BaseErrorEvent {

    interface OADEventListener {
        /**
         * Callback triggered when a new step of the OAD (Over the Air Download) has completed.
         * @param event the event code associated with the step. See {@link OADEvent} for the list of events
         * @param value the value associated with the step. Might indicate a progress, a success or a failure
         */
        void onOadEvent(OADEvent event, int value);
        void onDeviceReady(boolean ready);
        void onProgressUpdate(int progress);
        void onPacketTransferComplete(boolean state);
        void onOADComplete(boolean result);
    }

    /**
     * A MbtRequest implementation object is a request
     * that is sent in suitables conditions to define when you extend this interface.
     * It means that the request can fail to be sent to a receiver (peripheral object/device/class).
     * @param <T> the request type to send to the receiver
     */
    interface MbtRequest<T> {

        void onRequestSent(T request);
    }

    /**
     * A SimpleCommandCallback implementation object is a request
     * that is sent in suitables conditions to define when you extend this interface.
     * It means that the request can fail to be sent to a receiver (peripheral object/device/class).
     * In case of failure, the onError callback is triggered to return the info associated to the failure
     * @param <T> the request type to send to the receiver
     */
    @Keep
    interface SimpleCommandCallback<T,U> extends MbtClientEvents.MbtRequest<T> {

        void onError(T request, BaseError error, String additionnalInfo);
    }

    /**
     * A CommandCallback implementation object is a request
     * that is sent in suitables conditions to define when you extend this interface.
     * It means that the request can fail to be sent to a receiver (peripheral object/device/class).
     * In case of failure, the onError callback is triggered to return the info associated to the failure
     * If it succeeded to be sent, the MbtRequest implementation object
     * receives in return a response sent by the receiver
     * @param <T> the request type to send to the receiver
     * @param <U> the response type returned by the receiver
     */
    @Keep
    interface CommandCallback<T,U> extends SimpleCommandCallback<T,U> {

        void onResponseReceived(T request, U response);
    }

}
