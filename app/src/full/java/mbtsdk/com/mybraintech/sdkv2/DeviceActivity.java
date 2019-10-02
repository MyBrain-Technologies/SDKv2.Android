package mbtsdk.com.mybraintech.sdkv2;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import command.CommandInterface;
import config.SynchronisationConfig;
import core.bluetooth.StreamState;
import core.bluetooth.BtState;
import core.device.event.DCOffsetEvent;
import core.device.event.SaturationEvent;
import core.device.model.MbtDevice;
import core.eeg.storage.Feature;
import core.eeg.storage.MbtEEGPacket;
import engine.MbtClient;

import config.StreamConfig;
import engine.clientevents.BaseError;
import engine.clientevents.DeviceStatusListener;
import engine.clientevents.BluetoothStateListener;
import engine.clientevents.DeviceBatteryListener;
import engine.clientevents.EegListener;
import features.MbtDeviceType;
import features.MbtFeatures;
import utils.LogUtils;

import static utils.MatrixUtils.invertFloatMatrix;

public class
DeviceActivity extends AppCompatActivity {

    private static final int MAX_NUMBER_OF_DATA_TO_DISPLAY = 500;
    private static String TAG = DeviceActivity.class.getName();

    private MbtClient client;

    private String deviceName;
    private String deviceQrCode;
    private MbtDeviceType deviceType;
    private TextView deviceNameTextView;

    private LineChart eegGraph;
    private LineData eegLineData;

    private LineDataSet channel1;
    private LineDataSet channel2;

    private long eegDataCounter = 0;
    private TextView channel1Quality;
    private TextView channel2Quality;

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
        client = MbtClient.getClientInstance();

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
        initEegGraph();

        client.setConnectionStateListener(bluetoothStateListener);
    }

    private void initEegListener() {
        eegListener = new EegListener<BaseError>() {
            @Override
            public void onError(BaseError error, String additionnalInfo) {
                LogUtils.w(TAG, "error : " + error.getMessage());
                Toast.makeText(DeviceActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                if (isStreaming) {
                    stopStream();
                    updateStreaming();
                }
            }

            @Override
            public void onNewPackets(@NonNull final MbtEEGPacket mbtEEGPackets) {
                if (invertFloatMatrix(mbtEEGPackets.getChannelsData()) != null)
                    mbtEEGPackets.setChannelsData(invertFloatMatrix(mbtEEGPackets.getChannelsData()));

                if (isStreaming) {
                    if (eegGraph != null) {
                        addEegDataToGraph(mbtEEGPackets);

                        channel1Quality.setText(getString(R.string.channel_1_qc) + ((mbtEEGPackets.getQualities() != null && mbtEEGPackets.getQualities().get(0) != null) ? mbtEEGPackets.getQualities().get(0) : " -- "));
                        channel2Quality.setText(getString(R.string.channel_2_qc) + ((mbtEEGPackets.getQualities() != null && mbtEEGPackets.getQualities().get(1) != null) ? mbtEEGPackets.getQualities().get(1) : " -- "));
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
            public void onError(BaseError error, String additionnalInfo) {
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

            public void onError(BaseError error, String additionnalInfo) {
                notifyUser(error.getMessage() + (additionnalInfo != null ? additionnalInfo : ""));
            }
        };
    }

    private void initDisconnectButton() {
        disconnectButton = findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //returnOnPreviousActivity();
                if (isStreaming)
                    stopStream();
                client.disconnectBluetooth();
            }
        });
    }

    private void initDeviceNameTextView() {
        deviceNameTextView = findViewById(R.id.deviceNameTextView);
        if (getIntent().hasExtra(HomeActivity.DEVICE_NAME_EXTRA)) {
            deviceName = Objects.requireNonNull(getIntent().getExtras()).getString(HomeActivity.DEVICE_NAME_EXTRA, "");
        }
        if (getIntent().hasExtra(HomeActivity.DEVICE_QR_CODE_EXTRA)) {
            deviceQrCode = Objects.requireNonNull(getIntent().getExtras()).getString(HomeActivity.DEVICE_QR_CODE_EXTRA, "");
        }
        if (getIntent().hasExtra(HomeActivity.DEVICE_TYPE_EXTRA)) {
            deviceType = (MbtDeviceType) Objects.requireNonNull(getIntent().getExtras()).getSerializable(HomeActivity.DEVICE_TYPE_EXTRA);
        }

        deviceNameTextView.setText(deviceName + " | " + deviceQrCode);
    }

    private void initReadBatteryButton() {
        readBatteryButton = findViewById(R.id.readBatteryButton);
        readBatteryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deviceInfoListener != null)
                    client.readBattery(deviceInfoListener);
            }
        });
    }

    private void initChannelsTextView() {
        channel1Quality = findViewById(R.id.channel_1_quality);
        channel2Quality = findViewById(R.id.channel_2_quality);
        channel1Quality.setText(getString(R.string.channel_1_qc) + " -- ");
        channel2Quality.setText(getString(R.string.channel_2_qc) + " -- ");
    }

    private String ipAddress = "";
    private int port = 8000;

    private void initStartStopStreamingButton() {
        startStopStreamingButton = findViewById(R.id.startStopStreamingButton);
        startStopStreamingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isStreaming) { //streaming is not in progress : starting streaming

                    final AlertDialog.Builder alertDialogMain = new AlertDialog.Builder(DeviceActivity.this);
                    final AlertDialog.Builder alertDialogAddress = new AlertDialog.Builder(DeviceActivity.this);
                    final AlertDialog.Builder alertDialogPort = new AlertDialog.Builder(DeviceActivity.this);
                    final EditText ipAddressEditText = new EditText(DeviceActivity.this);
                    ipAddressEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                    final EditText portEditText = new EditText(DeviceActivity.this);
                    portEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT);
                    ipAddressEditText.setLayoutParams(lp);
                    portEditText.setLayoutParams(lp);
                    //portEditText.setText(port);
                    alertDialogMain.setCancelable(true);
                    alertDialogMain.setTitle("Stream over OSC ?");
                    alertDialogMain.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            alertDialogPort.setView(portEditText);
                            alertDialogPort.setTitle("Enter the port");
                            alertDialogPort.setNeutralButton("NEXT", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    port = Integer.parseInt(portEditText.getText().toString());
                                    alertDialogAddress.setView(ipAddressEditText);
                                    alertDialogAddress.setTitle("Enter the IP address");
                                    alertDialogAddress.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ipAddress = ipAddressEditText.getText().toString();
                                            startStream(new StreamConfig.Builder(eegListener)
                                                    .setNotificationPeriod(MbtFeatures.DEFAULT_CLIENT_NOTIFICATION_PERIOD)
                                                    .useQualities()
                                                    .streamOverOSC(new SynchronisationConfig.OSC.Builder()
                                                            .ipAddress(ipAddress)
                                                            .streamFeature(Feature.ALPHA_POWER)
                                                            .port(port)
                                                            .create())
                                                    .createForDevice(deviceType));
                                            updateStreaming();

                                        }
                                    });
                                    alertDialogAddress.show();

                                }
                            });
                            alertDialogPort.show();
                        }
                    });
                    alertDialogMain.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            startStream(new StreamConfig.Builder(eegListener)
                                    .setNotificationPeriod(MbtFeatures.DEFAULT_CLIENT_NOTIFICATION_PERIOD)
                                    .useQualities()
                                    .createForDevice(deviceType));
                            updateStreaming();
                        }
                    });
                    alertDialogMain.show();

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

    public void initEegGraph() {
        eegGraph = findViewById(R.id.eegGraph);

        channel1 = new LineDataSet(new ArrayList<Entry>(250), getString(R.string.channel1));
        channel2 = new LineDataSet(new ArrayList<Entry>(250), getString(R.string.channel2));

        channel1.setDrawValues(false);
        channel1.disableDashedLine();
        channel1.setDrawCircleHole(false);
        channel1.setDrawCircles(false);
        channel1.setColor(Color.rgb(3, 32, 123));
        channel1.setAxisDependency(YAxis.AxisDependency.LEFT);

        channel2.setDrawValues(false);
        channel2.disableDashedLine();
        channel2.setDrawCircleHole(false);
        channel2.setDrawCircles(false);
        channel2.setColor(Color.rgb(99, 186, 233));
        channel2.setAxisDependency(YAxis.AxisDependency.LEFT);

        eegLineData = new LineData();

        eegLineData.addDataSet(channel1);
        eegLineData.addDataSet(channel2);

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
        eegGraph.setAutoScaleMinMaxEnabled(true);
        eegGraph.getAxisLeft().setDrawGridLines(false);
        eegGraph.getAxisLeft().setDrawLabels(true);
        eegGraph.getAxisRight().setDrawLabels(true);
        eegGraph.getAxisRight().setDrawGridLines(false);
        eegGraph.getXAxis().setDrawGridLines(false);

        eegGraph.invalidate();
    }

    private boolean channelsHasTheSameNumberOfData(ArrayList<ArrayList<Float>> data) {
        boolean hasTheSameNumberOfData = true;

        int size = data.get(1).size();
        for (int i = 0; i < MbtFeatures.getNbChannels(deviceType); i++) {
            if (data.get(i).size() != size) {
                hasTheSameNumberOfData = false;
            }
        }
        return hasTheSameNumberOfData;
    }

    private void addEntry(ArrayList<ArrayList<Float>> channelData) {

        LineData data = eegGraph.getData();
        if (data != null) {

            if (channelData.size() < MbtFeatures.getNbChannels(deviceType)) {
                throw new IllegalStateException("Incorrect matrix size, one or more channel are missing");
            } else {
                if (channelsHasTheSameNumberOfData(channelData)) {
                    for (int currentEegData = 0; currentEegData < channelData.get(0).size(); currentEegData++) { //for each number of eeg data
                        for (int currentChannel = 0; currentChannel < MbtFeatures.getNbChannels(deviceType); currentChannel++) { //todo vpro
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

        eegDataCounter += mbtEEGPackets.getChannelsData().get(0).size();
        addEntry(mbtEEGPackets.getChannelsData());
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

    private void updateSerialNumber(String serialNumber) {
        client.updateSerialNumber(serialNumber, new CommandInterface.CommandCallback<byte[]>() {
            @Override
            public void onResponseReceived(CommandInterface.MbtCommand request, byte[] response) {

            }

            @Override
            public void onError(CommandInterface.MbtCommand request, BaseError error, String additionalInfo) {

            }

            @Override
            public void onRequestSent(CommandInterface.MbtCommand request) {
                Log.d(TAG," Request complete. Returned response : "+request);
            }
        });
    }

    private void startStream(StreamConfig streamConfig) {
        isStreaming = true;
        client.startStream(streamConfig);
    }

    private void stopStream() {
        isStreaming = false;
        client.stopStream();
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
        client.disconnectBluetooth();
        eegListener = null;
        bluetoothStateListener = null;
        client.setConnectionStateListener(null);
        returnOnPreviousActivity();
    }
}
