package mbtsdk.com.mybraintech.sdkv2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
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
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.EntryXComparator;

import java.util.ArrayList;

import core.eeg.storage.MbtEEGPacket;
import engine.MbtClient;

import engine.StreamConfig;
import engine.clientevents.EegListener;
import utils.MatrixUtils;

public class DeviceActivity extends AppCompatActivity {

    private static String TAG = DeviceActivity.class.getName();

    private MbtClient client;

    private String deviceName;
    private TextView deviceNameTextView;

    private LineChart eegGraph;
    private LineData eegLineData = new LineData();
    private ArrayList<ArrayList<Float>> bufferedChartData;
    private long chartCounter  = 0;
    private TextView channel1Quality;
    private TextView channel2Quality;

    private Button startStopStreamingButton;

    private Button disconnectButton;

    private boolean isStreaming = false;

    private EegListener eegListener = new EegListener() {

        @Override
        public void onNewPackets(final MbtEEGPacket mbtEEGPackets) {
            Log.i(TAG,"New EEG packets"+mbtEEGPackets.toString());
            //the SDK user can do what he wants now with the EEG data stored in the MbtEEGPackets

            mbtEEGPackets.setChannelsData(MatrixUtils.invertFloatMatrix(mbtEEGPackets.getChannelsData()));
            if(eegGraph!=null){
                eegGraph.post(new Runnable() {
                    @Override
                    public void run() {
                        eegGraph.post(new Runnable() {
                            @Override
                            public void run() {
                                chartCounter++;

                                if(chartCounter <= 2)
                                    addEntry(eegGraph, mbtEEGPackets.getChannelsData(), mbtEEGPackets.getStatusData());
                                else
                                    updateEntry(eegGraph,mbtEEGPackets.getChannelsData(), bufferedChartData, mbtEEGPackets.getStatusData());

                                bufferedChartData = mbtEEGPackets.getChannelsData();
                                bufferedChartData.add(0, mbtEEGPackets.getStatusData());
                            }
                        });
                    }
                });

                channel1Quality.post(new Runnable() {
                    @Override
                    public void run() {
                        channel1Quality.setText("CHANNEL 1 QC : " + (mbtEEGPackets.getQualities() != null ? mbtEEGPackets.getQualities().get(0) : "--"));
                    }
                });
                channel2Quality.post(new Runnable() {
                    @Override
                    public void run() {
                        channel2Quality.setText("CHANNEL 2 QC : " + (mbtEEGPackets.getQualities() != null ? mbtEEGPackets.getQualities().get(1) : "--"));
                    }
                });
            }
        }
        @Override
        public void onError(Exception exception) {
            exception.printStackTrace();
        }


    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        initChannelsTextView();
        initDeviceNameTextView();
        initDisconnectButton();

        client = MbtClient.getClientInstance();

        initStartStopStreamingButton();

        //initEegGraph();

    }

    private void initDisconnectButton() {
        disconnectButton = findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isStreaming) {
                    client.stopStream();
                    isStreaming = false;
                }

                if( true/*client.disconnectBluetooth()*/){ //disconnect succeeded
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
                    updateStreaming(true);
                    client.startStream(new StreamConfig.Builder(eegListener).create());
                    notifyUser("Starting streaming");
                }else { //streaming is in progress : stopping streaming
                    client.stopStream();
                    notifyUser("Stopping streaming");
                    updateStreaming(false);
                }
            }
        });
    }

    /**
     * Updates the streaming state boolean and the Stream button text
     * The Stream button text is changed into into "Stop Streaming" if streaming is started
     * or into "Start Streaming" if streaming is stopped
     * @param newIsStreaming
     */
    private void updateStreaming(boolean newIsStreaming){
        isStreaming = newIsStreaming;
        startStopStreamingButton.setText((isStreaming ? R.string.stop_streaming : R.string.start_streaming));
    }

    public void initEegGraph(){
        eegGraph = findViewById(R.id.eegGraph);
        LineDataSet dataSetChan1 = new LineDataSet(new ArrayList<Entry>(250), "Status");
        LineDataSet dataSetChan2 = new LineDataSet(new ArrayList<Entry>(250), "Channel 1");
        LineDataSet dataSetChan3 = new LineDataSet(new ArrayList<Entry>(250), "Channel 2");

        dataSetChan1.setLabel("STYM");
        dataSetChan1.setDrawValues(false);
        dataSetChan1.disableDashedLine();
        dataSetChan1.setDrawCircleHole(false);
        dataSetChan1.setDrawCircles(false);
        dataSetChan1.setColor(Color.GREEN);
        dataSetChan1.setDrawFilled(true);
        dataSetChan1.setFillColor(Color.GREEN);
        dataSetChan1.setFillAlpha(40);
        dataSetChan1.setAxisDependency(YAxis.AxisDependency.RIGHT);

        dataSetChan2.setLabel("Channel 1");
        dataSetChan2.setDrawValues(false);
        dataSetChan2.disableDashedLine();
        dataSetChan2.setDrawCircleHole(false);
        dataSetChan2.setDrawCircles(false);
        dataSetChan2.setColor(Color.RED);
        dataSetChan2.setAxisDependency(YAxis.AxisDependency.LEFT);

        dataSetChan3.setLabel("Channel 2");
        dataSetChan3.setDrawValues(false);
        dataSetChan3.disableDashedLine();
        dataSetChan3.setDrawCircleHole(false);
        dataSetChan3.setDrawCircles(false);
        dataSetChan3.setColor(Color.BLUE);
        dataSetChan3.setAxisDependency(YAxis.AxisDependency.LEFT);

        // setting chart
//        eegLineData.removeDataSet(2);
//        eegLineData.removeDataSet(1);
//        eegLineData.removeDataSet(0);
        eegLineData.addDataSet(dataSetChan1);
        eegLineData.addDataSet(dataSetChan2);
        eegLineData.addDataSet(dataSetChan3);

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
        eegGraph.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {

            }
        });

        eegGraph.invalidate();
    }


    private void addEntry(LineChart mChart, ArrayList<ArrayList<Float>> channelData, ArrayList<Float> statusData) {

        LineData data = mChart.getData();
        if (data != null) {

            if(channelData.size()<2){
                throw new IllegalStateException("Incorrect matrix size, one or more channel are missing");
            }else{
                if(channelData.get(0).size() == channelData.get(1).size()){
                    for(int i = 0; i< channelData.get(0).size(); i++){
                        if(statusData != null)
                            data.addEntry(new Entry(data.getDataSets().get(0).getEntryCount(), statusData.get(i).floatValue()), 0);

                        data.addEntry(new Entry(data.getDataSets().get(1).getEntryCount(), channelData.get(0).get(i).floatValue()*1000000), 1);
                        data.addEntry(new Entry(data.getDataSets().get(2).getEntryCount(), channelData.get(1).get(i).floatValue()*1000000), 2);
                    }
                }else{
                    throw new IllegalStateException("Channels do not have the same amount of data");
                }

            }
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(500);

            // move to the latest entry
            mChart.moveViewToX((data.getEntryCount()/2));

        }else{
            throw new IllegalStateException("Graph not correctly initialized");
        }
    }

    private void updateEntry(LineChart mChart,ArrayList<ArrayList<Float>> channelData, ArrayList<ArrayList<Float>> bufferedData, ArrayList<Float> statusData) {

        LineData data = mChart.getData();
        //System.currentTimeMillis();
        if (data != null) {
            if(statusData != null)
                data.getDataSets().get(0).clear();
            data.getDataSets().get(1).clear();
            data.getDataSets().get(2).clear();
            if(channelData.size()<2){
                throw new IllegalStateException("Incorrect matrix size, one or more channel are missing");
            }else{
                if(bufferedData.get(1).size() == bufferedData.get(2).size()){
                    for(int i = 0; i< bufferedData.get(1).size(); i++){
                        if(statusData != null)
                            data.addEntry(new Entry(data.getDataSets().get(0).getEntryCount(), bufferedData.get(0).get(i).floatValue()), 0);

                        data.addEntry(new Entry(data.getDataSets().get(1).getEntryCount(), bufferedData.get(1).get(i).floatValue()*1000000), 1);
                        data.addEntry(new Entry(data.getDataSets().get(2).getEntryCount(), bufferedData.get(2).get(i).floatValue()*1000000), 2);
                    }
                }else{
                    throw new IllegalStateException("Channels do not have the same amount of data");
                }

                if(channelData.get(0).size() == channelData.get(1).size()){
                    for(int i = 0; i< channelData.get(0).size(); i++){
                        if(statusData != null)
                            data.addEntry(new Entry(data.getDataSets().get(0).getEntryCount(), statusData.get(i).floatValue()), 0);

                        data.addEntry(new Entry(data.getDataSets().get(1).getEntryCount(), channelData.get(0).get(i).floatValue()*1000000), 1);
                        data.addEntry(new Entry(data.getDataSets().get(2).getEntryCount(), channelData.get(1).get(i).floatValue()*1000000), 2);
                    }
                }else{
                    throw new IllegalStateException("Channels do not have the same amount of data");
                }

            }
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(500);

            // move to the latest entry
            mChart.moveViewToX((data.getEntryCount()/2));

        }else{
            throw new IllegalStateException("Graph not correctly initialized");
        }
    }

    private void notifyUser(String message){
        Toast.makeText(DeviceActivity.this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(DeviceActivity.this,HomeActivity.class));
    }
}
