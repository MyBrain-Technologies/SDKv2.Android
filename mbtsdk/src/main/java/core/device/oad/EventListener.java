package core.device.oad;

import android.support.annotation.Keep;
import android.support.annotation.Nullable;

import engine.clientevents.BaseError;
import engine.clientevents.BaseErrorEvent;

public class EventListener{

    /**
     * Listener used to receive a notification when an OAD event occurs
     * @param <U> Error triggered if something went wrong during the firmware update
     */
    @Keep
    public interface OADEventListener<U extends BaseError> extends BaseErrorEvent<U> {

        /**
         * Callback triggered when a message/response of the headset device
         * related to the OAD update process
         * is received by the Bluetooth unit
         * @param object is an optional additional value that is associated to an event
         */
        void onOADEvent(OADEvent oadEvent, @Nullable Object object);
    }


}