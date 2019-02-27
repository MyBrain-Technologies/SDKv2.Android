package engine;

import android.bluetooth.le.ScanCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;


import config.MbtConfig;
import core.MbtManager;
import core.device.model.DeviceInfo;
import core.device.model.MbtDevice;
import core.eeg.storage.MbtEEGPacket;
import engine.clientevents.BaseError;
import engine.clientevents.ConfigError;
import engine.clientevents.ConnectionStateListener;
import engine.clientevents.ConnectionStateReceiver;
import engine.clientevents.DeviceInfoListener;
import engine.clientevents.EegListener;
import engine.clientevents.HeadsetDeviceError;
import features.MbtFeatures;
import features.MbtDeviceType;

/**
 * Created by Etienne on 08/02/2018.
 */

@Keep
public final class MbtClient {

    private static final String TAG = MbtClient.class.getName();

    /**
     * The MbtManager is responsible for managing all the package managers
     */
    private final MbtManager mbtManager;

    private static MbtClient clientInstance;

    /**
     * Initializes the MbtClient instance
     * @param context the context of the single, global Application object of the current process.
     * @return the initialized MbtClient instance to the application
     * @throws IllegalStateException if client has already been init.
     */
    public static MbtClient init(@NonNull Context context){
        if(clientInstance != null)
            throw new IllegalStateException("Client has already been init. You should call getClientInstance() instead");

        clientInstance = new MbtClientBuilder()
                .setContext(context)
                .setMbtManager(new MbtManager(context))
                .create();
        return clientInstance;
    }

    /**
     * @return The current instance of the client. Init() must have been called first
     * @throws NullPointerException if there is no instance created.
     */
    public static MbtClient getClientInstance(){
        if(clientInstance == null)
            throw new NullPointerException("Client instance has not been initialized. Please call init() method first.");
        return clientInstance;
    }

    /**
     * Constructor that use the MbtClientBuilder
     * @param builder object for creating the MbtClient instance with a setters syntax.
     */
    private MbtClient(MbtClientBuilder builder){
        //mContext = builder.mContext;
        this.mbtManager = builder.mbtManager;
    }

    public void scanDevicesForType(MbtDeviceType deviceType, long duration, ScanCallback scanCallback){
        //TODO
    }

    /**
     * Call this method to establish a bluetooth connection with a remote device.
     * It is possible to connect either a Melomind or a VPro device through this method. You need to specify
     * the device type in the {@link ConnectionConfig} input instance with the {@link MbtDeviceType} type.
     * <p>If the parameters are invalid, the method returns immediately and {@link ConnectionStateReceiver#onError(engine.clientevents.BaseError, String)} method is called</p>
     *
     * @param config the {@link ConnectionConfig} instance that holds all the configuration parameters inside.
     */
    @SuppressWarnings("unchecked")
    public void connectBluetooth(@NonNull ConnectionConfig config){
        MbtConfig.setScannableDevices(config.getDeviceType());
        MbtConfig.setBluetoothScanTimeout(config.getMaxScanDuration());
        MbtConfig.setConnectAudioIfDeviceCompatible(config.useAudio());

        if(config.getMaxScanDuration() < MbtFeatures.MIN_SCAN_DURATION){
            config.getConnectionStateListener().onError(ConfigError.ERROR_INVALID_PARAMS,ConfigError.SCANNING_MINIMUM_DURATION);
            return;
        }

        if(config.getDeviceType() == MbtDeviceType.VPRO){
            config.getConnectionStateListener().onError(HeadsetDeviceError.ERROR_VPRO_INCOMPATIBLE,null);
            return;
        }

        this.mbtManager.connectBluetooth(config.getConnectionStateListener(), config.getDeviceName());
    }

    /**
     * Call this method to attempt to disconnect from the currently connected bluetooth device.
     */
    public void disconnectBluetooth(){
        this.mbtManager.disconnectBluetooth(false);
    }

    /**
     * Call this method in order to get the battery level from the remote bluetooth device.
     * The values returned are the following:
     * <p>100%</p>
     * <p>85%</p>
     * <p>65%</p>
     * <p>50%</p>
     * <p>30%</p>
     * <p>15%</p>
     * <p>0%</p>
     */
    public void readBattery(DeviceInfoListener listener) {
        mbtManager.readBluetooth(DeviceInfo.BATTERY, listener);
    }

    /**
     * Initiates an EEG streaming ie, send a message to the headset to deliver EEG to the application.
     *
     * <p>You can customize some parameters in the {@link StreamConfig}class.</p>
     *
     * <p>If the parameters are incorrect, the function returns directly and the {@link EegListener#onError(engine.clientevents.BaseError,String)} method is called</p>
     * If something wrong happens during the operation, {@link EegListener#onError(engine.clientevents.BaseError,String)} method is called.
     *
     * <p>If everything went well, the EEG will be available in the {@link EegListener#onNewPackets(MbtEEGPacket)} callback.</p>
     *
     * @param streamConfig the configuration to pass to the streaming.
     */
    @SuppressWarnings("unchecked")
    public void startStream(@NonNull StreamConfig streamConfig){
        if(!streamConfig.isConfigCorrect())
            streamConfig.getEegListener().onError(ConfigError.ERROR_INVALID_PARAMS, streamConfig.shouldComputeQualities() ?
                    ConfigError.NOTIFICATION_PERIOD_RANGE_QUALITIES : ConfigError.NOTIFICATION_PERIOD_RANGE);
        else
            MbtConfig.setEegBufferLengthClientNotif((int)((streamConfig.getNotificationPeriod()* MbtFeatures.DEFAULT_SAMPLE_RATE)/1000));

        mbtManager.startStream(streamConfig.shouldComputeQualities(), streamConfig.getEegListener(), streamConfig.getDeviceStatusListener());
    }


    /**
     * Stops the currently running eeg stream. This stops bluetooth acquisition and
     * reinit all internal buffering system.
     */
    public void stopStream(){
        mbtManager.stopStream();
    }


    /**
     * Stops a pending connection process. If successful,
     * the new state {@link core.bluetooth.BtState#CONNECTION_INTERRUPTED} is sent to the user in the
     * {@link ConnectionStateReceiver#onReceive(Context, Intent)} callback.
     *
     * <p>If the device is already connected, it simply disconnects the device.</p>
     */
    public void cancelConnection() {
        this.mbtManager.disconnectBluetooth(true);
    }

    /**
     * Sets the {@link BroadcastReceiver} to the connectionStateReceiver value
     * @param connectionStateListener the new {@link BroadcastReceiver}. Set it to null if you want to reset the listener
     */
    public void setConnectionStateListener(ConnectionStateListener<BaseError> connectionStateListener){
        this.mbtManager.setConnectionStateListener(connectionStateListener);
    }

    /**
     * Sets the {@link EegListener} to the connectionStateListener value
     * @param eegListener the new {@link EegListener}. Set it to null if you want to reset the listener
     */
    public void setEEGListener(EegListener<BaseError> eegListener){
        this.mbtManager.setEEGListener(eegListener);
    }


    /**
     * Perform a request to retrieve the currently connected device. The operation is done in the background
     * and returned in the main thread with the associated callback.
     * @param callback the callback which will hold the {@link MbtDevice} instance. Careful,
     *                 the instance can be null, if no device is currently connected.
     */
    public void requestCurrentConnectedDevice(SimpleRequestCallback<MbtDevice> callback){
        mbtManager.requestCurrentConnectedDevice(callback);
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


    @Keep
    private static class MbtClientBuilder {
        private Context mContext;
        private MbtManager mbtManager;

        @NonNull
        public MbtClientBuilder setContext(final Context context){
            this.mContext=context;
            return this;
        }


        @NonNull
        public MbtClientBuilder setMbtManager(final MbtManager mbtManager){
            this.mbtManager = mbtManager;
            return this;
        }

        @NonNull
        public MbtClient create(){
            return new MbtClient(this);
        }
    }

    public class MbtClientExtra{

       public static final String EXTRA_NEW_STATE = "newState";

    }
}

