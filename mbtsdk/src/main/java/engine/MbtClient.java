package engine;

import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import core.MbtManager;
import core.bluetooth.MbtBluetoothManager;
import core.eeg.MbtEEGManager;
import core.eeg.signalprocessing.MBTCalibrationParameters;
import core.eeg.storage.MBTEEGPacket;
import core.recordingsession.MbtRecordingSessionManager;
import core.recordingsession.metadata.DeviceInfo;
import core.serversync.MbtServerSyncManager;
import features.MbtFeatures;
import features.ScannableDevices;
import model.MbtRecording;

import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static core.bluetooth.BtProtocol.BLUETOOTH_SPP;

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
    //Listeners declarations
    /*private MbtClientEvents.EegListener eegMelomindListener = null;
    private MbtClientEvents.BatteryListener batteryMelomindListener = null;
    private MbtClientEvents.StateListener bleStateMelomindListener = null;
    private MbtClientEvents.DeviceInfoListener deviceInfoListener = null;
    private MbtClientEvents.OADEventListener oadEventListener = null;
    private MbtClientEvents.MailboxEventListener mailboxEventListener = null;
    private MbtClientEvents.HeadsetStatusListener headsetStatusListener = null;*/

    /**
     * The MbtManager is responsible for managing all the package managers
     */
    private final MbtManager mbtManager;

    /**
     * Initializes the MbtClient instance
     * @param context the context of the single, global Application object of the current process.
     * @param mbtClientEvents object that contains callbacks for client events
     * @return the initialized MbtClient instance to the application
     */
    public static MbtClient init(@NonNull Context context, @NonNull MbtClientEvents mbtClientEvents){
        return new MbtClientBuilder()
                .setContext(context)
                .setMbtManager(new MbtManager(context))
                .setEvents(mbtClientEvents)
                .create();
    }

    /**
     * Constructor that use the MbtClientBuilder
     * @param builder object for creating the MbtClient instance with a setters syntax.
     */
    private MbtClient(MbtClientBuilder builder){
        this.mContext = builder.mContext;
        this.mEvents = builder.mEvents;
        mbtManager = builder.mbtManager;
    }

    public void scanDevicesForType(ScannableDevices deviceType, long duration, ScanCallback scanCallback){

    }

    public void connectBluetooth(@Nullable String name, StateListener stateListener){
        this.mbtManager.connectBluetooth(name, stateListener);
    }

    public boolean disconnectBluetooth(){
        //bluetoothManager.disconnect();
        this.mbtManager.disconnectBluetooth();
        return false;
    }


    public static void configureHeadset(){

    }


    public void readBattery(int periodInMillis, final DeviceInfoListener listener) {
        if(periodInMillis <= 0){
            mbtManager.readBluetooth(DeviceInfo.BATTERY, listener);
        }else{
            new Timer("batteryTimer").schedule(new TimerTask() {
                @Override
                public void run() {
                    mbtManager.readBluetooth(DeviceInfo.BATTERY, listener);
                }
            }, 2000, periodInMillis);
        }
        //this.gattController.startOrStopBatteryReader(true);
    }

    public void readFwVersion(@NonNull DeviceInfoListener listener){
        mbtManager.readBluetooth(DeviceInfo.FW_VERSION, listener);
    }

    public void readHwVersion(@NonNull DeviceInfoListener listener){
        mbtManager.readBluetooth(DeviceInfo.HW_VERSION, listener);
    }

    public void readSerialNumber(@NonNull DeviceInfoListener listener){
        mbtManager.readBluetooth(DeviceInfo.SERIAL_NUMBER, listener);
    }

    public void stopReadBattery(){
    }

    public void startstream(boolean useQualities, @NonNull final EegListener eegListener, @Nullable HeadsetStatusListener headsetStatusListener){
        mbtManager.startStream(useQualities, eegListener, headsetStatusListener);
    }

    public void stopStream(){
        mbtManager.stopStream();
    }

//    public void testEEGpackageClient(){
//        if (MbtFeatures.getBluetoothProtocol().equals(BLUETOOTH_LE)) {
//            getBluetoothManager().getMbtBluetoothLE().testAcquireDataRandomByte();
//        } else if (MbtFeatures.getBluetoothProtocol().equals(BLUETOOTH_SPP)){
//            getBluetoothManager().getMbtBluetoothSPP().testAcquireDataRandomByte();
//        }
//    }
//
//    /**
//     * Posts a BluetoothEEGEvent event to the bus so that MbtEEGManager can handle raw EEG data received
//     * @param data the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
//     */
//    public void handleDataAcquired(@NonNull final byte[] data){
//        getBluetoothManager().handleDataAcquired(data);
//    }
//
//    /**
//     * Initialize a new record session.
//     */
//    public void startRecord() {
//        getRecordingSessionManager().startRecord();
//    }
//
//    /**
//     * Stop current record and convert saved data into a new <b>MbtRecording</b> object
//     * @return the stopped recording data
//     */
//    public MbtRecording stopRecord() {
//        getRecordingSessionManager().stopRecord();
//        return getRecordingSessionManager().getCurrentRecording();
//    }
//
//    /**
//     * Saves current record into JSON file
//     */
//    public void saveRecordIntoJSON() {
//        getRecordingSessionManager().saveRecord();
//    }
//
//    /**
//     * Saves current record into JSON file
//     */
//    public void sendJSONtoServer() {
//        getRecordingSessionManager().sendJSONtoServer();
//    }
//
//    /**
//     * Computes the quality for each provided channels
//     * @param sampRate the number of value(s) inside each channel
//     * @param packetLength how long is a packet (time x samprate)
//     * @param channels the channel(s) to be computed
//     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
//     */
//    public float[] computeEEGSignalQuality(int sampRate, int packetLength, Float[] channels){
//        return getEEGManager().computeEEGSignalQuality(sampRate,packetLength,channels);
//    }
//
//    /**
//     * Computes the relaxation index using the provided <code>MBTEEGPacket</code>.
//     * For now, we admit there are only 2 channels for each packet
//     * @param sampRate the samprate of a channel (must be consistent)
//     * @param calibParams the calibration parameters previously performed
//     * @param packets the packets that contains EEG data, theirs status and qualities.
//     * @return the relaxation index
//     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
//     */
//    public float computeRelaxIndex(int sampRate, MBTCalibrationParameters calibParams, MBTEEGPacket... packets){
//        return getEEGManager().computeRelaxIndex(sampRate,calibParams,packets);
//    }
//
//    /**
//     * Computes the results of the previously done session
//     * @param threshold the level above which the relaxation indexes are considered in a relaxed state (under this threshold, they are considered not relaxed)
//     * @param relaxIndexValues the array that contains the relaxation indexes of the session
//     * @return the results of the previously done session
//     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
//     */
//    public HashMap<String, Float> computeStatisticsSNR(final float threshold, final Float[] relaxIndexValues){
//        return getEEGManager().computeStatisticsSNR(threshold, relaxIndexValues);
//    }
//
//    /**
//     * Converts the EEG raw data array into a user-readable matrix
//     * @param rawData the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
//     * @return the converted EEG data matrix that contains readable values for any user
//     */
//    public ArrayList<ArrayList<Float>> launchConversionToEEG(byte[] rawData){
//        return getEEGManager().launchConversionToEEG(rawData);
//    }

//    /**
//     * Gets the MbtEEGManager instance.
//     * The eeg manager that will manage the EEG data coming from the {@link MbtBluetoothManager}. It is responsible for
//     * managing buffers size, conversion from raw packets to eeg values (voltages).
//     */
//    private MbtEEGManager getEEGManager(){
//        return this.getMbtManager().getMbtEEGManager();
//    }
//
//    /**
//     * Gets the MbtBluetoothManager instance.
//     *  The bluetooth manager will manage the communication between the headset and the application.
//     */
//    private MbtBluetoothManager getBluetoothManager(){
//        return this.getMbtManager().getMbtBluetoothManager();
//    }
//
//    /**
//     * Gets the MbtRecordingSessionManager instance.
//     * The recording session manager will manage all the recordings that are made during the lifetime of this instance.
//     */
//    private MbtRecordingSessionManager getRecordingSessionManager(){
//        return this.getMbtManager().getMbtRecordingSessionManager();
//    }
//
//    /**
//     * Gets the MbtServerSyncManager instance.
//     * The server sync manager will manage the communication with MBT server API.
//     */
//    private MbtServerSyncManager getMbtServerSyncManager(){
//        return this.getMbtManager().getMbtServerSyncManager();
//    }

    /**
     * Gets the application context
     * @return the application context
     */
    public Context getmContext() {
        return mContext;
    }

    /**
     * Gets the callbacks for client events
     * @return object that contains the callbacks for client events
     */
    public MbtClientEvents getmEvents() {
        return mEvents;
    }

    /**
     * Gets the MbtManager instance.
     * MbtManager is responsible for managing all the package managers
     * @return the MbtManager instance.
     */
    public MbtManager getMbtManager() {
        return mbtManager;
    }

    public static class MbtClientBuilder {
        private Context mContext;
        private MbtClientEvents mEvents;
        private MbtManager mbtManager;

        public MbtClientBuilder setContext(final Context context){
            this.mContext=context;
            return this;
        }

        public MbtClientBuilder setEvents(final MbtClientEvents events){
            this.mEvents = events;
            return this;
        }

        public MbtClientBuilder setMbtManager(final MbtManager mbtManager){
            this.mbtManager = mbtManager;
            return this;
        }

        public MbtClient create(){
            return new MbtClient(this);
        }
    }
}
