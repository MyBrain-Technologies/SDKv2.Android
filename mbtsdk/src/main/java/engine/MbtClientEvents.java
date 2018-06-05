package engine;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import core.bluetooth.BtState;
import core.eeg.storage.MBTEEGPacket;
import core.oad.OADEvent;

/**
 * Created by Etienne on 08/02/2018.
 */

public interface MbtClientEvents {

    interface StateListener {
        /**
         * Callback indicating the current state of the bluetooth communication
         * See {@link BtState} for all possible states
         */
        void onStateChanged(@NonNull final BtState newState);
    }

    @Keep
    interface EegListener {
        /**
         * Callback triggered when the input eeg buffer is full, ie when raw buffer contains enough data to compute a new MBTEEGPacket.
         * This event is triggered from the MbtDataAcquisition class when (bufPos >= BLE_RAW_DATA_BUFFER_SIZE)
         * Warning, this callb is in worker thread. you need to call runOnUiThread to change views if necessary
         * @param mbteegPackets the eeg data (Channels, EEG values)
         * @param nbChannels the number of EEG acquisition channels
         * @param nbSamples //TODO remove this input
         * @param sampleRate //TODO might be unnecessary
         */
        //void onNewSamples(final ArrayList<ArrayList<Float>> matrix, @Nullable final ArrayList<Float> status, final int nbChannels, final int nbSamples, final int sampleRate);
        void onNewPackets(final MBTEEGPacket mbteegPackets, final int nbChannels, final int nbSamples, final int sampleRate);
        void onError(/*TODO error cause*/);
    }

    @Keep
    interface BatteryListener {
        void onBatteryChanged(final int level);
    }

    interface HeadsetStatusListener {
        /**
         * Callback indicating that the headset has entered in a new saturation state
         * @param newState the new measured state.
         */
        void onSaturationStateChanged(final int newState);

        /**
         * Callback indicating that the headset has measured a new dc offset
         * @param dcOffset the new measured offset.
         */
        void onNewDCOffsetMeasured(final int dcOffset);
    }

    @Keep
    interface DeviceInfoListener {
        void onFwVersionReceived(String fwVersion);
        void onHwVersionReceived(String hwVersion);
        void onSerialNumberReceived(String serialNumber);
    }

    @Keep
    interface BandwidthListener {
        /**
         * Triggers every time data is received in order to compute the time it took to receive it
         * @param bandwidth the speed in bytes per second
         */
        void onNewSpeed(final double bandwidth);
    }

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
