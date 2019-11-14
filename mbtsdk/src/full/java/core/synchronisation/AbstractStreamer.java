package core.synchronisation;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import config.SynchronisationConfig;
import core.eeg.storage.Feature;
import core.eeg.storage.MbtEEGPacket;
import utils.AsyncUtils;
import utils.LogUtils;

/**
 * @param <U> type of the message sent over the streaming protocol
 */
public abstract class AbstractStreamer<U, V, C extends SynchronisationConfig.AbstractConfig> implements IStreamer<U> {

    private static final String TAG = AbstractStreamer.class.getName();

    private static final boolean START_STREAM = true;
    private static final boolean STOP_STREAM = false;

    static final String ADDRESS_PREFIX = "/";

    static class StreamNotificationState {

        boolean startNotificationSent = false;
        boolean stopNotificationSent = false;

        void sendStartNotification() {
            this.startNotificationSent = true;
        }

        void sendStopNotification() {
            this.stopNotificationSent = true;
        }
    }

    private static final String RAW_EEG_ADDRESS = ADDRESS_PREFIX + "raweeg";
    private static final String QUALITY_ADDRESS = ADDRESS_PREFIX + "quality";
    private static final String FEATURE_ADDRESS = ADDRESS_PREFIX + "feature";
    private static final String STATUS_ADDRESS = ADDRESS_PREFIX + "status";

    private static HashMap<String, StreamNotificationState> notificationStateForAddressMap = new HashMap<>();
    private static ArrayList<Feature> featuresToStream = new ArrayList<>();

    V streamer;

    AbstractStreamer(C config) {
        if(config == null)
            return;

        boolean streamRawEEG = config.streamRawEEG();
        boolean streamQualities = config.streamQualities();
        boolean streamStatus = config.streamStatus();
        HashSet<Feature> features = config.getFeaturesToStream();

        Log.d(TAG,"\nstreamRawEEG "+streamRawEEG+ " \nstreamQualities "+streamQualities + " \nstreamStatus "+streamStatus + " \nfeaturesToStream " +(features != null ? features.toString(): "null"));

        if (streamRawEEG)
            notificationStateForAddressMap.put(RAW_EEG_ADDRESS, new StreamNotificationState());

        if (streamQualities)
            notificationStateForAddressMap.put(QUALITY_ADDRESS, new StreamNotificationState());

        if (streamStatus)
            notificationStateForAddressMap.put(STATUS_ADDRESS, new StreamNotificationState());

        if (features != null)
            for (Feature feature : features) {
                featuresToStream.add(feature);
                notificationStateForAddressMap.put(FEATURE_ADDRESS + ADDRESS_PREFIX + feature.name().toLowerCase().replace("_",ADDRESS_PREFIX), new StreamNotificationState());
            }

        initStreamer(config);
        notifyReceiverStartOrStop(START_STREAM);

    }

    abstract void initStreamer(C config);

    private void notifyReceiverStartOrStop(String address, boolean startOrStopStream){

        if(startOrStopStream == START_STREAM && !notificationStateForAddressMap.get(address).startNotificationSent) {
            notificationStateForAddressMap.get(address).sendStartNotification();
            stream(initStreamRequest(address, startOrStopStream));

        }else if(startOrStopStream == STOP_STREAM && !notificationStateForAddressMap.get(address).stopNotificationSent) {
            notificationStateForAddressMap.get(address).sendStopNotification();
            stream(initStreamRequest(address, startOrStopStream));
        }
    }

    private void notifyReceiverStartOrStop(boolean isStart){
        LogUtils.d(TAG,(isStart ? "Start" : "Stop")+" OSC stream");

        if(streamRawEEG())
            notifyReceiverStartOrStop(RAW_EEG_ADDRESS, isStart);

        if(streamQualities())
            notifyReceiverStartOrStop(QUALITY_ADDRESS, isStart);

        if(streamStatus())
            notifyReceiverStartOrStop(STATUS_ADDRESS, isStart);

        if(getFeaturesAddresses() != null)
            for (String featureAddress : getFeaturesAddresses()) {
                notifyReceiverStartOrStop(featureAddress, isStart);
            }
    }

    private void streamRawEEGPacket(ArrayList<ArrayList<Float>> channelsData){
        LogUtils.d(TAG,"Number of Data: "+channelsData.size()+ " | Number of Channels: "+channelsData.get(0).size());

        int nbData = channelsData.size();
        int nbChannels = channelsData.get(0).size();

        for (int eegRawData = 0; eegRawData < nbData; eegRawData++) {
            ArrayList<Float> dataToStream = new ArrayList<>();
            for (int channel = 0; channel < nbChannels; channel++) {
                dataToStream.add(channelsData.get(eegRawData).get(channel));
            }
            stream(initStreamRequest(RAW_EEG_ADDRESS, dataToStream));
        }
    }

    private void streamQualities(ArrayList<Float> qualities){
        stream(initStreamRequest(QUALITY_ADDRESS, qualities));
    }

    private void streamStatus(ArrayList<Float> status){
        stream(initStreamRequest(STATUS_ADDRESS, status));
    }

    private void streamFeatures(MbtEEGPacket eegPacket, ArrayList<String> featuresAddresses, ArrayList<Feature> featuresToStream){
        LogUtils.d(TAG,"Stream feature "+featuresToStream);
        for (int feature = 0 ; feature < featuresToStream.size() ; feature ++){
            stream(initStreamRequest(
                    featuresAddresses.get(feature), //get the address
                    eegPacket.getFeature(featuresToStream.get(feature)))); //get the value of the feature
        }
    }

    private boolean streamRawEEG(){
        return notificationStateForAddressMap.containsKey(RAW_EEG_ADDRESS);
    }

    private boolean streamQualities(){
        return notificationStateForAddressMap.containsKey(QUALITY_ADDRESS);
    }

    private boolean streamStatus(){
        return notificationStateForAddressMap.containsKey(STATUS_ADDRESS);
    }

    private ArrayList<String> getFeaturesAddresses(){
        ArrayList<String> featuresAddresses = new ArrayList<>();
        for (String address : notificationStateForAddressMap.keySet()) {
            if(address.startsWith(FEATURE_ADDRESS))
                featuresAddresses.add(address);
        }
        return (featuresAddresses.isEmpty() ? null : featuresAddresses);
    }

    public void reset(){
        notifyReceiverStartOrStop(STOP_STREAM);
        notificationStateForAddressMap = new HashMap<>();
        featuresToStream = new ArrayList<>();
    }

    /**
     * Select the EEG packets data to stream and stream it
     */
    void execute(final MbtEEGPacket... mbtEEGPackets){
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                if(mbtEEGPackets != null){
                    for (MbtEEGPacket mbtEEGPacket : mbtEEGPackets) {
                        if(streamRawEEG())
                            streamRawEEGPacket(mbtEEGPacket.getChannelsData());

                        if(streamQualities() & mbtEEGPacket.getQualities() != null) //if qualities have not been enabled in the StreamConfig configuration
                            streamQualities(mbtEEGPacket.getQualities());

                        if(streamStatus() & mbtEEGPacket.getStatusData() != null) //if qualities have not been enabled in the StreamConfig configuration
                            streamStatus(mbtEEGPacket.getStatusData());

                        if(featuresToStream != null && getFeaturesAddresses() != null)
                            streamFeatures(mbtEEGPacket, getFeaturesAddresses(), featuresToStream);
                    }
                }
            }
        });
    }

}



