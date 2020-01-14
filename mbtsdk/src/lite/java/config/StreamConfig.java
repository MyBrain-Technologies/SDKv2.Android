package config;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.LinkedList;
import command.DeviceCommand;
import command.DeviceStreamingCommands;
import core.eeg.storage.MbtEEGPacket;
import engine.MbtClient;
import engine.clientevents.BaseError;
import engine.clientevents.DeviceStatusListener;
import engine.clientevents.EegListener;
import features.MbtDeviceType;
import features.MbtFeatures;
/**
 * This class aims at configuring the stream process. It contains user configurable
 * parameters to specify how the streaming is going to be.
 * <p>Use the {@link Builder} class to instanciate this.</p>
 */
@Keep
public final class StreamConfig {

    private int notificationPeriod;
    private final EegListener<BaseError> eegListener;
    private DeviceStatusListener<BaseError> deviceStatusListener;
    private boolean computeQualities;
    /**
     * Recording configuration is all the data required to store the EEG packets in a JSON file.
     */
    private RecordConfig recordConfig;
    /**
     * Optional list of commands sent to the headset in order to
     * configure a parameter,
     * or get values stored by the headset
     * or ask the headset to perform an action.
     */
    private LinkedList<DeviceCommand> deviceCommands ;
    private StreamConfig(MbtDeviceType deviceType,
                         boolean computeQualities,
                         EegListener<BaseError> eegListener,
                         DeviceStatusListener<BaseError> deviceStatusListener,
                         int notificationPeriod,
                         DeviceStreamingCommands[] deviceCommands,
                         RecordConfig recordConfig){
        this.computeQualities = computeQualities;
        this.eegListener = eegListener;
        this.deviceStatusListener = deviceStatusListener;
        this.notificationPeriod = notificationPeriod;
        this.recordConfig = recordConfig;
        this.deviceCommands = new LinkedList<>();
        DeviceStreamingCommands.EegConfig eegConfig = new DeviceStreamingCommands.EegConfig.Builder(null).createForDevice(deviceType);
        if(deviceCommands != null && deviceCommands.length > 0) {
            for (DeviceStreamingCommands deviceCommand : deviceCommands) {
                if(deviceCommand != null){
                    int index = getRegisteredCommandIndex(deviceCommand);
                    if(index != -1) {//if a command with the same type is already in the list, it is replaced (input can be different)
                        if (deviceCommand instanceof DeviceStreamingCommands.EegConfig)
                            eegConfig = (DeviceStreamingCommands.EegConfig) deviceCommand;
                        else
                            this.deviceCommands.set(index,  (DeviceCommand) deviceCommand);
                    }else if(!(deviceCommand instanceof DeviceStreamingCommands.EegConfig))
                        this.deviceCommands.addFirst( (DeviceCommand) deviceCommand);
                }
            }
        }
        this.deviceCommands.addLast(eegConfig);

    }
    /**
     * Returns the index of the already registered command in the deviceCommands list
     * Returns -1 if the command is not already registered
     * @param deviceCommand
     * @return
     */
    private int getRegisteredCommandIndex(DeviceStreamingCommands deviceCommand){
        for (DeviceCommand registeredCommand : deviceCommands) {
            if(registeredCommand.getClass() == deviceCommand.getClass())
                return deviceCommands.indexOf(registeredCommand);
        }
        return -1;
    }
    public EegListener getEegListener() {
        return eegListener;
    }
    public DeviceStatusListener<BaseError> getDeviceStatusListener() {
        return deviceStatusListener;
    }
    public int getNotificationPeriod() {
        return notificationPeriod;
    }
    public boolean shouldComputeQualities() {
        return computeQualities;
    }
    public LinkedList<DeviceCommand> getDeviceCommands() {
        return deviceCommands;
    }
    /**
     * Return the recording configuration that holds all the data required to store the EEG packets in a JSON file.
     */
    public RecordConfig getRecordConfig() {
        return recordConfig;
    }
    public void setNotificationPeriod(int notificationPeriod) {
        this.notificationPeriod = notificationPeriod;
    }
    public void setComputeQualities(boolean computeQualities) {
        this.computeQualities = computeQualities;
    }
    public void setDeviceCommands(LinkedList<DeviceCommand> deviceCommands) {
        this.deviceCommands = deviceCommands;
    }
    public void setDeviceStatusListener(DeviceStatusListener<BaseError> deviceStatusListener) {
        this.deviceStatusListener = deviceStatusListener;
    }
    /**
     * Builder class to ease construction of the {@link StreamConfig} instance.
     */
    @Keep
    public static class Builder{
        private int notificationPeriod = MbtFeatures.DEFAULT_CLIENT_NOTIFICATION_PERIOD;
        /**
         * The EEG Listener is a callback that receives the EEG raw data streamed
         * from the headset to the SDK.
         */
        @NonNull
        private final EegListener<BaseError> eegListener;
        @Nullable
        private DeviceStatusListener<BaseError> deviceStatusListener;
        private boolean computeQualities = false;
        /**
         * Recording configuration is all the data required to store the EEG packets in a JSON file.
         */
        @Nullable
        private RecordConfig recordConfig;
        /**
         * Optional set of commands sent to the headset in order to
         * configure a parameter,
         * or get values stored by the headset
         * or ask the headset to perform an action.
         */
        private DeviceStreamingCommands[] deviceCommands;
        /**
         * The EEG Listener is mandatory to receive the EEG stream from the headset to the SDK.
         */
        public Builder(@NonNull EegListener<BaseError> eegListener){
            this.eegListener = eegListener;
        }

        /**
         * Use this method to specify how much eeg you want to receive in the {@link EegListener#onNewPackets(MbtEEGPacket)} method.
         *
         * <p>Warning, the duration is based on a quantity of eeg data. This quantity depends on the sampling frequency of the device.
         * It is by default {@link MbtFeatures#DEFAULT_SAMPLE_RATE} in Hertz.
         * The specified period is in any case a fixed duration based on a timer. This means that you may be notified with a certain latency.
         *</p>
         *
         * <p>For better performances, it is HIGHLY recommended that the period is a multiple of the sampling period.
         * For example, if the sampling frequency is 250Hz, then the sampling period is 4ms.
         * It means that the value must be a multiple of 4ms.</p>
         *
         * <p>It is by default set to {@link MbtFeatures#DEFAULT_CLIENT_NOTIFICATION_PERIOD}</p>
         * @param periodInMillis the period in milliseconds
         * @return the builder instance
         */
        @NonNull
        public Builder setNotificationPeriod(int periodInMillis){
            this.notificationPeriod = periodInMillis;
            return this;
        }

        public StreamConfig createForDevice(MbtDeviceType deviceType){
            return new StreamConfig(
                    deviceType,
                    this.computeQualities,
                    this.eegListener,
                    this.deviceStatusListener,
                    this.notificationPeriod,
                    this.deviceCommands,
                    this.recordConfig);
        }
    }
    /**
     * Checks if the notification configuration parameters are correct
     * @return true is the configuration is correct, false otherwise
     */
    public boolean isNotificationConfigCorrect() {
        if(this.notificationPeriod <  (this.computeQualities ?
                MbtFeatures.MIN_CLIENT_NOTIFICATION_PERIOD_WITH_QUALITIES_IN_MILLIS : MbtFeatures.MIN_CLIENT_NOTIFICATION_PERIOD_IN_MILLIS))
            return false;
        else if(notificationPeriod >  (this.computeQualities ?
                MbtFeatures.MAX_CLIENT_NOTIFICATION_PERIOD_WITH_QUALITIES_IN_MILLIS : MbtFeatures.MAX_CLIENT_NOTIFICATION_PERIOD_IN_MILLIS))
            return false;
        return true;
    }
}
