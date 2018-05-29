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
import core.eeg.signalprocessing.MBTEEGPacket;
import core.oad.OADEvent;
import engine.MbtClient;
import engine.MbtClientEvents;
import eventbus.EventBusManager;
import eventbus.events.EEGDataIsReady;
import features.MbtFeatures;

import static features.ScannableDevices.MELOMIND;

public class MainActivity extends AppCompatActivity implements MbtClientEvents, MbtClientEvents.EegListener, MbtClientEvents.BatteryListener, MbtClientEvents.DeviceInfoListener, MbtClientEvents.HeadsetStatusListener, MbtClientEvents.StateListener, MbtClientEvents.OADEventListener, MbtClientEvents.MailboxEventListener  {

    private static String TAG = MainActivity.class.getName();

    private MbtClient client;
    private EventBusManager eventBusManager;
    private TextView eegTextView;
    private TextView nbChannelsTextView;
    private TextView nbSamplesTextView;
    private TextView sampleRateTextView;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        eegTextView = (TextView) findViewById(R.id.data);
        nbChannelsTextView = (TextView) findViewById(R.id.nbChannels);
        nbSamplesTextView = (TextView) findViewById(R.id.nbSamples);
        sampleRateTextView = (TextView) findViewById(R.id.sampleRate);

//        //Listeners declarations
//        MbtClientEvents.EegListener eegListener = null;
//        MbtClientEvents.BatteryListener batteryListener = null;
//        MbtClientEvents.StateListener stateListener = null;
//        MbtClientEvents.DeviceInfoListener deviceInfoListener = null;
//        MbtClientEvents.HeadsetStatusListener headsetStatusListener = null;
//        MbtClientEvents.BandwidthListener bandwidthListener = null;
//        MbtClientEvents.OADEventListener oadEventListener = null;
//        MbtClientEvents.MailboxEventListener mailboxEventListener = null;
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
        client.testEEGpackageClient(); //todo remove if tests are successful
        eventBusManager = new EventBusManager(this);

    }

    @Override
    public void onStateChanged(@NonNull BtState newState) {
        Log.i(TAG, "Received new state" + newState);
    }

    @Override
    public void onNewPackets(MBTEEGPacket mbteegPackets, int nbChannels, int sampleRate, int nbSamples) {
        Log.i(TAG, "Received new EEG packet on UI");
        for (int i=0; i<mbteegPackets.getChannelsData().size();i++){
            for (int j=0; j<mbteegPackets.getChannelsData().get(i).size();j++){
                Log.i(TAG, " "+mbteegPackets.getChannelsData().get(i).get(j));
            }
        }
        eegTextView.setText(Arrays.toString(mbteegPackets.getChannelsData().toArray())); //update text on UI with the EEG packet ready
        nbChannelsTextView.setText(Integer.toString(nbChannels));
        sampleRateTextView.setText(Integer.toString(sampleRate));
        nbSamplesTextView.setText(Integer.toString(nbSamples));
    }

    @Override
    public void onBatteryChanged(int level) {
        Log.i(TAG, "Received battery level on UI" + level);
    }

    @Override
    public void onSaturationStateChanged(int newState) {
        Log.i(TAG, "Received saturation on UI" + newState);
    }

    @Override
    public void onNewDCOffsetMeasured(int dcOffset) {
        Log.i(TAG, "Received dc offset on UI" + dcOffset);
    }

    @Override
    public void onFwVersionReceived(String fwVersion) {
        Log.i(TAG, "Received hwVersion on UI" + fwVersion);
    }

    @Override
    public void onHwVersionReceived(String hwVersion) {
        Log.i(TAG, "Received hwVersion on UI" + hwVersion);

    }

    @Override
    public void onSerialNumberReceived(String serialNumber) {
        Log.i(TAG, "Received serial number on UI" + serialNumber);
    }

    @Override
    public void onOadEvent(OADEvent event, int value) {
        Log.i(TAG, "Received OAD Event on UI" + value);
    }

    @Override
    public void onDeviceReady(boolean ready) {
        Log.i(TAG, "Received device ready state on UI" + ready);
    }

    @Override
    public void onProgressUpdate(int progress) {
        Log.i(TAG, "Received progress on UI" + progress);
    }

    @Override
    public void onPacketTransferComplete(boolean state) {
        Log.i(TAG, "Received packet transfer complete state on UI" + state);
    }

    @Override
    public void onOADComplete(boolean result) {
        Log.i(TAG, "Received oad complete state on UI" + result);

    }

    @Override
    public void onMailBoxEvent(byte eventCode, Object eventValues) {
        Log.i(TAG, "Received mail box event on UI" + eventCode +"/n Values: "+eventValues.toString() );
    }

    @Subscribe
    public void onEvent(final EEGDataIsReady event) {
        Log.i(TAG, "event EEGDataIsReady received" );
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onNewPackets(new MBTEEGPacket(event.getMatrix(), null, event.getStatus(), System.currentTimeMillis()), event.getNbChannels(), event.getSampleRate(), event.getMatrix().get(0).size());
            }
        });
    }
}
