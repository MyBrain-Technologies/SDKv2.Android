package core;

import android.content.Context;

import core.bluetooth.BtProtocol;
import core.bluetooth.MbtBluetoothManager;
import core.eeg.MbtEEGManager;
import core.recordingsession.MbtRecordingSessionManager;
import core.serversync.MbtServerSyncManager;

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
}