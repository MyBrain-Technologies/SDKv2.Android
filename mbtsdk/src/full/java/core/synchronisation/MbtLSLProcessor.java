package core.synchronisation;

import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.util.Arrays;

import config.SynchronisationConfig;
import utils.LogUtils;

/**
 * The lab streaming layer (LSL) is a system for the unified collection of measurement
 * time series in research experiments that handles both the networking,
 * time-synchronization, (near-) real-time access as well as optionally
 * the centralized collection, viewing and disk recording of the data.
 */
public class MbtLSLProcessor extends AbstractStreamer<Float[], LSL.StreamOutlet, SynchronisationConfig.LSL> {

    MbtLSLProcessor(SynchronisationConfig.LSL config) {
        super(config);
    }

    @Override
    protected void initStreamer(SynchronisationConfig.LSL config) {

    }

    @Override
    public Float[] initStreamRequest(String address, Object... dataToStream) {
        LSL.StreamInfo info = new LSL.StreamInfo(address.replace(ADDRESS_PREFIX, ""), /*biosignal.name().toLowerCase()*/"EEG",1, LSL.IRREGULAR_RATE, LSL.ChannelFormat.float32,"uid");
        try {
            streamer = new LSL.StreamOutlet(info);
        } catch(IOException ex) {
            LogUtils.e(this.getClass().getName(),"Unable to open LSL outlet. Have you added <uses-permission android:name=\"android.permission.INTERNET\" /> to your manifest file?");
        }
        return Arrays.copyOf(dataToStream,
                dataToStream.length,
                Float[].class);
    }

    @Override
    public void stream(Float[] message) {
        streamer.push_sample(ArrayUtils.toPrimitive(message));
    }
}
