package core.bluetooth;

/**
 * Created by Etienne on 08/02/2018.
 */
public interface IStreamable {

    boolean startStream();

    boolean stopStream();

    interface DataStreamListener{
        void onNewData();
    }
}
