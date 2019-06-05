package core.osc;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import java.util.ArrayList;
import java.util.Arrays;

import core.eeg.storage.MbtEEGPacket;


public class OSCAsyncTask extends AsyncTask<MbtEEGPacket, Void, Void> {


    public enum FREQUENCY {

        ALPHA(53, 56),
        BETA(57, 60),
        GAMMA(61, 64);

        private int ratio;
        private int power;

        private final String POWER_ADDRESS = "/power";
        private final String RATIO_ADDRESS = "/ratio";

        FREQUENCY(int ratio, int power) {
            this.ratio = ratio;
            this.power = power;
        }

    }


    OSCAsyncTask(OSCPortOut output) {
        oscPortOut = output;
    }

    static private final String EEG_P3_ADDRESS = "/p3";
    static private final String EEG_P4_ADDRESS = "/p4";
    static private final ArrayList<String> EEG_ADDRESS = new ArrayList<>(Arrays.asList(EEG_P3_ADDRESS, EEG_P4_ADDRESS));
    static private final String MICRO_VOLT = "/uv";
    static private OSCPortOut oscPortOut;


    @Override
    protected Void doInBackground(MbtEEGPacket... mbtEEGPacketsBundle) {

        for (MbtEEGPacket mbtEEGPackets : mbtEEGPacketsBundle) {
            int size = mbtEEGPackets.getChannelsData().size();

            for (int eegRawData = 0; eegRawData < size; eegRawData++) {
                if(size > 0) {
                    for (int channel = 0; channel < mbtEEGPackets.getChannelsData().get(0).size(); channel++) {
                        processUv(mbtEEGPackets.getChannelsData().get(channel).get(eegRawData), EEG_ADDRESS.get(channel));
                    }
                }
            }

            float[][] features = mbtEEGPackets.getFeatures();
            float[] p3feature = features[0];
            float[] p4feature = features[1];

            sendFeature(EEG_P3_ADDRESS, FREQUENCY.ALPHA, p3feature);
            sendFeature(EEG_P4_ADDRESS, FREQUENCY.ALPHA, p4feature);

        }
        return null;
    }

    private void sendFeature(String eegPosition, FREQUENCY frequency, float[] features) {
        String powerAddress = eegPosition + frequency.POWER_ADDRESS;
        float powerArgument = features[frequency.power];
        sendOSCMessage(powerAddress, powerArgument);

        String ratioAddress = eegPosition + frequency.RATIO_ADDRESS;
        float ratioArgument = features[frequency.ratio];
        sendOSCMessage(ratioAddress, ratioArgument);
    }

    private void sendOSCMessage(String address, Object argument) {
       OSCMessage message = new OSCMessage(address);
        message.addArgument(argument);
        send(message);
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


    private void processUv(@NonNull Float rawEEG, String eegPosition) {
        OSCMessage channelData = new OSCMessage(eegPosition + MICRO_VOLT);
        channelData.addArgument(rawEEG);
        send(channelData);
    }
}
