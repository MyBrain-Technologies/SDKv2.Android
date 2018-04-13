package core;

import android.content.Context;

import core.bluetooth.BtProtocol;
import core.bluetooth.MbtBluetoothManager;
import core.eeg.MbtEEGManager;
import core.recordingsession.MbtRecordingSessionManager;
import core.serversync.MbtServerSyncManager;

public final class MbtManager {

    private static final String TAG = MbtManager.class.getName();

    private MbtEEGManager mbtEEGManager;
    private MbtBluetoothManager mbtBluetoothManager;
    private MbtServerSyncManager mbtServerSyncManager;
    private MbtRecordingSessionManager mbtRecordingSessionManager;

    public MbtManager(Context context) {
        mbtEEGManager = new MbtEEGManager(context);
        mbtBluetoothManager = new MbtBluetoothManager(context);
        mbtServerSyncManager = new MbtServerSyncManager(context);
        mbtRecordingSessionManager = new MbtRecordingSessionManager(context);
    }

    public BtProtocol getBluetoothProtocol(){
        return mbtBluetoothManager .getBtProtocol();
    }



}
