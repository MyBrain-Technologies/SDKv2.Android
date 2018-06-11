package mbtsdk.com.mybraintech.sdkv2;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;

import config.MbtConfig;
import core.eeg.storage.MbtEEGPacket;
import engine.MbtClient;
import engine.MbtClientEvents;
import eventbus.EventBusManager;

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
            public void onNewPackets(ArrayList<MbtEEGPacket> mbtEEGPackets) {
                Log.i(TAG,"New EEG packets"+mbtEEGPackets.toString());
                //the SDK user can do what he wants now with the EEG data stored in the MbtEEGPackets
                eegTextView.setText(mbtEEGPackets.toString()); //update text on UI with the EEG packet ready
            }

            @Override
            public void onError(@NonNull Exception exception) {
                Log.e(TAG, "Error during EEG processing");
            }

        };

        MbtConfig.setScannableDevices(MELOMIND);//TODO remove when tests are ok

        client = MbtClient.init(getApplicationContext(),eegListener);

        client.startStream(false);
        client.testEEGpackageClientBLE(); //todo remove if tests are successful

    }

}
