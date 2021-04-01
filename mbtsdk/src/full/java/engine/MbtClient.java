package engine;

import android.content.BroadcastReceiver;
import android.content.Context;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import command.CommandInterface;
import command.DeviceCommands;
import config.ConnectionConfig;
import config.MbtConfig;
import config.RecordConfig;
import config.StreamConfig;
import core.Indus5FastMode;
import core.MbtManager;
import core.bluetooth.BluetoothState;
import core.device.model.DeviceInfo;
import core.device.model.MbtDevice;
import core.device.model.MbtVersion;
import core.eeg.storage.MbtEEGPacket;
import engine.clientevents.BaseError;
import engine.clientevents.BluetoothError;
import engine.clientevents.BluetoothStateListener;
import engine.clientevents.ConfigError;
import engine.clientevents.ConnectionStateListener;
import engine.clientevents.DeviceBatteryListener;
import engine.clientevents.EegListener;

import engine.clientevents.OADStateListener;
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
     * Set the current instance of the client instance to null
     */
    public static void resetClientInstance(){
        clientInstance = null;
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
     * the device type in the {@link ConnectionConfig} input instance with the {@link MbtDeviceType} type.
     * <p>If the parameters are invalid, the method returns immediately and {@link BluetoothStateListener#onError(engine.clientevents.BaseError, String)} method is called</p>
     *
     * @param config the {@link ConnectionConfig} instance that holds all the configuration parameters inside.
     */
    @SuppressWarnings("unchecked")
    public void connectBluetooth(@NonNull ConnectionConfig config){
        MbtConfig.setBluetoothScanTimeout(config.getMaxScanDuration());

        if(!config.isDeviceNameValid(config.getDeviceType())) {
            config.getConnectionStateListener().onError(ConfigError.ERROR_INVALID_PARAMS, " Device name must start with the " + MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX + " and contain 10 digits ");
            return;
        }

        if(!config.isDeviceQrCodeValid()){
            config.getConnectionStateListener().onError(ConfigError.ERROR_INVALID_PARAMS, " Device QR code must start with " + MbtFeatures.QR_CODE_NAME_PREFIX + " and contain "+ MbtFeatures.DEVICE_QR_CODE_LENGTH+ " digits ");
            return;
        }

        if(!config.isScanDurationValid()){
            config.getConnectionStateListener().onError(ConfigError.ERROR_INVALID_PARAMS,ConfigError.SCANNING_MINIMUM_DURATION);
            return;
        }

        if(config.getDeviceType() == MbtDeviceType.VPRO && config.connectAudio()){
            config.getConnectionStateListener().onError(BluetoothError.ERROR_A2DP_CONNECT_FAILED,"Impossible to connect a VPRO headset for audio streaming.");
        }

        if(!config.isMtuValid()){
            config.getConnectionStateListener().onError(ConfigError.ERROR_INVALID_PARAMS,"MTU must be included between 23 and 121");
            return;
        }

        this.mbtManager.connectBluetooth(config.getConnectionStateListener(),config.connectAudio(), config.getDeviceName(), config.getDeviceQrCode(), config.getDeviceType(), config.getMtu());
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
    public void readBattery(DeviceBatteryListener listener) {
        mbtManager.readBluetooth(DeviceInfo.BATTERY, listener);
    }

    /**
     * Initiates an EEG streaming ie, send a message to the headset to deliver EEG to the application.
     *
     * <p>You can customize some parameters in the {@link StreamConfig}class.</p>
     *
     * <p>If the parameters are incorrect, the function returns directly and the {@link EegListener#onError} method is called</p>
     * If something wrong happens during the operation, {@link EegListener#onError} method is called.
     *
     * <p>If everything went well, the EEG will be available in the {@link EegListener#onNewPackets(MbtEEGPacket)} callback.</p>
     *
     * @param streamConfig the configuration to pass to the streaming.
     */
    @SuppressWarnings("unchecked")
    public void startStream(@NonNull StreamConfig streamConfig){
        if (Indus5FastMode.INSTANCE.isEnabled()) {
            mbtManager.startStream(streamConfig);
        } else {
            if (!streamConfig.isNotificationConfigCorrect())
                streamConfig.getEegListener().onError(ConfigError.ERROR_INVALID_PARAMS, streamConfig.shouldComputeQualities() ?
                        ConfigError.NOTIFICATION_PERIOD_RANGE_QUALITIES : ConfigError.NOTIFICATION_PERIOD_RANGE);
            else
                requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
                    @Override
                    public void onRequestComplete(MbtDevice device) {
                        MbtConfig.setEegBufferLengthClientNotif(streamConfig.getNotificationPeriod());
                    }
                });

            mbtManager.startStream(streamConfig);
        }
    }

    /**
     * Stops the currently running eeg stream. This stops bluetooth acquisition and
     * reset all internal buffering system.
     */
    public void stopStream(){
        mbtManager.stopStream(null);
    }

    /**
     * Stops the currently running eeg stream. This stops bluetooth acquisition and
     * reset all internal buffering system.
     */
    public void stopStream(RecordConfig recordConfig){
        mbtManager.stopStream(recordConfig);
    }

    public void startRecord(Context context){
        mbtManager.startRecord(context);
    }

    public void stopRecord(@NonNull RecordConfig recordConfig){
        mbtManager.stopRecord(recordConfig);
    }

    /**
     * Sends a command to the connected headset to change its current serial number
     * The new serial number is stored and returned by the headset if the command succeeds.
     * The headset returns a response that can be retrieved in the onRequestComplete callback of the requestCallback input
     * This response contains the new serial number in a byte array
     * @param serialNumber is the new value to set to the serial number
     * @param commandCallback returns the headset raw response in the onRequestComplete callback if the command has been well sent
     * A non null requestCallback instance is mandatory if you want to get the headset response
     * The onError callback provided by the requestCallback is triggered if an error occurred during the request sending.
     */
    public void updateSerialNumber(@NonNull String serialNumber, @Nullable CommandInterface.CommandCallback< byte[]> commandCallback){
        mbtManager.sendCommand(new DeviceCommands.UpdateSerialNumber(serialNumber, commandCallback));
    }

    /**
     * Sends a command to the connected headset to change its current external name
     * The headset returns a response that can be retrieved in the onRequestComplete callback of the requestCallback input
     * This response contains the new external name in a byte array
     * @param externalName is the new value to set to the external name
     * @param commandCallback returns the headset raw response in the onRequestComplete callback if the command has been well sent
     * A non null requestCallback instance is mandatory if you want to get the headset response
     * The onError callback provided by the requestCallback is triggered if an error occurred during the request sending.
     */
    public void updateExternalName(@NonNull String externalName, @Nullable CommandInterface.CommandCallback<byte[]> commandCallback){
        mbtManager.sendCommand(new DeviceCommands.UpdateExternalName(externalName, commandCallback));
    }

    /**
     * Sends a command to the connected headset to establish an audio Bluetooth A2DP connection
     * The headset returns a response that can be retrieved in the onRequestComplete callback of the requestCallback input
     * This response contains the connection status in a byte array
     * @param commandCallback returns the headset raw response in the onRequestComplete callback if the command has been well sent
     * A non null requestCallback instance is mandatory if you want to get the headset response
     * The onError callback provided by the requestCallback is triggered if an error occurred during the request sending.
     */
    public void connectAudio(@Nullable CommandInterface.CommandCallback<byte[]> commandCallback){
        mbtManager.sendCommand(new DeviceCommands.ConnectAudio(commandCallback));
    }

    /**
     * Sends a command to the connected headset to establish an audio Bluetooth A2DP disconnection
     * The headset returns a response that can be retrieved in the onRequestComplete callback of the requestCallback input
     * This response contains the disconnection status in a byte array
     * @param commandCallback returns the headset raw response in the onRequestComplete callback if the command has been well sent
     * A non null requestCallback instance is mandatory if you want to get the headset response
     * The onError callback provided by the requestCallback is triggered if an error occurred during the request sending.
     */
    public void disconnectAudio(@Nullable CommandInterface.CommandCallback<byte[]> commandCallback){
        mbtManager.sendCommand(new DeviceCommands.DisconnectAudio(commandCallback));
    }

    /**
     * Sends a command to the connected headset to get its system status
     * The system status is returned as a byte array in the onRequestComplete callback of the requestCallback
     * @param commandCallback returns the headset raw response in the onRequestComplete callback if the command has been well sent
     * A non null requestCallback instance is mandatory if you want to get the headset response
     * The onError callback provided by the requestCallback is triggered if an error occurred during the request sending.
     */
    public void getDeviceSystemStatus(@NonNull CommandInterface.CommandCallback<byte[]> commandCallback){
        mbtManager.sendCommand(new DeviceCommands.GetSystemStatus(commandCallback));
    }

    /**
     * Sends a command to reboot the connected headset
     * @param simpleCommandCallback returns the headset raw response in the onRequestComplete callback if the command has been well sent
     * The onError callback provided by the requestCallback is triggered if an error occurred during the request sending.
     * A non null requestCallback instance is mandatory to be notified if an error occurred during the request sending.
     * The onRequestComplete callback provided by the requestCallback is never called
     * It is useless to enter a CommandCallback Object for the commandCallback input :
     * as no response is expected, you have to enter a ICommandCallback Object.
     * The onError callback is triggered in case you enter a CommandCallback and the device command is not sent.
     */
    public void rebootDevice(@Nullable CommandInterface.SimpleCommandCallback simpleCommandCallback) {
        mbtManager.sendCommand(new DeviceCommands.Reboot(simpleCommandCallback));
    }

    /**
     * Stops a pending connection process. If successful,
     * the new state {@link core.bluetooth.BluetoothState#CONNECTION_INTERRUPTED} is sent to the user in the {@link BluetoothStateListener#onNewState(BluetoothState)} callback
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

    /**
     * Perform a request to start an OAD firmware update.
     * @param firmwareVersion is the firmware version to install on the connected headset device.
     * @param stateListener is an optional (nullable) listener that notify the client when the OAD update progress & state change.
     */
    public void updateFirmware(@NonNull MbtVersion firmwareVersion, @Nullable OADStateListener<BaseError> stateListener){
        mbtManager.updateFirmware(firmwareVersion, stateListener);
    }

    /**
     * Apply a bandpass filter to the input signal to keep frequencies included between
     * @param minFrequency and
     * @param maxFrequency .
     * @param size is the number of EEG data of one channel
     * @param inputData is the array of EEG data to filter for one channel
     * @param resultCallback is the callback that returns the filtered signal
     */
    public void bandpassFilter(float minFrequency, float maxFrequency, int size,@NonNull float[] inputData,@NonNull SimpleRequestCallback<float[]> resultCallback){
        mbtManager.bandpassFilter(minFrequency, maxFrequency, size, inputData, resultCallback);
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
        MbtClientBuilder setMbtManager(final MbtManager mbtManager){
            this.mbtManager = mbtManager;
            return this;
        }

        @NonNull
        public MbtClient create(){
            return new MbtClient(this);
        }
    }

}

