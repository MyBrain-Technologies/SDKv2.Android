package core.synchronisation;
import java.util.ArrayList;
import config.SynchronisationConfig;


public class MbtLSLStreamer extends IStreamer<Object> {


    public MbtLSLStreamer(SynchronisationConfig config) {
        super(config.streamRawEEG(), config.streamQualities(), config.getFeaturesToStream());
    }

    @Override
    protected void stream(Object message) {

    }

    @Override
    protected Object initStreamRequest(ArrayList<Float> dataToStream, String address) {
        return null;
    }
}
