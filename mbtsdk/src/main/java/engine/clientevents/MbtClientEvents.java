package engine.clientevents;

import android.support.annotation.Keep;

import core.device.oad.OADEvent;

/**
 * Created by Etienne on 08/02/2018.
 */

@Keep
public interface MbtClientEvents<E extends BaseError> extends BaseErrorEvent<E> {

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
}
