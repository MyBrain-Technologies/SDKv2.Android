package engine;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import core.bluetooth.BtState;
import core.eeg.storage.MBTEEGPacket;
import core.oad.OADEvent;

/**
 * Created by Etienne on 08/02/2018.
 */

public interface MbtClientEvents extends ErrorEvent{

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

    interface MailboxEventListener{
        /**
         * Callback triggered after a new mailbox message has been received by the central from the peripheral
         * @param eventCode the Mailbox code associated with the message. See {@link core.bluetooth.lowenergy.MailboxEvents} for the list of all codes
         * @param eventValues the values sent by the peripheral.
         */
        void onMailBoxEvent (final byte eventCode, final Object eventValues);
    }

}
