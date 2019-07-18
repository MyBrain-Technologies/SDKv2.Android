package core.synchronisation;

import android.os.AsyncTask;

import java.util.ArrayList;

import core.eeg.storage.MbtEEGFeatures;
import core.eeg.storage.MbtEEGPacket;


public abstract class IStreamer<U> extends AsyncTask<MbtEEGPacket, Void, Void> {

    static private final String ADDRESS_PREFIX = "/";
    static final String RAW_EEG_ADDRESS = ADDRESS_PREFIX + "raweeg";
    static final String QUALITY_ADDRESS = ADDRESS_PREFIX +"quality";


    private boolean streamRawEEG;
    private boolean streamQualities;

    protected boolean startStreamNotificationSent;
    protected boolean stopStreamNotificationSent;

    private ArrayList<MbtEEGFeatures.Feature> featuresToStream;

    IStreamer(boolean streamRawEEG, boolean streamQualities, ArrayList<MbtEEGFeatures.Feature> featuresToStream) {
        this.streamRawEEG = streamRawEEG;
        this.streamQualities = streamQualities;
        this.featuresToStream = featuresToStream;

        this.startStreamNotificationSent = false;
        this.stopStreamNotificationSent = false;
    }

    @Override
    protected Void doInBackground(MbtEEGPacket... mbtEEGPacketsBundle) {

        for (MbtEEGPacket mbtEEGPackets : mbtEEGPacketsBundle) {
            if(streamRawEEG)
                streamRawEEGPacket(mbtEEGPackets.getChannelsData());

            if(streamQualities & mbtEEGPackets.getQualities() != null)
                streamQualities(mbtEEGPackets.getQualities());

            if(featuresToStream != null && !featuresToStream.isEmpty())
                streamFeatures(mbtEEGPackets, featuresToStream);
        }
        return null;
    }

        private void streamQualities(ArrayList<Float> qualities){
       stream(initStreamRequest(qualities, QUALITY_ADDRESS));

    }

    private void streamFeatures(MbtEEGPacket eegPacket, ArrayList<MbtEEGFeatures.Feature> featuresToStream){
        for (MbtEEGFeatures.Feature feature : featuresToStream) {
            stream(initStreamRequest(eegPacket.getFeature(feature), ADDRESS_PREFIX + feature.name()));//todo address
        }
    }

    private void streamRawEEGPacket(ArrayList<ArrayList<Float>> channelsData){
        int nbData = channelsData.size();
        int nbChannels = channelsData.get(0).size();

        for (int eegRawData = 0; eegRawData < nbData; eegRawData++) {
            ArrayList<Float> dataToStream = new ArrayList<>();
            for (int channel = 0; channel < nbChannels; channel++) {
                dataToStream.add(channelsData.get(eegRawData).get(channel));
            }
            stream(initStreamRequest(dataToStream, RAW_EEG_ADDRESS));
        }

    }

    protected void sendStartStreamNotification(){
        this.startStreamNotificationSent = true;
    }

    protected void sendStopStreamNotification(){
        this.stopStreamNotificationSent = true;
    }

    protected abstract void stream(U message);

    protected abstract U initStreamRequest(ArrayList<Float> dataToStream, String address);

}
