 package engine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;


import config.ConnectionConfig;
import config.MbtConfig;
import config.StreamConfig;
import core.MbtManager;
import core.device.model.MbtDevice;
import core.eeg.storage.MbtEEGPacket;
import core.device.model.DeviceInfo;
import engine.clientevents.BaseError;
import engine.clientevents.ConfigError;
import engine.clientevents.ConnectionStateListener;
import engine.clientevents.ConnectionStateReceiver;
import engine.clientevents.DeviceBatteryListener;
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
     */
    public static MbtClient init(@NonNull Context context){
        if(clientInstance == null) {
            clientInstance = new MbtClientBuilder()
                    .setContext(context)
                    .setMbtManager(new MbtManager(context))
                    .create();
        }
        return clientInstance;
    }

    /**
     * @return The current instance of the client. Init() must have been called first
     */
    public static MbtClient getClientInstance(){
        return clientInstance;
    }
    /**
     * Constructor that use the MbtClientBuilder
     * @param builder object for creating the MbtClient instance with a setters syntax.
     */
    private MbtClient(MbtClientBuilder builder){
        this.mbtManager = builder.mbtManager;
    }

    /**
     * Call this method to establish a bluetooth connection with a remote Melomind device.
     * <p>If the parameters are invalid, the method returns immediately and {@link ConnectionStateReceiver#onError(engine.clientevents.BaseError, String)} method is called</p>
     *
     * @param config the {@link ConnectionConfig} instance that holds all the configuration parameters inside.
     */
    @SuppressWarnings("unchecked")
    public void connectBluetooth(@NonNull ConnectionConfig config){
        MbtConfig.setBluetoothScanTimeout(config.getMaxScanDuration());
        MbtConfig.setConnectAudioIfDeviceCompatible(config.useAudio());

        if(config.getDeviceName()!= null && config.getDeviceName().length() != MbtFeatures.DEVICE_NAME_LENGTH) {
            config.getConnectionStateListener().onError(ConfigError.ERROR_INVALID_PARAMS, " Device name must start with the " + MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX + " and contain 10 digits ");
            return;
        }
        if(config.getDeviceQrCode() != null && config.getDeviceQrCode().length() != MbtFeatures.DEVICE_QR_CODE_LENGTH && config.getDeviceQrCode().length() != MbtFeatures.DEVICE_QR_CODE_LENGTH-1 ) {
            config.getConnectionStateListener().onError(ConfigError.ERROR_INVALID_PARAMS, " Device QR code must start with " + MbtFeatures.QR_CODE_NAME_PREFIX + " and contain 8 digits "+ MbtFeatures.DEVICE_QR_CODE_LENGTH);
            return;
        }
        if(config.getMaxScanDuration() < MbtFeatures.MIN_SCAN_DURATION){
            config.getConnectionStateListener().onError(ConfigError.ERROR_INVALID_PARAMS,ConfigError.SCANNING_MINIMUM_DURATION);
            return;
        }

        if(config.getDeviceType() == MbtDeviceType.VPRO){
            config.getConnectionStateListener().onError(HeadsetDeviceError.ERROR_VPRO_INCOMPATIBLE,null);
            return;
        }

        this.mbtManager.connectBluetooth(config.getConnectionStateListener(), config.getDeviceName(), config.getDeviceQrCode(), config.getDeviceType());
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
    public void readBattery(@NonNull final DeviceBatteryListener listener) {
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
            MbtConfig.setEegBufferLengthClientNotif(((streamConfig.getNotificationPeriod()* MbtFeatures.DEFAULT_SAMPLE_RATE)/1000));

        mbtManager.startStream(streamConfig);
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
     * the new state {@link core.bluetooth.BtState#CONNECTION_INTERRUPTED} is sent to the user in the {@link BluetoothStateListener#onNewState(BtState)} callback
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

    @Keep
    private static class MbtClientBuilder {
        private Context mContext;
        private MbtManager mbtManager;

        @NonNull
        public MbtClientBuilder setContext(final Context context){
            this.mContext = context;
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
