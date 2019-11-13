package mbtsdk.com.mybraintech.sdkv2;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Objects;

import core.bluetooth.StreamState;
import core.device.event.DCOffsetEvent;
import core.device.event.SaturationEvent;
import core.bluetooth.BtState;
import core.device.model.MbtDevice;
import core.eeg.storage.MbtEEGPacket;
import engine.MbtClient;

import config.StreamConfig;
import engine.SimpleRequestCallback;
import engine.clientevents.BaseError;
import engine.clientevents.DeviceStatusListener;
import engine.clientevents.BluetoothStateListener;
import engine.clientevents.DeviceBatteryListener;
import engine.clientevents.EegListener;
import engine.clientevents.OADStateListener;
import features.MbtDeviceType;
import features.MbtFeatures;
import utils.AsyncUtils;
import utils.LogUtils;

import static utils.MatrixUtils.invertFloatMatrix;

public class DeviceActivity extends AppCompatActivity {

    private static int MAX_NUMBER_OF_DATA_TO_DISPLAY = 500;
    private static String TAG = DeviceActivity.class.getName();

    private MbtClient sdkClient;
    private MbtDevice device;

    private String deviceName;
    private String deviceQrCode;
    private MbtDeviceType deviceType = MbtDeviceType.MELOMIND;
    private TextView deviceNameTextView;

    private LineChart eegGraph;
    private LineData eegLineData;

    private long eegDataCounter = 0;
    private TextView channelsQuality;

    private Button startStopStreamingButton;

    private Button disconnectButton;

    private Button readBatteryButton;
    private String lastReadBatteryLevel = "";

    private boolean isConnected = false;
    private boolean isStreaming = false;

    private BluetoothStateListener bluetoothStateListener;
    private DeviceStatusListener<BaseError> deviceStatusListener;
    private DeviceBatteryListener deviceInfoListener;

    private EegListener<BaseError> eegListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        sdkClient = MbtClient.getClientInstance();

        initConnectionStateListener();
        initDeviceInfoListener();
        initDeviceStatusListener();
        initEegListener();

        initToolBar();
        initChannelsTextView();
        initDeviceNameTextView();
        initDisconnectButton();
        initReadBatteryButton();
        initStartStopStreamingButton();

        sdkClient.setConnectionStateListener(bluetoothStateListener);
    }

    private void initEegListener() {
        eegListener = new EegListener<BaseError>() {
            @Override
            public void onError(BaseError error, String additionalInfo) {
                LogUtils.w(TAG, "error : " + error.getMessage());
                Toast.makeText(DeviceActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                if (isStreaming) {
                    stopStream();
                    updateStreaming();
                }
            }

            @Override
            public void onNewPackets(@NonNull final MbtEEGPacket mbtEEGPackets) {
                Log.d(TAG, " onNewPacket");
                if(eegGraph == null)
                    AsyncUtils.executeAsync(new Runnable() {
                        @Override
                        public void run() {
                            sdkClient.requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
                                @Override
                                public void onRequestComplete(MbtDevice object) {
                                    device = object;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            initEegGraph(device.getNbChannels(), device.getSampRate());
                                        }
                                    });
                                }
                            });
                        }
                    });


                if (invertFloatMatrix(mbtEEGPackets.getChannelsData()) != null)
                    mbtEEGPackets.setChannelsData(invertFloatMatrix(mbtEEGPackets.getChannelsData()));

                if (isStreaming) {
                    if (eegGraph != null) {
                        addEegDataToGraph(mbtEEGPackets);

                        StringBuilder qualities = new StringBuilder();
                        for (int qualityTextView = 0 ; qualityTextView < device.getNbChannels() ; qualityTextView++){
                            qualities.append(getString(R.string.quality))
                                    .append(qualityTextView+1)
                                    .append( ": ")
                                    .append( mbtEEGPackets.getQualities() != null ?
                                            mbtEEGPackets.getQualities().get(qualityTextView)
                                            : "--")
                                    .append(" ");
                        }
                        channelsQuality.setText(qualities.toString());

                    }
                }
            }

            @Override
            public void onNewStreamState(@NonNull StreamState streamState) {

            }
        };
    }

    private void initDeviceStatusListener() {
        deviceStatusListener = new DeviceStatusListener<BaseError>() {

            @Override
            public void onError(BaseError error, String additionalInfo) {

            }

            @Override
            public void onSaturationStateChanged(SaturationEvent saturation) {
                notifyUser("Saturation: " + saturation.getSaturationCode());
            }

            @Override
            public void onNewDCOffsetMeasured(DCOffsetEvent dcOffsets) {
                notifyUser("Offset: " + Arrays.toString(dcOffsets.getOffset()));
            }
        };
    }

    private void initDeviceInfoListener() {
        deviceInfoListener = new DeviceBatteryListener() {
            @Override
            public void onBatteryLevelReceived(String newLevel) {
                lastReadBatteryLevel = newLevel;
                notifyUser("Current battery level : " + lastReadBatteryLevel + " %");
            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                notifyUser(getString(R.string.error_read_battery));
            }
        };
    }

    private void initConnectionStateListener() {
        bluetoothStateListener = new BluetoothStateListener() {
            @Override
            public void onNewState(BtState newState, MbtDevice device) {
            }

            @Override
            public void onDeviceConnected(MbtDevice device) {
                LogUtils.i(TAG, " device connected");
                isConnected = true;
            }

            @Override
            public void onDeviceDisconnected(MbtDevice device) {
                LogUtils.i(TAG, " device disconnected");
                isConnected = false;
                returnOnPreviousActivity();
            }

            public void onError(BaseError error, String additionalInfo) {
                notifyUser(error.getMessage() + (additionalInfo != null ? additionalInfo : ""));
            }
        };
    }

    private void initDisconnectButton() {

        disconnectButton = findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnOnPreviousActivity();
                if (isStreaming)
                    stopStream();
                sdkClient.disconnectBluetooth();

            }
        });
    }

    private void initDeviceNameTextView() {
        deviceNameTextView = findViewById(R.id.deviceNameTextView);

        sdkClient.requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
            @Override
            public void onRequestComplete(MbtDevice connectedDevice) {
                device = connectedDevice;
                if (device != null) {
                    deviceName = device.getProductName();
                    deviceQrCode = device.getExternalName();

                    deviceNameTextView.setText(deviceName + (deviceType != null && deviceType.equals(MbtDeviceType.VPRO) ? "" : " | " + deviceQrCode));
                }
            }
        });

    }

    private void initReadBatteryButton() {
        readBatteryButton = findViewById(R.id.readBatteryButton);
        readBatteryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deviceInfoListener != null)
                    sdkClient.readBattery(deviceInfoListener);
            }
        });
    }

    private void initChannelsTextView() {
        channelsQuality = findViewById(R.id.channels_quality);
    }

    private void initStartStopStreamingButton() {
        startStopStreamingButton = findViewById(R.id.startStopStreamingButton);
        startStopStreamingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isStreaming) { //streaming is not in progress : starting streaming
                    startStream(new StreamConfig.Builder(eegListener)
                            .setNotificationPeriod(MbtFeatures.DEFAULT_CLIENT_NOTIFICATION_PERIOD)
                            .createForDevice(deviceType));
                } else { //streaming is in progress : stopping streaming
                    stopStream(); // set false to isStreaming et null to the eegListener
                }
                updateStreaming(); //update the UI text in both case according to the new value of isStreaming
            }
        });
    }

    /**
     * Updates the streaming state boolean and the Stream button text
     * The Stream button text is changed into into "Stop Streaming" if streaming is started
     * or into "Start Streaming" if streaming is stopped
     */
    private void updateStreaming() {
        startStopStreamingButton.setText((isStreaming ? R.string.stop_streaming : R.string.start_streaming));
    }

    public void initEegGraph(int nbChannels, int sampRate) {
        if(eegGraph == null)
            eegGraph = findViewById(R.id.eegGraph);

        eegLineData = new LineData();

        for (int channel=0 ; channel < nbChannels ; channel++){

            LineDataSet channelDataSet = new LineDataSet(new ArrayList<Entry>(sampRate), getString(R.string.channel)+(channel+1));
            channelDataSet.setDrawValues(false);
            channelDataSet.disableDashedLine();
            channelDataSet.setDrawCircleHole(false);
            channelDataSet.setDrawCircles(false);
            channelDataSet.setColor(Color.rgb((int) (Math.random()*255), (int) (Math.random()*255), (int) (Math.random()*255)
            ));
            channelDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            eegLineData.addDataSet(channelDataSet);
        }

        eegGraph.setData(eegLineData);

        XAxis xAxis = eegGraph.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(Color.rgb(255, 192, 56));
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(1f); // one hour

        eegGraph.setDoubleTapToZoomEnabled(false);
//        final int MAXIMUM_VOLTAGE = 300;
//        final int MINIMUM_VOLTAGE = -300;
//        eegGraph.setVisibleYRange(MINIMUM_VOLTAGE, MAXIMUM_VOLTAGE, YAxis.AxisDependency.LEFT);// limit the number of visible entries

        final int TIME_WINDOW = 2; //2 seconds
        MAX_NUMBER_OF_DATA_TO_DISPLAY = TIME_WINDOW * sampRate;
        eegGraph.setVisibleXRangeMaximum(MAX_NUMBER_OF_DATA_TO_DISPLAY);// limit the number of visible entries

        eegGraph.setAutoScaleMinMaxEnabled(true);
        eegGraph.getAxisLeft().setDrawGridLines(false);
        eegGraph.getAxisLeft().setDrawLabels(true);
        eegGraph.getAxisRight().setDrawLabels(true);
        eegGraph.getAxisRight().setDrawGridLines(false);
        eegGraph.getXAxis().setDrawGridLines(false);

        eegGraph.invalidate();
    }

    private boolean channelsHasTheSameNumberOfData(int nbChannels, ArrayList<ArrayList<Float>> data) {
        boolean hasTheSameNumberOfData = true;

        int size = data.get(1).size();
        for (int i = 0; i < nbChannels; i++) {
            if (data.get(i).size() != size) {
                hasTheSameNumberOfData = false;
            }
        }
        return hasTheSameNumberOfData;
    }

    private void addEntry(int nbChannels, ArrayList<ArrayList<Float>> channelData) {

        LineData data = eegGraph.getData();
        if (data != null) {

            if (channelData.size() < nbChannels) {
                throw new IllegalStateException("Incorrect matrix size, one or more channel are missing");
            } else {
                if (channelsHasTheSameNumberOfData(nbChannels,channelData)) {
                    for (int currentEegData = 0; currentEegData < channelData.get(0).size(); currentEegData++) { //for each number of eeg data
                        for (int currentChannel = 0; currentChannel < nbChannels; currentChannel++) {
                            data.addEntry(new Entry(data.getDataSets().get(currentChannel).getEntryCount(), channelData.get(currentChannel).get(currentEegData) * 1000000), currentChannel);
                        }

                    }
                } else {
                    throw new IllegalStateException("Channels do not have the same amount of data");
                }
            }

            data.notifyDataChanged();
            eegGraph.notifyDataSetChanged();// let the chart know it's data has changed
            eegGraph.setVisibleXRangeMaximum(MAX_NUMBER_OF_DATA_TO_DISPLAY);// limit the number of visible entries
            eegGraph.moveViewToX((data.getEntryCount() / 2));// move to the latest entry

        } else {
            throw new IllegalStateException("Graph not correctly initialized");
        }
    }

    private void addEegDataToGraph(MbtEEGPacket mbtEEGPackets) {
        int size = mbtEEGPackets.getChannelsData().get(0).size();
        eegDataCounter += size;
        addEntry(mbtEEGPackets.getChannelsData().size(), mbtEEGPackets.getChannelsData());
    }

    private void notifyUser(String message) {
        Toast.makeText(DeviceActivity.this, message, Toast.LENGTH_LONG).show();
    }

    public void initToolBar() {
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.logo);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.light_blue)));
        }
    }

    private void startStream(StreamConfig streamConfig) {
        isStreaming = true;
        sdkClient.startStream(streamConfig);
    }

    private void stopStream() {
        isStreaming = false;
        sdkClient.stopStream();
    }

    private void returnOnPreviousActivity() {
        LogUtils.i(TAG, " return on previous activity");
        notifyUser(getString(R.string.disconnected_headset));
        eegListener = null;
        bluetoothStateListener = null;
        finish();
        Intent intent = new Intent(DeviceActivity.this, HomeActivity.class);
        intent.putExtra(HomeActivity.PREVIOUS_ACTIVITY_EXTRA, DeviceActivity.TAG);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        sdkClient.disconnectBluetooth();
        eegListener = null;
        bluetoothStateListener = null;
        sdkClient.setConnectionStateListener(null);
        returnOnPreviousActivity();
    }
}
