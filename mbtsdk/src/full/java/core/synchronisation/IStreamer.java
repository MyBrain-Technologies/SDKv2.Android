package core.synchronisation;

import android.os.AsyncTask;

import java.util.ArrayList;

import core.eeg.storage.MbtEEGPacket;


public abstract class IStreamer<U> extends AsyncTask<MbtEEGPacket, Void, Void> {

    static final String RAW_EEG_ADDRESS = "/raweeg";
    static final String QUALITY_ADDRESS = "/quality";

    static private final String ALPHA_ADDRESS = "/alpha";
    static private final String BETA_ADDRESS = "/beta";
    static private final String DELTA_ADDRESS = "/delta";
    static private final String GAMMA_ADDRESS = "/gamma";

    static final String POWER_ADDRESS = "/power";
    static final String LOG_POWER_ADDRESS = "/logpower";
    static final String NORMALIZED_POWER_ADDRESS = "/normalizedpower";
    static final String RATIO_ADDRESS = "/ratio";

    protected boolean streamRawEEG;
    protected boolean streamQualities;

    protected int[] featuresToStream;

    public IStreamer(boolean streamRawEEG, boolean streamQualities, int[] featuresToStream) {
        this.streamRawEEG = streamRawEEG;
        this.streamQualities = streamQualities;
        this.featuresToStream = featuresToStream;
    }

    @Override
    protected Void doInBackground(MbtEEGPacket... mbtEEGPacketsBundle) {

        for (MbtEEGPacket mbtEEGPackets : mbtEEGPacketsBundle) {
            if(streamRawEEG)
                streamRawEEGPacket(mbtEEGPackets.getChannelsData());

            if(streamQualities & mbtEEGPackets.getQualities() != null)
                streamQualities(mbtEEGPackets.getQualities());

            if(featuresToStream != null && featuresToStream.length != 0)
                streamFeatures(mbtEEGPackets, featuresToStream);
        }
        return null;
    }

        private void streamQualities(ArrayList<Float> qualities){
       stream(initStreamRequest(qualities, QUALITY_ADDRESS));

    }

    private void streamFeatures(MbtEEGPacket eegPacket, int[] featuresToStream){
        for (int feature : featuresToStream) {
            stream(initStreamRequest(eegPacket.getFeature(feature), ));//todo address
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

    protected abstract void stream(U message);

    protected abstract U initStreamRequest(ArrayList<Float> dataToStream, String address);
}
