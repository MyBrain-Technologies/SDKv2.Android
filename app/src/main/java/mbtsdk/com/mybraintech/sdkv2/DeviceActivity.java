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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

import core.bluetooth.BtState;
import core.eeg.storage.MbtEEGPacket;
import engine.MbtClient;

import engine.StreamConfig;
import engine.clientevents.ConnectionStateListener;
import engine.clientevents.EEGException;
import engine.clientevents.EegListener;
import features.MbtFeatures;
import utils.MatrixUtils;

public class DeviceActivity extends AppCompatActivity {

    private static String TAG = DeviceActivity.class.getName();
    private final int INDEX_STATUS = 0;


    private MbtClient client;

    private String deviceName;
    private TextView deviceNameTextView;

    private LineChart eegGraph;
    private LineData eegLineData;
    private ArrayList<ArrayList<Float>> bufferedChartData;
    private long chartCounter  = 0;
    private TextView channel1Quality;
    private TextView channel2Quality;

    private Button startStopStreamingButton;

    private Button disconnectButton;

    private boolean isStreaming = false;
    private BtState currentState;

    private EegListener eegListener = new EegListener<EEGException>() {

        @Override
        public void onError(EEGException exception) {
            Toast.makeText(DeviceActivity.this, exception.toString(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNewPackets(final MbtEEGPacket mbtEEGPackets) {
            Log.i(TAG,"New EEG packets"+mbtEEGPackets.toString());
            //the SDK user can do what he wants now with the EEG data stored in the MbtEEGPackets

            mbtEEGPackets.setChannelsData(MatrixUtils.invertFloatMatrix(mbtEEGPackets.getChannelsData()));

            if(eegGraph!=null){
                addEegDataToGraph(mbtEEGPackets);

                channel1Quality.post(new Runnable() {
                    @Override
                    public void run() {
                        //channel1Quality.setText(getString(R.string.channel_1_qc) + (mbtEEGPackets.getQualities() != null ? mbtEEGPackets.getQualities().get(0) : "--"));
                    }
                });
                channel2Quality.post(new Runnable() {
                    @Override
                    public void run() {
                        //channel2Quality.setText(getString(R.string.channel_2_qc) + (mbtEEGPackets.getQualities() != null ? mbtEEGPackets.getQualities().get(1) : "--"));
                    }
                });
            }
        }

    };

    private ConnectionStateListener connectionStateListener = new ConnectionStateListener() {
        @Override
        public void onStateChanged(@NonNull BtState newState) {
            currentState = newState;
            Log.e(TAG, "Current state updated Device Activity "+newState);
        }

        @Override
        public void onError(Exception expection) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        initTopBar();

        initChannelsTextView();
        initDeviceNameTextView();
        initDisconnectButton();

        client = MbtClient.getClientInstance();

        initStartStopStreamingButton();

        initEegGraph();

    }

    private void initDisconnectButton() {
        disconnectButton = findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isStreaming) {
                    stopStream();
                }
                client.disconnectBluetooth();
                if(currentState.equals(BtState.DISCONNECTED)){
                    setConnectionStateListener(null);
                    startActivity(new Intent(DeviceActivity.this,HomeActivity.class)); //back to the home page
                    finish();
                }else{ //disconnect failed
                    notifyUser(getString(R.string.disconnect_failed)+deviceName);
                }
            }
        });
    }

    private void initDeviceNameTextView() {
        deviceNameTextView = findViewById(R.id.deviceNameTextView);
        if(getIntent().hasExtra(HomeActivity.DEVICE_NAME)){
            deviceName = getIntent().getExtras().getString(HomeActivity.DEVICE_NAME,"");
            deviceNameTextView.setText(deviceName);
        }
    }

    private void initChannelsTextView() {
        channel1Quality = findViewById(R.id.channel_1_quality);
        channel2Quality = findViewById(R.id.channel_2_quality);
    }

    private void initStartStopStreamingButton(){
        startStopStreamingButton= findViewById(R.id.startStopStreamingButton);
        startStopStreamingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isStreaming) { //streaming is not in progress : starting streaming
                    startStream(new StreamConfig.Builder(eegListener).setNotificationPeriod(MbtFeatures.DEFAULT_CLIENT_NOTIFICATION_PERIOD).create());
                    notifyUser("Starting streaming");
                }else { //streaming is in progress : stopping streaming
                    stopStream(); // set false to isStreaming et null to the eegListener
                    notifyUser("Stopping streaming");
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
    private void updateStreaming(){
        startStopStreamingButton.setText((isStreaming ? R.string.stop_streaming : R.string.start_streaming));
    }

    public void initEegGraph(){
        eegGraph = findViewById(R.id.eegGraph);
        ArrayList<Entry> data = new ArrayList<>(MbtFeatures.getSampleRate());
        data.add(new Entry(0, 0)); //init with a 0 value in order to avoid a crash

        //LineDataSet status = new LineDataSet(data, "Status");
        LineDataSet channel1 = new LineDataSet(data, "Channel 1");
        LineDataSet channel2 = new LineDataSet(data, "Channel 2");

        /*status.setLabel("STYM");
        status.setDrawValues(false);
        status.disableDashedLine();
        status.setDrawCircleHole(false);
        status.setDrawCircles(false);
        status.setColor(Color.GREEN);
        status.setDrawFilled(true);
        status.setFillColor(Color.GREEN);
        status.setFillAlpha(40);
        status.setAxisDependency(YAxis.AxisDependency.RIGHT);*/

        channel1.setLabel("Channel 1");
        channel1.setDrawValues(false);
        channel1.disableDashedLine();
        channel1.setDrawCircleHole(false);
        channel1.setDrawCircles(false);
        channel1.setColor(Color.BLACK);
        channel1.setAxisDependency(YAxis.AxisDependency.LEFT);

        channel2.setLabel("Channel 2");
        channel2.setDrawValues(false);
        channel2.disableDashedLine();
        channel2.setDrawCircleHole(false);
        channel2.setDrawCircles(false);
        channel2.setColor(Color.MAGENTA);
        channel2.setAxisDependency(YAxis.AxisDependency.LEFT);

        eegLineData = new LineData(/*status,*/channel1,channel2);

        eegGraph.setData(eegLineData);
        // this.chart.setAutoScaleMinMaxEnabled(true);

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
        eegGraph.getAxisRight().setAxisMinimum(0f);
        eegGraph.getAxisRight().setAxisMaximum(1f);
        eegGraph.getXAxis().setDrawGridLines(false);

        //eegGraph.getXAxis().setDrawLabels(true);

        /*chart.getAxisLeft().setAxisMaxValue(-10000f);
        chart.getAxisLeft().setAxisMaxValue(-10000f);*/


        eegGraph.invalidate();
    }

    private void addEntry(LineChart mChart, ArrayList<ArrayList<Float>> channelData, ArrayList<Float> statusData) {

        LineData data = mChart.getData();
        if (data != null) {

            if(channelData.size()< MbtFeatures.getNbChannels()){
                throw new IllegalStateException("Incorrect matrix size, one or more channel are missing");
            }else{
                if(channelsHasTheSameNumberOfData(channelData)){
                    for(int i = 0; i< channelData.get(0).size(); i++){ //for each number of eeg data
                        /*if(statusData != null && statusData.size()>0)
                            data.addEntry(new Entry(data.getDataSets().get(INDEX_STATUS).getEntryCount(), statusData.get(i)), INDEX_STATUS);*/
                        for (int channelIndex = 0; channelIndex < MbtFeatures.getNbChannels() ; channelIndex++){
                            data.addEntry(new Entry(data.getDataSets().get(channelIndex).getEntryCount(), channelData.get(channelIndex).get(i) *1000000),channelIndex);
                        }
                    }
                }else{
                    throw new IllegalStateException("Channels do not have the same amount of data");
                }
            }
            data.notifyDataChanged();

            mChart.notifyDataSetChanged();// let the chart know it's data has changed
            mChart.setVisibleXRangeMaximum(500);// limit the number of visible entries
            mChart.moveViewToX((data.getEntryCount()/2));// move to the latest entry

        }else{
            throw new IllegalStateException("Graph not correctly initialized");
        }
    }

    private boolean channelsHasTheSameNumberOfData(ArrayList<ArrayList<Float>> data){
        boolean hasTheSameNumberOfData = true;
        int size = data.get(0).size();
        for (int i = 0 ; i < MbtFeatures.getNbChannels() ; i++){
            if(data.get(i).size() != size){
                hasTheSameNumberOfData = false;
            }
        }
        return hasTheSameNumberOfData;
    }

    private void updateEntry(LineChart mChart,ArrayList<ArrayList<Float>> channelData, ArrayList<ArrayList<Float>> bufferedData, ArrayList<Float> statusData) {

        LineData lineData = mChart.getLineData();
        //System.currentTimeMillis();
        if (lineData != null) {
            /*if(statusData != null)
                lineData.getDataSets().get(INDEX_STATUS).clear();*/
            for (ILineDataSet dataSet : lineData.getDataSets()){
                dataSet.clear();
            }
            if(channelData.size()< MbtFeatures.getNbChannels()){
                throw new IllegalStateException("Incorrect matrix size, one or more channel are missing");
            }else{
                if(channelsHasTheSameNumberOfData(bufferedData)){
                    for(int i = 0; i< bufferedData.get(0).size(); i++){ //250 loop

                        /*if(statusData != null)
                            lineData.addEntry(new Entry(lineData.getDataSets().get(INDEX_STATUS).getEntryCount(), bufferedData.get(0).get(i)), 0);*/
                        Log.e(TAG,"channel 1 "+ lineData.getDataSetByIndex(0).getEntryCount() );
                        if(lineData.getDataSetByIndex(0).getEntryCount()>0){
                            for (int k=0; k< lineData.getDataSetByIndex(0).getEntryCount();k++){
                                Log.e(TAG,"value "+ lineData.getDataSetByIndex(0).getEntriesForXValue(k));
                            }
                        }
                        for (int channelIndex = 0; channelIndex < MbtFeatures.getNbChannels() ; channelIndex++){
                            Entry e = new Entry(lineData.getDataSets().get(channelIndex).getEntryCount(), bufferedData.get(channelIndex).get(i)*1000000);
                            lineData.getDataSetByIndex(channelIndex).addEntry(new Entry(lineData.getDataSets().get(channelIndex).getEntryCount(), bufferedData.get(channelIndex).get(i)*1000000));
                            Log.e(TAG,"added entry"+ e.toString() );

                        }

                    }
                }else{
                    throw new IllegalStateException("Channels do not have the same amount of data");
                }

                if(channelsHasTheSameNumberOfData(channelData)){
                    for(int i = 0; i< channelData.get(0).size(); i++){
                        /*if(statusData != null)
                            data.addEntry(new Entry(data.getDataSets().get(0).getEntryCount(), statusData.get(i)), 0);*/

                        for (int channelIndex = 0; channelIndex < MbtFeatures.getNbChannels() ; channelIndex ++){
                            lineData.getDataSetByIndex(channelIndex).addEntry(new Entry(lineData.getDataSets().get(channelIndex).getEntryCount(), channelData.get(channelIndex).get(i)*1000000));
                        }
                    }
                }else{
                    throw new IllegalStateException("Channels do not have the same amount of data");
                }
            }
            lineData.notifyDataChanged();

            mChart.notifyDataSetChanged();// let the chart know it's data has changed
            mChart.setVisibleXRangeMaximum(500); // limit the number of visible entries
            mChart.moveViewToX((lineData.getEntryCount()/2));// move the view to the latest entry to avoid manual scrolling
        }else{
            throw new IllegalStateException("Graph not correctly initialized");
        }
    }

    private void addEegDataToGraph(MbtEEGPacket mbtEEGPackets) {
        chartCounter++;
        if(chartCounter <= 2)
            addEntry(eegGraph, mbtEEGPackets.getChannelsData(), mbtEEGPackets.getStatusData());
        else
            updateEntry(eegGraph,mbtEEGPackets.getChannelsData(), bufferedChartData, mbtEEGPackets.getStatusData());

        bufferedChartData = mbtEEGPackets.getChannelsData();
        //bufferedChartData.add(0, mbtEEGPackets.getStatusData());
    }

    private void notifyUser(String message){
        Toast.makeText(DeviceActivity.this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        client.disconnectBluetooth();
        setEegListener(null);
        setConnectionStateListener(null);
        finish();
        startActivity(new Intent(DeviceActivity.this,HomeActivity.class));
    }

    public void initTopBar(){
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.logo);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.light_blue)));
        }
    }

    private void setEegListener(EegListener eegListener) {
        this.eegListener = eegListener;
    }

    private void setConnectionStateListener(ConnectionStateListener connectionStateListener) {
        this.eegListener = eegListener;
    }

    private void stopStream(){
        isStreaming = false;
        client.stopStream();
        setEegListener(null);
    }

    private void startStream(StreamConfig streamConfig){
        isStreaming = true;
        client.startStream(streamConfig);
    }
}
