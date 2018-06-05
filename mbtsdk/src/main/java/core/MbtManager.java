package core;

import android.content.Context;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import config.MbtConfig;
import core.bluetooth.BtProtocol;
import core.bluetooth.MbtBluetoothManager;
import core.eeg.MbtEEGManager;
import core.eeg.storage.MBTEEGPacket;
import core.recordingsession.MbtRecordingSessionManager;
import core.serversync.MbtServerSyncManager;
import eventbus.EventBusManager;
import eventbus.events.ClientReadyEEGEvent;
import features.MbtFeatures;

/**
 * MbtManager is responsible for managing communication between all the package managers
 *
 * @author Sophie ZECRI on 29/05/2018
 */
public final class MbtManager {

    private static final String TAG = MbtManager.class.getName();

    /**
     *     Used to save context
     */
    private Context mContext;
    /**
     *  The bluetooth manager will manage the communication between the headset and the application.
     */
    private MbtBluetoothManager mbtBluetoothManager;
    /**
     * The eeg manager that will manage the EEG data coming from the {@link MbtBluetoothManager}. It is responsible for
     * managing buffers size, conversion from raw packets to eeg values (voltages).
     */
    private MbtEEGManager mbtEEGManager;

    /**
     * The Event Bus Manager is responsible for registering the subscribers and posting events
     */
    private EventBusManager eventBusManager;

    /**
     * The recording session manager will manage all the recordings that are made during the lifetime of this instance.
     */
    private MbtRecordingSessionManager mbtRecordingSessionManager;
    /**
     * The server sync manager will manage the communication with MBT server API.
     */
    private MbtServerSyncManager mbtServerSyncManager;

    public MbtManager(Context context) {
        mbtBluetoothManager = new MbtBluetoothManager(context,this); //warning : very important to init mbtBluetootbManager before mbtEEGManager (if opposite : a NullPointerException is raised)
        mbtEEGManager = new MbtEEGManager(context,this);
        mbtServerSyncManager = new MbtServerSyncManager(context);
        mbtRecordingSessionManager = new MbtRecordingSessionManager(context);
        eventBusManager = new EventBusManager();
        eventBusManager.registerOrUnregister(true,this);

    }

    public BtProtocol getBluetoothProtocol(){
        return mbtBluetoothManager.getBtProtocol();
    }

    public MbtBluetoothManager getMbtBluetoothManager() {
        return mbtBluetoothManager;
    }

    public MbtEEGManager getMbtEEGManager() {
        return mbtEEGManager;
    }

    public MbtRecordingSessionManager getMbtRecordingSessionManager() {
        return mbtRecordingSessionManager;
    }

    public MbtServerSyncManager getMbtServerSyncManager() {
        return mbtServerSyncManager;
    }


    /**
     * onEvent is called by the Event Bus when a ClientReadyEEGEvent event is posted
     * This event is published by {@link core.eeg.MbtEEGManager}:
     * this manager handles EEG data acquired by the headset
     * Creates a new MBTEEGPacket instance when the raw buffer contains enough data
     * @param event contains data transmitted by the publisher : here it contains the converted EEG data matrix, the status, the number of acquisition channels and the sampling rate
     */
    @Subscribe
    public void onEvent(final ClientReadyEEGEvent event) { //warning : do not remove this attribute (consider unsused by the IDE, but actually used)
        Log.i(TAG, "event ClientReadyEEGEvent received" );
        notifyClientReadyEEG(event.getEegPackets(),event.getStatus(), MbtFeatures.getNbChannels(), MbtConfig.getSampleRate());
    }

    /**
     * Notifies the client that the raw EEG data have been converted and are ready to use
     */
    private void notifyClientReadyEEG(ArrayList<MBTEEGPacket> mbteegPackets, ArrayList<Float> status, int nbChannels, int sampleRate) {

    }
}