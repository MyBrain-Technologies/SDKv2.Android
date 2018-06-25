package engine;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import config.MbtConfig;
import core.MbtManager;
import core.bluetooth.BtState;
import core.eeg.storage.MbtEEGPacket;
import core.recordingsession.metadata.DeviceInfo;
import engine.clientevents.BaseException;
import engine.clientevents.ConnectionException;
import engine.clientevents.ConnectionStateListener;
import engine.clientevents.DeviceInfoListener;
import engine.clientevents.EEGException;
import engine.clientevents.EegListener;
import features.MbtFeatures;
import features.ScannableDevices;

/**
 * Created by Etienne on 08/02/2018.
 */

@Keep
public final class MbtClient {

    private static final String TAG = MbtClient.class.getName();

    //the client callbacks that will allow fluid communication between SDK and client app
    /**
     * The client listener to be notified upon arrival of a new list of EEG Packets.
     */

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

    /**
     * Call this method to establish a bluetooth connection with a remote device.
     * It is possible to connect either a Melomind or a VPro device through this method. You need to specify
     * the device type in the {@link ConnectionConfig} input instance with the {@link ScannableDevices} type.
     * <p>If the parameters are invalid, the method returns immediately and {@link ConnectionStateListener#onError(BaseException)} method is called</p>
     *
     * @param config the {@link ConnectionConfig} instance that holds all the configuration parameters inside.
     */
    public void connectBluetooth(@NonNull ConnectionConfig config){
        //MbtConfig.bluetoothConnectionTimeout = config.getConnectionTimeout();
        if(config.getMaxScanDuration() < MbtFeatures.MIN_SCAN_DURATION){
            config.getConnectionStateListener().onError(new ConnectionException(ConnectionException.INVALID_SCAN_DURATION));
            return;
        }

        if(config.getDeviceType() == ScannableDevices.VPRO){
            config.getConnectionStateListener().onError(new ConnectionException("ERROR, VPRO not supported in this version. Only MELOMIND available"));
            return;
        }

        MbtConfig.scannableDevices = config.getDeviceType();
        MbtConfig.bluetoothScanTimeout = config.getMaxScanDuration();

        this.mbtManager.connectBluetooth(config.getDeviceName(), config.getConnectionStateListener());
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
     * @param listener The callback that contains the onBatteryReceivedMethod
     */
    public void readBattery(@NonNull final DeviceInfoListener listener) {
        mbtManager.readBluetooth(DeviceInfo.BATTERY, listener);
    }

    /**
     * Initiates an EEG streaming ie, send a message to the headset to deliver EEG to the application.
     *
     * <p>You can customize some parameters in the {@link StreamConfig}class.</p>
     *
     * <p>If the parameters are incorrect, the function returns directly and the {@link EegListener#onError(BaseException)} method is called</p>
     * If something wrong happens during the operation, {@link EegListener#onError(BaseException)} method is called.
     *
     * <p>If everything went well, the EEG will be available in the {@link EegListener#onNewPackets(MbtEEGPacket)} callback.</p>
     *
     * @param streamConfig the configuration to pass to the streaming.
     */
    @SuppressWarnings("unchecked")
    public void startStream(@NonNull StreamConfig streamConfig){
        if(!streamConfig.isConfigCorrect())
            streamConfig.getEegListener().onError(new EEGException(EEGException.INVALID_PARAMETERS));
        else
            MbtConfig.eegBufferLengthClientNotif = (int)((streamConfig.getNotificationPeriod()* MbtFeatures.DEFAULT_SAMPLE_RATE)/1000);

        mbtManager.startStream(false, streamConfig.getEegListener(), streamConfig.getDeviceStatusListener());
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
     * the new state {@link core.bluetooth.BtState#INTERRUPTED} is sent to the user in the
     * {@link ConnectionStateListener#onStateChanged(BtState)} callback.
     *
     * <p>If the device is already connected, it simply disconnects the device.</p>
     */
    public void cancelConnection() {
        this.mbtManager.disconnectBluetooth(true);
    }

    /**
     * Sets the {@link ConnectionStateListener} to the connectionStateListener value
     * @param connectionStateListener the new {@link ConnectionStateListener}. Set it to null if you want to reset the listener
     */
    public void setConnectionStateListener(ConnectionStateListener<ConnectionException> connectionStateListener){
        this.mbtManager.setConnectionStateListener(connectionStateListener);
    }

    /**
     * Sets the {@link EegListener} to the connectionStateListener value
     * @param eegListener the new {@link EegListener}. Set it to null if you want to reset the listener
     */
    public void setEEGListener(EegListener<EEGException> eegListener){
        this.mbtManager.setEEGListener(eegListener);
    }


    /**
     * Builder that aims at helping user to instanciate the Client class.
     * At the moment, it is used internally.
     */
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
}
