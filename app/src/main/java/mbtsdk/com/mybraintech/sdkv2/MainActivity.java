package mbtsdk.com.mybraintech.sdkv2;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import config.MbtConfig;
import core.bluetooth.BtState;
import core.device.DCOffsets;
import core.device.SaturationEvent;
import core.eeg.storage.MbtEEGPacket;
import engine.DeviceInfoListener;
import engine.EegListener;
import engine.DeviceStatusListener;
import engine.MbtClient;
import engine.MbtClientEvents;
import engine.StateListener;
import eventbus.events.ClientReadyEEGEvent;

import static features.ScannableDevices.MELOMIND;

public class MainActivity extends AppCompatActivity{

    private static String TAG = MainActivity.class.getName();

    private MbtClient client;
    private TextView eegTextView;
    private TextView dcoffsettextView;

    Timer timer;

    boolean start = true;
    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            if(start){
                client.startstream(false, eegListener, deviceStatusListener);
                start = false;
            }else{
                client.stopStream();
                start = true;
            }
        }
    };

    private DeviceStatusListener deviceStatusListener = new DeviceStatusListener() {
        @Override
        public void onSaturationStateChanged(SaturationEvent saturation) {
            Toast.makeText(MainActivity.this, "Saturation detected with code " + saturation.getSaturationCode(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNewDCOffsetMeasured(DCOffsets dcOffsets) {
            dcoffsettextView.setText("offset 1 : " + dcOffsets.getOffset()[0] + " offset 2 : " + dcOffsets.getOffset()[1]);
        }

        @Override
        public void onError(String reason) {

        }
    };


    private DeviceInfoListener deviceInfoListener = new DeviceInfoListener() {
        @Override
        public void onBatteryChanged(String newLevel) {
            Log.i(TAG, "new battery level is " + newLevel);
        }

        @Override
        public void onFwVersionReceived(String fwVersion) {
            Log.i(TAG, "firmware version is " + fwVersion);
            Toast.makeText(MainActivity.this, "FwVersion is " + fwVersion, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onHwVersionReceived(String hwVersion) {
            Log.i(TAG, "hardware version is " + hwVersion);
        }

        @Override
        public void onSerialNumberReceived(String serialNumber) {
            Log.i(TAG, "serial number is " + serialNumber);
        }

        @Override
        public void onError(String reason) {
            Log.e(TAG, reason);

        }
    };

    private StateListener stateListener = new StateListener() {
        @Override
        public void onStateChanged(@NonNull BtState newState) {
            Log.i(TAG,"newstate is " + newState.toString());
            if(newState == BtState.CONNECTED_AND_READY){
                Log.d(TAG, "connected and ready");
                client.readFwVersion(deviceInfoListener);
                client.readHwVersion(deviceInfoListener);
                client.readSerialNumber(deviceInfoListener);
                client.readBattery(0, deviceInfoListener);

                client.startstream(false, eegListener, deviceStatusListener);

//                timer = new Timer();
//                timer.schedule(timerTask ,0, 30000);

            } else if (newState == BtState.DISCONNECTED || newState == BtState.SCAN_TIMEOUT || newState == BtState.INTERNAL_FAILURE){
                if(timer != null)
                    timer.cancel();
                Log.i(TAG, "restarting");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        client.connectBluetooth("melo_1010876553", stateListener);
                    }
                },10000);

            }
        }

        @Override
        public void onError(String reason) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        eegTextView = findViewById(R.id.data); //todo remove when tests are ok
        dcoffsettextView = findViewById(R.id.dcoffset);
//        getSupportActionBar().setHomeButtonEnabled(true);
//        getSupportActionBar().

        MbtConfig.setScannableDevices(MELOMIND);//TODO remove when test are ok

        client = MbtClient.init(getApplicationContext());

        client.connectBluetooth("melo_1010876553", stateListener);

//        client.startstream(true, new EegListener() {
//            @Override
//            public void onError(String reason) {
//
//            }
//
//            @Override
//            public void onNewPackets(MBTEEGPacket mbteegPackets, int nbChannels, int nbSamples, int sampleRate) {
//
//            }
//        });
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


    private EegListener eegListener = new EegListener() {
        @Override
        public void onNewPackets(MbtEEGPacket eegPackets) {
            Log.i(TAG, "eegPacket size is " + eegPackets.getChannelsData().size());
        }

        @Override
        public void onError(String reason) {
            Toast.makeText(MainActivity.this, reason, Toast.LENGTH_SHORT).show();
        }
    };


}
