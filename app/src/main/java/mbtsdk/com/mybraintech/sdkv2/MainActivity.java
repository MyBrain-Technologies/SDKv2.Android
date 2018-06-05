package mbtsdk.com.mybraintech.sdkv2;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
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

        MbtClientEvents.EegListener eegListener = new MbtClientEvents.EegListener() {
                      @Override
            public void onNewPackets(ArrayList<MBTEEGPacket> mbteegPackets, int nbChannels, int nbSamples, int sampleRate) {
                Log.i(TAG,"New EEG packets");
                //the SDK user can do what he wants now with the EEG data stored in the mbteegPackets
            }

            @Override
            public void onError() {
                Log.e(TAG,"Error during EEG processing");
            }
        };

        MbtConfig.setScannableDevices(MELOMIND);//TODO remove when tests are ok

        client = MbtClient.init(getApplicationContext(),eegListener);

        /*client.startstream(true, new MbtClientEvents.EegListener() {
            @Override
            public void onNewPackets(MBTEEGPacket mbteegPackets, int nbChannels, int nbSamples, int sampleRate) {

            }

            @Override
            public void onError() {

            }
        });*/
    }

}
