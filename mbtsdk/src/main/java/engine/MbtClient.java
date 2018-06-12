package engine;

import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import config.DeviceConfig;
import core.MbtManager;
import core.recordingsession.metadata.DeviceInfo;
import core.eeg.storage.MbtEEGPacket;
import features.ScannableDevices;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtClient {

    private static final String TAG = MbtClient.class.getName();
    /**
     *     Used to save context
     */
    private Context mContext;

    //the client callbacks that will allow fluid communication between SDK and client app
    /**
     * The client listener to be notified upon arrival of a new list of EEG Packets.
     */

    /**
     * The MbtManager is responsible for managing all the package managers
     */
    private final MbtManager mbtManager;

    /**
     * Initializes the MbtClient instance
     * @param context the context of the single, global Application object of the current process.
     * @return the initialized MbtClient instance to the application
     */
    public static MbtClient init(@NonNull Context context){
        return new MbtClientBuilder()
                .setContext(context)
                .setMbtManager(new MbtManager(context))
                .create();
    }

    /**
     * Constructor that use the MbtClientBuilder
     * @param builder object for creating the MbtClient instance with a setters syntax.
     */
    private MbtClient(MbtClientBuilder builder){
        this.mContext = builder.mContext;
        this.mbtManager = builder.mbtManager;
    }

    public void scanDevicesForType(ScannableDevices deviceType, long duration, ScanCallback scanCallback){
        //TODO
    }

    public void connectBluetooth(@Nullable String name, StateListener stateListener){
        this.mbtManager.connectBluetooth(name, stateListener);
    }

    public boolean disconnectBluetooth(){
        this.mbtManager.disconnectBluetooth();
        return false;
    }


    public void configureHeadset(DeviceConfig config){
        //TODO
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
            },0, periodInMillis);
        }
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

    public void startstream(boolean useQualities, @NonNull final EegListener eegListener, @Nullable DeviceStatusListener deviceStatusListener){
        mbtManager.startStream(useQualities, eegListener, deviceStatusListener);
    }


    public void stopStream(){
        mbtManager.stopStream();
    }

    public void notifyClientReadyEEG(final ArrayList<MbtEEGPacket> eegPackets) {
//        if (this.eegListener != null)
//            this.eegListener.onNewPackets((ArrayList<MbtEEGPacket>) eegPackets);
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



//    private MbtDevice.InternalConfig getDeviceInternalConfig() {
//        MbtDevice.InternalConfig internalConfig = null;
//        switch (MbtConfig.getScannableDevices()) {
//            case MELOMIND:
//                internalConfig = getBluetoothManager().getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_LE) ? getBluetoothManager().getMbtBluetoothLE().getMelomindDevice().getInternalConfig() : getBluetoothManager().getMbtBluetoothSPP().getMelomindDevice().getInternalConfig();
//                break;
//            case VPRO:
//                internalConfig = getBluetoothManager().getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_LE) ? getBluetoothManager().getMbtBluetoothLE().getVproDevice().getInternalConfig() : getBluetoothManager().getMbtBluetoothSPP().getVproDevice().getInternalConfig();
//                break;
//        }
//        return internalConfig;
//    }

//    public void testEEGpackageClientBLE() {
//        this.mbtManager.getMbtBluetoothManager().getMbtBluetoothLE().testAcquireDataRandomByte();
//    }

    public static class MbtClientBuilder {
        private Context mContext;
        private MbtManager mbtManager;

        public MbtClientBuilder setContext(final Context context){
            this.mContext=context;
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
