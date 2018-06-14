package engine;

import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.lang.NullArgumentException;

import java.util.ArrayList;
import java.util.HashMap;

import config.MbtConfig;
import config.StreamConfig;
import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.bluetooth.MbtBluetoothManager;
import core.eeg.MbtEEGManager;
import core.eeg.signalprocessing.MBTCalibrationParameters;
import core.eeg.storage.MbtEEGPacket;
import core.eeg.storage.MbtRawEEG;
import core.recordingsession.MbtRecordingSessionManager;
import core.serversync.MbtServerSyncManager;
import exception.EmptyEegDataException;
import features.MbtFeatures;
import features.ScannableDevices;
import model.MbtDevice;
import model.MbtRecording;

import static config.MbtConfig.getSampleRate;
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
    private static Context mContext;

    //the client callbacks that will allow fluid communication between SDK and client app
    /**
     * The client listener to be notified upon arrival of a new list of EEG Packets.
     */
    private MbtClientEvents.EegListener eegListener = null;
    private MbtClientEvents.BatteryListener batteryListener = null;
    private MbtClientEvents.StateListener bleStateListener = null;
    private MbtClientEvents.DeviceInfoListener deviceInfoListener = null;
    private MbtClientEvents.OADEventListener oadEventListener = null;
    private MbtClientEvents.MailboxEventListener mailboxEventListener = null;
    private MbtClientEvents.HeadsetStatusListener headsetStatusListener = null;

    /**
     * The MbtManager is responsible for managing all the package managers
     */
    private final MbtManager mbtManager;

    private static MbtClient clientInstance;

    /**
     * Initializes the MbtClient instance
     * @param context the context of the single, global Application object of the current process.
     * @return the initialized MbtClient instance to the application
     */
    public static MbtClient init(@NonNull Context context){
        clientInstance = new MbtClientBuilder()
                .setContext(context)
                .setMbtManager(new MbtManager(context))
                .create();
        return clientInstance;
    }

    public static MbtClient getClientInstance(){
        if(clientInstance == null)
            clientInstance = init(mContext);
        return clientInstance;
    }

    /**
     * Constructor that use the MbtClientBuilder
     * @param builder object for creating the MbtClient instance with a setters syntax.
     */
    private MbtClient(MbtClientBuilder builder){
        mContext = builder.mContext;
        this.mbtManager = builder.mbtManager;
    }

    public boolean scanDevicesForType(ScannableDevices deviceType, long duration, ScanCallback scanCallback){

        return true; //todo return false if scan failed, true if success
    }

    public boolean connectBluetooth(){

       // this.bluetoothManager.connect();
        return true;
    }

    public boolean disconnectBluetooth(){
        //bluetoothManager.disconnect();

        return false;
    }


    public void configureHeadset(StreamConfig config){
    }


    public synchronized void readBattery(int period) {
        //this.gattController.startOrStopBatteryReader(true);
    }

    public void stopReadBattery(){
    }

    public void startStream(@Nullable final boolean useQualities){ //todo remove comments after tests

        //this.mbtManager.getMbtEEGManager().getDataAcquisition().reconfigureBuffers(getSampleRate(),getDeviceInternalConfig().getNbPackets(),getDeviceInternalConfig().getStatusBytes());

        MbtClientEvents.EegListener eegCallback = new MbtClientEvents.EegListener() { //if a new list of EEG packets is received, the client is notified
            @Override
            public void onNewPackets(MbtEEGPacket eegPackets) {
                if (MbtClient.this.eegListener != null)
                    MbtClient.this.eegListener.onNewPackets(eegPackets); //client callback for notifying the Activity

            }

            @Override
            public void onError(@NonNull Exception exception) {
                exception.printStackTrace();
                if (MbtClient.this.eegListener != null)
                    MbtClient.this.eegListener.onError(exception); //client callback for notifying the Activity that an error occured
            }
        };
        mbtManager.setEegCallback(eegCallback);
    }

    public void stopStream(){
        mbtManager.setEegCallback(null);
    }

    /**
     * Posts a BluetoothEEGEvent event to the bus so that MbtEEGManager can handle raw EEG data received
     * @param data the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     */
    public void handleDataAcquired(@NonNull final byte[] data){
        getBluetoothManager().handleDataAcquired(data);
    }

    /**
     * Initialize a new record session.
     */
    public void startRecord() {
        getRecordingSessionManager().startRecord();
    }

    /**
     * Stop current record and convert saved data into a new <b>MbtRecording</b> object
     * @return the stopped recording data
     */
    public MbtRecording stopRecord() {
        getRecordingSessionManager().stopRecord();
        return getRecordingSessionManager().getCurrentRecording();
    }

    /**
     * Saves current record into JSON file
     */
    public void saveRecordIntoJSON() {
        getRecordingSessionManager().saveRecord();
    }

    /**
     * Saves current record into JSON file
     */
    public void sendJSONtoServer() {
        getRecordingSessionManager().sendJSONtoServer();
    }

    /**
     * Computes the quality for each provided EEG acquisition channels.
     * The Melomind headset has 2 channels and the VPRO headset has 9 channels.
     * @param consolidatedEEG the user-readable EEG data matrix
     * @param packetLength how long is a packet (time x samprate)
     * @return an array that contains the quality of each EEG acquisition channels.
     * This array contains 2 qualities (items) if the headset used is MELOMIND.
     * This array contains 9 qualities (items) if the headset used is VPRO.
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public ArrayList<Float> computeEEGSignalQuality(ArrayList<ArrayList<Float>> consolidatedEEG, int packetLength){
        return getEEGManager().computeEEGSignalQuality(consolidatedEEG,packetLength);
    }

    /**
     * Computes the relaxation index using the provided <code>MbtEEGPacket</code>.
     * For now, we admit there are only 2 channels for each packet
     * @param sampRate the samprate of a channel (must be consistent)
     * @param calibParams the calibration parameters previously performed
     * @param packets the packets that contains EEG data, theirs status and qualities.
     * @return the relaxation index
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public float computeRelaxIndex(int sampRate, MBTCalibrationParameters calibParams, MbtEEGPacket... packets){
        return getEEGManager().computeRelaxIndex(sampRate,calibParams,packets);
    }

    /**
     * Computes the results of the previously done session
     * @param threshold the level above which the relaxation indexes are considered in a relaxed state (under this threshold, they are considered not relaxed)
     * @param relaxIndexValues the array that contains the relaxation indexes of the session
     * @return the results of the previously done session
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public HashMap<String, Float> computeStatisticsSNR(final float threshold, final Float[] relaxIndexValues){
        return getEEGManager().computeStatisticsSNR(threshold, relaxIndexValues);
    }

    /**
     * Converts the EEG raw data array into a user-readable matrix
     * @param rawData the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     * @return the converted EEG data matrix that contains readable values for any user
     */
    public ArrayList<ArrayList<Float>> launchConversionToEEG(ArrayList<MbtRawEEG> rawData){
        return getEEGManager().launchConversionToEEG(rawData);
    }

    /**
     * Callback called when the EEG packets buffer is full in order to transmit the user-readable EEG data to the client.
     * Notifies the client that the raw EEG data have been converted and are ready to use.
     */
     public void notifyClientReadyEEG(final MbtEEGPacket eegPackets) {
         if (this.eegListener != null)
             this.eegListener.onNewPackets(eegPackets);
    }

    /**
     * Cancels the current scanning to stop looking for a Mbt Bluetooth device to connect
     * @return false if the scan has been correctly cancelled, true otherwise.
     */
    public void cancelScanning(){
         //todo cancel
    }

    /**
     * Gets the MbtEEGManager instance.
     * The eeg manager that will manage the EEG data coming from the {@link MbtBluetoothManager}. It is responsible for
     * managing buffers size, conversion from raw packets to eeg values (voltages).
     */
    private MbtEEGManager getEEGManager(){
        return this.getMbtManager().getMbtEEGManager();
    }

    /**
     * Gets the MbtBluetoothManager instance.
     *  The bluetooth manager will manage the communication between the headset and the application.
     */
    private MbtBluetoothManager getBluetoothManager(){
        return this.getMbtManager().getMbtBluetoothManager();
    }

    /**
     * Gets the MbtRecordingSessionManager instance.
     * The recording session manager will manage all the recordings that are made during the lifetime of this instance.
     */
    private MbtRecordingSessionManager getRecordingSessionManager(){
        return this.getMbtManager().getMbtRecordingSessionManager();
    }

    /**
     * Gets the MbtServerSyncManager instance.
     * The server sync manager will manage the communication with MBT server API.
     */
    private MbtServerSyncManager getMbtServerSyncManager(){
        return this.getMbtManager().getMbtServerSyncManager();
    }

    /**
     * Gets the application context
     * @return the application context
     */
    public Context getmContext() {
        return mContext;
    }

    /**
     * Gets the EEG callbacks for a EEG client events
     * @return the callbacks for a EEG client events
     */
    public MbtClientEvents.EegListener getEegListener() {
        return eegListener;
    }

    /**
     * Gets the MbtManager instance.
     * MbtManager is responsible for managing all the package managers
     * @return the MbtManager instance.
     */
    public MbtManager getMbtManager() {
        return mbtManager;
    }

    private MbtDevice.InternalConfig getDeviceInternalConfig() {
        MbtDevice.InternalConfig internalConfig = null;
        switch (MbtConfig.getScannableDevices()) {
            case MELOMIND:
                internalConfig = getBluetoothManager().getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_LE) ? getBluetoothManager().getMbtBluetoothLE().getMelomindDevice().getInternalConfig() : getBluetoothManager().getMbtBluetoothSPP().getMelomindDevice().getInternalConfig();
                break;
            case VPRO:
                internalConfig = getBluetoothManager().getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_LE) ? getBluetoothManager().getMbtBluetoothLE().getVproDevice().getInternalConfig() : getBluetoothManager().getMbtBluetoothSPP().getVproDevice().getInternalConfig();
                break;
        }
        return internalConfig;
    }

    public void testEEGpackageClientBLE() {
        this.mbtManager.getMbtBluetoothManager().getMbtBluetoothLE().testAcquireDataRandomByte();
    }

    public static class MbtClientBuilder {
        private Context mContext;
        private MbtClientEvents.EegListener eegListener;
        private MbtManager mbtManager;

        public MbtClientBuilder setContext(final Context context){
            this.mContext=context;
            return this;
        }

        public MbtClientBuilder setEegListener(final MbtClientEvents.EegListener eegListener){
            this.eegListener = eegListener;
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
