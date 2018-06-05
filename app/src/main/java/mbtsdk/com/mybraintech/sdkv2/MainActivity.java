package mbtsdk.com.mybraintech.sdkv2;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import org.greenrobot.eventbus.Subscribe;

import java.util.Arrays;

import config.MbtConfig;
import core.bluetooth.BtState;
import core.eeg.storage.MBTEEGPacket;
import core.oad.OADEvent;
import engine.MbtClient;
import engine.MbtClientEvents;
import eventbus.EventBusManager;
import eventbus.events.ClientReadyEEGEvent;

import static features.ScannableDevices.MELOMIND;

public class MainActivity extends AppCompatActivity{

    private static String TAG = MainActivity.class.getName();

    private MbtClient client;
    private EventBusManager eventBusManager; //warning : do not remove this attribute (consider unsused by the IDE, but actually used)
    private TextView eegTextView;

    static {
        System.loadLibrary("native-lib");    // Used to load the 'native-lib' library on application startup.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        eegTextView = findViewById(R.id.data); //todo remove when tests are ok

        MbtClientEvents mbtClientEvents = new MbtClientEvents() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return super.equals(obj);
            }

            @Override
            protected Object clone() throws CloneNotSupportedException {
                return super.clone();
            }

            @Override
            public String toString() {
                return super.toString();
            }

            @Override
            protected void finalize() throws Throwable {
                super.finalize();
            }
        };
        MbtConfig.setScannableDevices(MELOMIND);//TODO remove when test are ok

        client = MbtClient.init(getApplicationContext(),mbtClientEvents);

        client.testEEGpackageClient(); //todo remove when tests are ok
        eventBusManager = new EventBusManager(this);


        client.startstream(true, new MbtClientEvents.EegListener() {
            @Override
            public void onNewPackets(MBTEEGPacket mbteegPackets, int nbChannels, int nbSamples, int sampleRate) {

            }

            @Override
            public void onError() {

            }
        });
    }


    /**
     * onEvent is called by the Event Bus when a ClientReadyEEGEvent event is posted
     * This event is published by {@link core.eeg.MbtEEGManager}:
     * this manager handles EEG data acquired by the headset
     * Creates a new MBTEEGPacket instance when the raw buffer contains enough data
     * @param event contains data transmitted by the publisher : here it contains the converted EEG data matrix, the status, the number of acquisition channels and the sampling rate
     */
    @Subscribe
    public void onEvent(final ClientReadyEEGEvent event) { //warning : do not remove this attribute (consider unsused by the IDE, but actually used)
        Log.i(TAG, "event ClientReadyEEGEvent received" );
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() { //update textView on MainActivity UI
                //onNewPackets(new MBTEEGPacket(event.getMatrix(), null, event.getStatus(), System.currentTimeMillis()), event.getNbChannels(), event.getSampleRate(), event.getMatrix().get(0).size());
            }
        });
    }
}
