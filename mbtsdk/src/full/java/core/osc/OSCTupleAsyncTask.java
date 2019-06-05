package core.osc;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import core.eeg.storage.MbtEEGPacket;


public class OSCTupleAsyncTask extends AsyncTask<MbtEEGPacket, Void, Void> {


    public enum FREQUENCY {
        ALPHA(53, 56),
        BETA(57, 60),
        GAMMA(61, 64);

        private int ratio;
        private int power;

        FREQUENCY(int ratio, int power) {
            this.ratio = ratio;
            this.power = power;
        }

    }


    OSCTupleAsyncTask(OSCPortOut output) {
        oscPortOut = output;
    }
    static private final String MICRO_VOLT = "/uv";
    static private final String POWER = "/power";
    static private final String RATIO = "/ratio";
    static private OSCPortOut oscPortOut;


    @Override
    protected Void doInBackground(MbtEEGPacket... mbtEEGPacketsBundle) {

        for (MbtEEGPacket mbtEEGPackets : mbtEEGPacketsBundle) {
            int size = mbtEEGPackets.getChannelsData().size();

            for (int eegRawData = 0; eegRawData < size; eegRawData++) {
                Float p3uv = mbtEEGPackets.getChannelsData().get(0).get(eegRawData);
                Float p4uv = mbtEEGPackets.getChannelsData().get(1).get(eegRawData);
                processUv(p3uv, p4uv);
            }

            float[][] features = mbtEEGPackets.getFeatures();

            sendFeature(FREQUENCY.ALPHA.power, POWER, features);
            sendFeature(FREQUENCY.ALPHA.ratio, RATIO, features);


        }
        return null;
    }

    private void sendFeature(int index, String featureLabel, float[][] features) {
        OSCMessage channelDataPower = new OSCMessage(featureLabel);

        for (float[] feature : features) {
            channelDataPower.addArgument(feature[index]);
        }
        send(channelDataPower);
    }

    private void send(OSCMessage channelData) {
        try {
            // Send the messages
            Log.i("Osc", "Address: " + channelData.getAddress() + " | Data: " + channelData.getArguments());
            oscPortOut.send(channelData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void processUv(@NonNull Float rawEEGP3, @NonNull Float rawEEGP4) {
        OSCMessage channelData = new OSCMessage(MICRO_VOLT);
        channelData.addArgument(rawEEGP3);
        channelData.addArgument(rawEEGP4);
        send(channelData);
    }
}
