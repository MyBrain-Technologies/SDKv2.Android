package engine;

import android.content.Context;
import android.support.annotation.NonNull;

import core.bluetooth.MbtBluetoothManager;
import core.eeg.MbtEEGManager;
import core.recordingsession.MbtRecordingSessionManager;
import core.serversync.MbtServerSyncManager;
import utils.AsyncUtils;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtClient {

    /**
     *     Used to save context
     */
    private Context mContext;

    /**
     *     Contains the client callbacks that will allow fluid communication between SDK and client app.
     */
    private MbtClientEvents mEvents;


    private final MbtBluetoothManager bluetoothManager;

    /**
     * The eeg manager that will manage the EEG data coming from the @bluetoothManager. It is responsible for
     * managing buffers size, conversion from raw packets to eeg values (voltages).
     */
    private final MbtEEGManager eegManager;

    /**
     * The recording session manager will manage all the recordings that are made during the lifetime of this instance.
     */
    private final MbtRecordingSessionManager recordingSessionManager;

    /**
     * The server sync manager will manage the communication with MBT server API.
     */
    private final MbtServerSyncManager serverSyncManager;

    private MbtClient(@NonNull Context context, @NonNull MbtClientEvents mbtClientEvents){
        //save client side objects in variables
        mEvents = mbtClientEvents;
        mContext = context;

        //init internal managers
        bluetoothManager = new MbtBluetoothManager(context);
        eegManager = new MbtEEGManager(context);
        recordingSessionManager = new MbtRecordingSessionManager(context);
        serverSyncManager = new MbtServerSyncManager(context);

    }

    public static MbtClient init(@NonNull Context context, @NonNull MbtClientEvents mbtClientEvents){
        return new MbtClient(context, mbtClientEvents);
    }

    public boolean connectBluetooth(){
        this.bluetoothManager.connect();
        return false;
    }

    public boolean disconnectBluetooth(){
        return false;
    }

}
