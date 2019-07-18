package core.synchronisation;

import java.io.IOException;
import java.util.ArrayList;
import config.SynchronisationConfig;
import utils.LogUtils;


public class MbtLSLStreamer extends IStreamer<ArrayList<Float>> {

    private LSL.StreamOutlet lslOut;

    public MbtLSLStreamer(SynchronisationConfig config) {
        super(config.streamRawEEG(), config.streamQualities(), config.getFeaturesToStream());
    }

    @Override
    protected void stream(ArrayList<Float> message) {
        lslOut.push_sample(message);
    }


    @Override
    protected ArrayList<Float> initStreamRequest(ArrayList<Float> dataToStream, String address) {
        LSL.StreamInfo info = new LSL.StreamInfo("Address",address,1, LSL.IRREGULAR_RATE, LSL.ChannelFormat.string,"myuid4563");
        try {
            lslOut = new LSL.StreamOutlet(info);
        } catch(IOException ex) {
            LogUtils.e(this.getClass().getName(),"Unable to open LSL outlet. Have you added <uses-permission android:name=\"android.permission.INTERNET\" /> to your manifest file?");
            return null;
        }

        return dataToStream;
    }

    @Override
    protected void sendStartStreamNotification() {
        //todo send boolean true
        super.sendStartStreamNotification();
    }

    @Override
    protected void sendStopStreamNotification() {
        //todo send boolean false
        lslOut.close();
        lslOut = null;
        super.sendStopStreamNotification();
    }
}
