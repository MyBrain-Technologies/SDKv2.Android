package engine;

import android.content.Context;
import android.support.annotation.NonNull;

import config.MbtConfig;
import core.bluetooth.MbtBluetoothManager;
import core.eeg.MbtEEGManager;
import core.recordingsession.MbtRecordingSessionManager;
import core.serversync.MbtServerSyncManager;
import features.MbtFeatures;
import model.MbtDevice;
import model.MelomindDevice;
import model.VProDevice;

import static features.MbtFeatures.DEFAULT_GROUNDS;
import static features.MbtFeatures.DEFAULT_LOCATIONS;
import static features.MbtFeatures.DEFAULT_REFERENCES;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtClient {

    private static final String TAG = MbtClient.class.getName();
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

    private MbtClient(MbtClientBuilder builder){
        this.mContext=builder.mContext;
        this.mEvents=builder.mEvents;
        this.bluetoothManager=builder.bluetoothManager;
        this.eegManager=builder.eegManager;
        this.recordingSessionManager=builder.recordingSessionManager;
        this.serverSyncManager=builder.serverSyncManager;
    }

    /**
     *
     */
    public static void configure(){
        return;
    }

    public boolean connectBluetooth(){

        this.bluetoothManager.connect();
        return false;
    }

    public boolean disconnectBluetooth(){
        bluetoothManager.disconnect();

        return false;
    }

    public Context getmContext() {
        return mContext;
    }

    public MbtClientEvents getmEvents() {
        return mEvents;
    }

    public MbtBluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public MbtEEGManager getEegManager() {
        return eegManager;
    }

    public MbtRecordingSessionManager getRecordingSessionManager() {
        return recordingSessionManager;
    }

    public MbtServerSyncManager getServerSyncManager() {
        return serverSyncManager;
    }

    public static class MbtClientBuilder {
        private Context mContext;
        private MbtClientEvents mEvents;
        private MbtBluetoothManager bluetoothManager;
        private MbtEEGManager eegManager;
        private MbtRecordingSessionManager recordingSessionManager;
        private MbtServerSyncManager serverSyncManager;

        public MbtClientBuilder setContext(final Context context){
            this.mContext=context;
            return this;
        }

        public MbtClientBuilder setEvents(final MbtClientEvents events){
            this.mEvents = events;
            return this;
        }

        public MbtClientBuilder setBluetoothManager(final MbtBluetoothManager bluetoothManager){
            this.bluetoothManager = bluetoothManager;
            return this;
        }

        public MbtClientBuilder setEegManager(final MbtEEGManager eegManager){
            this.eegManager=eegManager;
            return this;
        }

        public MbtClientBuilder setRecordingSessionManager(final MbtRecordingSessionManager recordingSessionManager) {
            this.recordingSessionManager=recordingSessionManager;
            return this;
        }

        public MbtClientBuilder setServerSyncManager(final MbtServerSyncManager serverSyncManager){
            this.serverSyncManager = serverSyncManager;
            return this;
        }

        public MbtClient create(){
            return new MbtClient(this);
        }

    }

    /*public void scanDevicesForType(MbtConfig.ScannableDevices deviceType, long duration, ScanCallback scanCallback){

    }*/
    public void configureHeadset(){

    }

    public synchronized void readBattery(int period) {
        //this.gattController.startOrStopBatteryReader(true);
    }

    public void stopReadBattery(){
        //this.gattController.startOrStopBatteryReader(false);
    }

    public void startstream(boolean useQualities, final MbtClientEvents clientEvents){

    }

}
