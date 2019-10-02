package core.synchronisation;

import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import config.SynchronisationConfig;
import utils.LogUtils;


public class MbtLSLProcessor extends AbstractStreamer<Float[], LSL.StreamOutlet, SynchronisationConfig.LSL> {

    private LSL.StreamOutlet lslOut;

    MbtLSLProcessor(SynchronisationConfig.LSL config) {
        super(config);
    }

    @Override
    protected void initStreamer(SynchronisationConfig.LSL config) {

//        LSL.StreamInfo info = new LSL.StreamInfo("Address",config.getIpAddress(),1, LSL.IRREGULAR_RATE, LSL.ChannelFormat.string,"myuid4563");
//        try {
//            lslOut = new LSL.StreamOutlet(info);
//        } catch(IOException ex) {
//            LogUtils.e(this.getClass().getName(),"Unable to open LSL outlet. Have you added <uses-permission android:name=\"android.permission.INTERNET\" /> to your manifest file?");
//        }
    }

    @Override
    public Float[] initStreamRequest(String address, Object... dataToStream) {
        return Arrays.copyOf(dataToStream,
                dataToStream.length,
                Float[].class);
    }

    @Override
    public void stream(Float[] message) {
        //lslOut.push_sample(ArrayUtils.toPrimitive(message));
    }
}
