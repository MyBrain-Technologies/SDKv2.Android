package mbtsdk.com.mybraintech.sdkv2;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.aakira.expandablelayout.ExpandableRelativeLayout;
import com.github.florent37.viewanimator.ViewAnimator;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mikhaellopez.circularfillableloaders.CircularFillableLoaders;
import com.woxthebox.draglistview.DragItem;
import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import config.ConnectionConfig;
import config.RecordConfig;
import config.StreamConfig;
import core.bluetooth.StreamState;
import core.device.event.DCOffsetEvent;
import core.device.event.SaturationEvent;
import core.device.model.MbtDevice;
import core.eeg.storage.MbtEEGPacket;
import core.recording.metadata.Comment;
import engine.MbtClient;
import engine.SimpleRequestCallback;
import engine.clientevents.BaseError;
import engine.clientevents.ConnectionStateListener;
import engine.clientevents.DeviceBatteryListener;
import engine.clientevents.DeviceStatusListener;
import engine.clientevents.EegListener;
import features.MbtAcquisitionLocations;
import features.MbtDeviceType;
import features.MbtFeatures;
import utils.AsyncUtils;
import utils.MatrixUtils;
import utils.MbtLock;

public class VProActivity extends AppCompatActivity implements ConnectionStateListener<BaseError>, EegListener<BaseError>, DeviceBatteryListener<BaseError>, DeviceStatusListener<BaseError> {

    private static String TAG = VProActivity.class.getName();

    private static final int NB_CHANNEL = 9;
    private static final boolean USE_OAD_IN_VPRO = false;
    private static final boolean SHOULD_KEEP_HISTORY = false;

    private static final String DIRECTORY = "MBT";

    private SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm:ss");

    private MbtClient sdkClient;

    private DragListView lv;
    private ChartDataAdapter cda;
    private ArrayList<LineData> dataList;

    private boolean isStreaming = false;
    private boolean isRecording = false;
    private Map<Integer, Boolean> freezeMap = new HashMap<>();

    private ProgressBar progressBar;

    private ImageButton settingsButton;

    private Button connectButton;
    private Button disconnectButton;

    private Button startStreamButton;
    private Button stopStreamButton;

    private Button startRecordButton;
    private Button stopRecordButton;

    private TextView fwVersionTextView;
    private TextView batteryView;
    private TextView timerStreamTextView;
    private TextView timerRecordTextView;

    private LinearLayout buttonLayout;
    private TextView idleTV;
    private TextView appliVersion;

    private TextView qualities;

    //Comments
    private FrameLayout commentLayout;
    private FloatingActionButton fabComment;
    private EditText commentEditText;
    private ImageButton commentSend;

    private long counter = 0L;
    private final int TIME_WINDOW = 2;

    private Dialog mOADDialog;
    private CircularFillableLoaders mLoader;

    ArrayList<ArrayList<Float>> bufferedMatrix;

    private String patientID = null;
    private String condition = null;

    //Advanced Features
    private CheckBox checkBox;
    private ExpandableRelativeLayout advancedLayout;
    private EditText patientIDEditText;
    private EditText conditionEditText;
    private Switch timerSwitch;
    private EditText timerEditText;
    private Spinner timerSpinner;

    private String currentTimerScale = null;
    private boolean useAdvancedFeatures = false;

    private boolean bEnableFilter = false;
    private int freqBound1 = 2;
    private int freqBound2 = 30;

    private boolean bEnableAutoScale = false;
    private float YMaxValue = 1;
    private float YMinValue = -1;

    String[] channelName = new String[NB_CHANNEL];

    Map<Integer, Integer> relativeChannelPositions;
    private ArrayList<Comment> comments;
    private String deviceName;

    @Override
    public void onBatteryLevelReceived(final String level) {
        batteryView.post(new Runnable() {
            @Override
            public void run() {
                batteryView.setText("Battery : " + level + " %");
            }
        });
    }

    @Override
    public void onDeviceConnected(MbtDevice device) {
        setState(appliState.CONNECTED);

        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //vpro.updateDeviceConfiguration(MBTConfig.locations, MBTConfig.references, MBTConfig.grounds);
                MBTConfig.loadConfig("F-R");
                sdkClient.readBattery(mbtsdk.com.mybraintech.sdkv2.VProActivity.this);
            }
        });
        t.setDaemon(true); // ça c'est important ça veut dire que le thread ne va pas vivre en dehors du process de l'app
        t.start();
    }

    @Override
    public void onDeviceDisconnected(MbtDevice device) {
        setState(appliState.DISCONNECTED);
    }


    @Override
    public void onSaturationStateChanged(SaturationEvent saturationEvent) {

    }

    @Override
    public void onNewDCOffsetMeasured(DCOffsetEvent dcOffsetEvent) {

    }

    @Override
    public void onNewPackets(@NonNull final MbtEEGPacket mbteegPackets) {
        if(isRecording){
            if(useAdvancedFeatures && timerSwitch.isChecked())
                nbPacketsToRecord--;
            else
                nbPacketsToRecord++;

            if(nbPacketsToRecord >= 0)
                timerRecordTextView.setText("Time : " + mFormat.format(new Date(nbPacketsToRecord*1000)));
            else
                stopRecordButton.performClick();
        }

        if(isStreaming) {
            nbPacketsStreamed++;
            if (nbPacketsStreamed >= 0)
                timerStreamTextView.setText("Time : " + mFormat.format(new Date(nbPacketsStreamed * 1000)));
        }
        setQualities(mbteegPackets.getQualities());

        if(mbteegPackets.getChannelsData() != null){

            int j = 0;
            float[] filteredData;
            final float[] floatArray = new float[mbteegPackets.getChannelsData().size()*TIME_WINDOW];
            final ArrayList<Float> resultFilteredData =  new ArrayList<Float>(mbteegPackets.getChannelsData().size()*TIME_WINDOW);
            counter++;

            mbteegPackets.setChannelsData(MatrixUtils.invertFloatMatrix(mbteegPackets.getChannelsData()));

            if(!freezeMap.get(0)){
                if(SHOULD_KEEP_HISTORY || counter <= 2)
                    addEntry(mbteegPackets.getStatusData(), dataList.get(0));
                else
                    updateEntry(mbteegPackets.getStatusData(), bufferedMatrix.get(0), dataList.get(0));

            }

            for(int i = 0; i < mbteegPackets.getChannelsData().size(); i++){

                if(((counter % 2) == 0) && (counter >=2) && bEnableFilter){
                    j = 0;

                    for (Float f : bufferedMatrix.get(i+1)){
                        floatArray[j++] = (f != null ? f : 0);
                    }

                    for (Float f : mbteegPackets.getChannelsData().get(i) ) {
                        floatArray[j++] = (f != null ? f : 0); // Or whatever default you want.
                    }

                    final MbtLock lock = new MbtLock();
                    AsyncUtils.executeAsync(new Runnable() {
                        @Override
                        public void run() {
                            sdkClient.bandpassFilter(freqBound1,freqBound2, mbteegPackets.getChannelsData().size()*TIME_WINDOW, floatArray,
                                    new SimpleRequestCallback<float[]>() {
                                        @Override
                                        public void onRequestComplete(float[] filteredData) {
                                            lock.setResultAndNotify(filteredData);
                                        }
                                    });
                        }
                    });
                    filteredData = (float[]) lock.waitAndGetResult(500);

                    resultFilteredData.clear();
                    for (float f : filteredData) {
                        resultFilteredData.add(Float.valueOf(f));
                    }

                }

                if(!freezeMap.get(i)){

                    LineData lineData = dataList.get(i + 1);
                    int channel = 0;
                    for (Map.Entry<Integer, MbtAcquisitionLocations> locationsEntry : MBTConfig.locations.entrySet()) {
                        if(channel == i){
                            lineData = dataList.get(locationsEntry.getKey() + 1);
                        }
                        channel++;
                    }

                    if(SHOULD_KEEP_HISTORY || counter <= 2) {
                        if (bEnableFilter) {
                            if((SHOULD_KEEP_HISTORY && counter % 2 == 0) || counter == 2){
                                addEntry(resultFilteredData, lineData);
                            }
                        } else {
                            addEntry(mbteegPackets.getChannelsData().get(i), lineData);
                        }
                    }

                    else {
                        if (bEnableFilter) {
                            if((counter % 2) == 0){
                                addEntryFilter(resultFilteredData, lineData);
                            }
                        } else if(bufferedMatrix != null){
                            updateEntry(mbteegPackets.getChannelsData().get(i), bufferedMatrix.get(i + 1), lineData);
                        }
                    }
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //addEntry(mLineChart, matrix);

                    cda.notifyDataSetChanged();
                }
            });

            if(bEnableFilter)
            {
//            bufferedMatrix = new ArrayList<>();//ArrayList<ArrayList<Float>>) filtredEegPackets.clone();
                bufferedMatrix = (ArrayList<ArrayList<Float>>) mbteegPackets.getChannelsData().clone();
            }else{
                bufferedMatrix = (ArrayList<ArrayList<Float>>) mbteegPackets.getChannelsData().clone();
            }

            bufferedMatrix.add(0, mbteegPackets.getStatusData());
        }
    }

    @Override
    public void onNewStreamState(@NonNull StreamState streamState) {

        if(streamState.equals(StreamState.STREAMING))
            flashStreamingButton();
        else
            Toast.makeText(mbtsdk.com.mybraintech.sdkv2.VProActivity.this, "STREAM " + streamState, Toast.LENGTH_LONG).show();

    }

    @Override
    public void onError(BaseError baseError, String s) {
        if (mAppliState.equals(appliState.CONNECTING))
            setState(appliState.DISCONNECTED);

        Toast.makeText(mbtsdk.com.mybraintech.sdkv2.VProActivity.this, "Error: " + baseError.getMessage(), Toast.LENGTH_LONG).show();

    }

    private enum appliState {DISCONNECTING, DISCONNECTED, CONNECTING, CONNECTED, STREAMING, RECORDING}
    private appliState mAppliState = appliState.DISCONNECTED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();

        sdkClient = MbtClient.getClientInstance();

        setContentView(R.layout.activity_vpro);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        relativeChannelPositions = new HashMap<>();

        lv = (DragListView) findViewById(R.id.listView1);
        lv.setNestedScrollingEnabled(false);
        lv.getRecyclerView().setVerticalScrollBarEnabled(false);

        lv.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        lv.setCanDragHorizontally(false);
        lv.setDragEnabled(true);
        lv.setDragListListener(new DragListView.DragListListener() {
            @Override
            public void onItemDragStarted(int position) {

            }

            @Override
            public void onItemDragging(int itemPosition, float x, float y) {

            }

            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {
                //Make sure drag has really occured
                if(toPosition != fromPosition){
                    //creating temp buffer
                    ArrayList<Integer> tempList = new ArrayList<Integer>(9);
                    for(int i = 0; i < 9; i++)
                        tempList.add(relativeChannelPositions.get(i));

                    relativeChannelPositions.put(toPosition, tempList.get(fromPosition));
                    tempList.remove(fromPosition);


                    for (int i = 0; i < NB_CHANNEL; i++){
                        if(i != toPosition){
                            relativeChannelPositions.put(i, tempList.get(0));
                            tempList.remove(0);
                        }
                    }

                }
                Log.d(TAG, "drag complete");

            }
        });

//        View header = getLayoutInflater().inflate(R.layout.header_layout, null, false);
//        lv.addHeaderView(header);


        settingsButton = (ImageButton) findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu settingsMenu = new PopupMenu(VProActivity.this, settingsButton);
                settingsMenu.getMenuInflater().inflate(R.menu.menu_scrolling, settingsMenu.getMenu());

                settingsMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()){
                            case R.id.action_filters:
                            case R.id.action_mtu:
                                AlertDialog.Builder builderFilter = new AlertDialog.Builder(mbtsdk.com.mybraintech.sdkv2.VProActivity.this);
                                builderFilter.setTitle("Enable Filter");
                                View filterView = getLayoutInflater().inflate(R.layout.enablefilter,null);
                                builderFilter.setView(filterView);

                                Switch switchEnableFilter = filterView.findViewById(R.id.switchFilter);
                                switchEnableFilter.setChecked(bEnableFilter);

                                switchEnableFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                                {
                                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                        bEnableFilter = isChecked;
                                    }
                                });

                                final EditText textFreqBound1 = (EditText)filterView.findViewById(R.id.textFreqBound1);
                                final EditText textFreqBound2 = (EditText)filterView.findViewById(R.id.textFreqBound2);

                                textFreqBound1.setText(String.valueOf(freqBound1));
                                textFreqBound2.setText(String.valueOf(freqBound2));

                                builderFilter.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        freqBound1 = Integer.parseInt(textFreqBound1.getText().toString());
                                        freqBound2 = Integer.parseInt(textFreqBound2.getText().toString());
                                    }
                                });

                                builderFilter.create();
                                builderFilter.show();
                                break;

                            case R.id.action_config:

                                final String[] configArray = getResources().getStringArray(R.array.config_name);
                                AlertDialog.Builder builder = new AlertDialog.Builder(mbtsdk.com.mybraintech.sdkv2.VProActivity.this);
                                builder.setTitle("Pick new config")
                                        .setItems(R.array.config_name, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, final int which) {
                                                if(mAppliState != appliState.RECORDING){
                                                    MBTConfig.loadConfig(configArray[which]);
                                                    initChannelsNames();

                                                    if(mAppliState == appliState.CONNECTED || mAppliState == appliState.STREAMING){
                                                        new Handler().post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                //MBTConfig.loadConfig(configArray[which]);
                                                                Toast.makeText(mbtsdk.com.mybraintech.sdkv2.VProActivity.this, "Config: "+ MBTConfig.getConfigAsString(), Toast.LENGTH_SHORT).show();

                                                            }
                                                        });
                                                        Log.d(TAG, "updating configuration");
                                                    }
                                                }else{
                                                    Toast.makeText(mbtsdk.com.mybraintech.sdkv2.VProActivity.this, "Can't change config while recording, try again later.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                builder.create();
                                builder.show();
                                break;
                            case R.id.action_oad:
                                Toast.makeText(mbtsdk.com.mybraintech.sdkv2.VProActivity.this, "Unavailable for the moment...", Toast.LENGTH_SHORT).show();
                                break;
                            case R.id.action_GraphScale:
                                AlertDialog.Builder builderScale = new AlertDialog.Builder(mbtsdk.com.mybraintech.sdkv2.VProActivity.this);
                                builderScale.setTitle("Enable scaling");
                                View scaleView = getLayoutInflater().inflate(R.layout.graph_scale,null);
                                builderScale.setView(scaleView);


                                Switch switchEnableAutoScale = scaleView.findViewById(R.id.switch_scale);
                                switchEnableAutoScale.setChecked(bEnableAutoScale);

                                switchEnableAutoScale.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                                {
                                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                        bEnableAutoScale = isChecked;
                                    }
                                });
                                final EditText textYmax = (EditText)scaleView.findViewById(R.id.editText_Ymax);
                                final EditText textYmin = (EditText)scaleView.findViewById(R.id.editText_Ymin);

                                builderScale.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if(!textYmax.getText().toString().isEmpty()){
                                            //YMaxValue = Float.parseFloat(textYmax.getText().toString());
                                            //YMinValue = Float.parseFloat(textYmin.getText().toString());
                                            YMaxValue = Integer.parseInt(textYmax.getText().toString());
                                            YMinValue = Integer.parseInt(textYmin.getText().toString());

                                        }else {
                                            YMaxValue = -1;
                                            YMinValue = -1;
                                        }

                                    }
                                });

                                builderScale.create();
                                builderScale.show();
                                break;
                        }
                        return true;
                    }
                });
                settingsMenu.show();
            }
        });




        MBTConfig.loadConfig("F-R");  //TODO get previous saved config (shared memories ? )
        initChannelsNames();

        initQualities();

        initializeCharts();

        for(int i = 0; i < NB_CHANNEL; i++){
            freezeMap.put(i, false);
        }

        if(USE_OAD_IN_VPRO) {
            mOADDialog = new Dialog(mbtsdk.com.mybraintech.sdkv2.VProActivity.this);
            mOADDialog.setCancelable(false);
            mOADDialog.setContentView(R.layout.oad_dialog_content);
            mOADDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            mLoader = (CircularFillableLoaders) mOADDialog.findViewById(R.id.circularFillableLoaders);
            mLoader.setProgress(100);
        }

        appliVersion = (TextView) findViewById(R.id.appliVersion);
        appliVersion.setText("V " + BuildConfig.VERSION_NAME);

        progressBar = (ProgressBar) findViewById(R.id.progress);

        buttonLayout = (LinearLayout) findViewById(R.id.layout_buttons);

        batteryView = (TextView) findViewById(R.id.battcheck);
        fwVersionTextView = (TextView) findViewById(R.id.fwVersion);
        timerStreamTextView = (TextView) findViewById(R.id.timerStream);
        timerRecordTextView = (TextView) findViewById(R.id.timerRecording);


        idleTV = (TextView) findViewById(R.id.idleTextView);

        connectButton = (Button) findViewById(R.id.connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "init vpro connection process");
                setState(appliState.CONNECTING);
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sdkClient.connectBluetooth(new ConnectionConfig.Builder(mbtsdk.com.mybraintech.sdkv2.VProActivity.this)
                                .deviceName(deviceName)
                        .createForDevice(MbtDeviceType.VPRO));

                    }
                });
                t.setDaemon(true);
                t.start();
            }

        });

        disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "init vpro disconnection process");
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(isRecording){
                            RecordConfig recordConfig = new RecordConfig.Builder(getApplicationContext())
                                    .acquisitionLocations(MBTConfig.getLocationsInElectricalOrder())
                                    .referenceLocations(MBTConfig.references)
                                    .groundLocations(MBTConfig.grounds)
                                    .projectName(getResources().getString(R.string.app_name))
                                    .folder(DIRECTORY)
                                    .duration((int) programmedNbPacketsToRecord)
                                    .subjectID(patientID)
                                    .condition(condition)
                                    .enableMultipleRecordings()
                                    .useExternalStorage()
                                    .headerComments(comments)
                                    .create();

                            sdkClient.stopRecord(recordConfig);
                            //vpro.writeJsonFile(getResources().getString(R.string.app_name), patientID, condition);
                            sdkClient.stopStream();
                        }else if(isStreaming)
                            sdkClient.stopStream();
                        sdkClient.disconnectBluetooth();
                    }
                });
                t.setDaemon(true); // ça c'est important ça veut dire que le thread ne va pas vivre en dehors du process de l'app
                t.start();

                setState(appliState.DISCONNECTING);


            }
        });


        startStreamButton = (Button) findViewById(R.id.startStreamingButton);
        startStreamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "init vpro streaming process");
                initializeCharts();
                comments = new ArrayList<>();
                qualities.setVisibility(View.VISIBLE);

                if(useAdvancedFeatures)
                    patientID = patientIDEditText.getText().toString();
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sdkClient.startStream(new StreamConfig.Builder(mbtsdk.com.mybraintech.sdkv2.VProActivity.this)
                                .useQualities()
                        .createForDevice(MbtDeviceType.VPRO));
                    }
                });
                t.setDaemon(true); // ça c'est important ça veut dire que le thread ne va pas vivre en dehors du process de l'app
                t.start();
                setState(appliState.STREAMING);
            }
        });

        stopStreamButton = (Button) findViewById(R.id.stopStreamingButton);
        stopStreamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "stopping vpro streaming process");
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(isRecording){
                            RecordConfig recordConfig = new RecordConfig.Builder(getApplicationContext())
                                    .acquisitionLocations(MBTConfig.getLocationsInElectricalOrder())
                                    .referenceLocations(MBTConfig.references)
                                    .groundLocations(MBTConfig.grounds)
                                    .projectName(getResources().getString(R.string.app_name))
                                    .folder(DIRECTORY)
                                    .duration((int) programmedNbPacketsToRecord)
                                    .subjectID(patientID)
                                    .condition(condition)
                                    .enableMultipleRecordings()
                                    .useExternalStorage()
                                    .headerComments(comments)
                                    .create();

                            sdkClient.stopRecord(recordConfig);
                        }
                        sdkClient.stopStream();

                        isStreaming = false;
                        isRecording = false;
                    }
                });
                t.setDaemon(true); // ça c'est important ça veut dire que le thread ne va pas vivre en dehors du process de l'app
                t.start();
                setState(appliState.CONNECTED);
            }
        });

        startRecordButton = (Button) findViewById(R.id.startRecordingButton);
        startRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(useAdvancedFeatures){
                    int multiplier = 0;
                    condition = conditionEditText.getText().toString();
                    if(timerSwitch.isChecked()) {
                        switch (currentTimerScale) {
                            case "sec":
                                multiplier = 1;
                                break;
                            case "min":
                                multiplier = 60;
                                break;
                            case "hours":
                                multiplier = 3600;
                                break;
                        }

                        programmedNbPacketsToRecord = multiplier * (!timerEditText.getText().toString().equals("") ? Integer.parseInt(timerEditText.getText().toString()) : 0);
                        if (programmedNbPacketsToRecord <= 0) {
                            timerSwitch.setChecked(false);
                        }
                    }
                }

                Log.i(TAG, "init vpro recording process");
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sdkClient.startRecord(getApplicationContext());
                    }
                });
                t.setDaemon(true); // ça c'est important ça veut dire que le thread ne va pas vivre en dehors du process de l'app
                t.start();
                setState(appliState.RECORDING);
            }
        });

        stopRecordButton = (Button) findViewById(R.id.stopRecordingButton);
        stopRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int duration =  programmedNbPacketsToRecord > 0 ?
                        (int) programmedNbPacketsToRecord : (int) nbPacketsToRecord;

                setState(appliState.STREAMING);
                Log.i(TAG, "stopping vpro recording process");
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        RecordConfig recordConfig = new RecordConfig.Builder(getApplicationContext())
                                .acquisitionLocations(MBTConfig.getLocationsInElectricalOrder())
                                .referenceLocations(MBTConfig.references)
                                .groundLocations(MBTConfig.grounds)
                                .projectName(getResources().getString(R.string.app_name))
                                .folder(DIRECTORY)
                                .duration(duration)
                                .subjectID(patientID)
                                .condition(condition)
                                .enableMultipleRecordings()
                                .useExternalStorage()
                                .headerComments(comments)
                                .create();

                        sdkClient.stopRecord(recordConfig);
                    }
                });
                t.setDaemon(true); // ça c'est important ça veut dire que le thread ne va pas vivre en dehors du process de l'app
                t.start();
            }
        });

        fabComment = (FloatingActionButton) findViewById(R.id.fab_comment);
        fabComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                commentLayout.setVisibility(View.VISIBLE);
                ViewAnimator.animate(commentLayout)
                        .slideBottom()
                        .andAnimate(fabComment)
                        .alpha(0f)
                        .duration(500)
                        .start();
                fabComment.setEnabled(false);

            }
        });

        commentLayout = (FrameLayout) findViewById(R.id.comment_content);

        commentEditText = (EditText) findViewById(R.id.comment_edittext);
        commentSend = (ImageButton) findViewById(R.id.comment_button);
        commentSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "comment send click");
                if(!commentEditText.getText().toString().equals("")){
                    saveNewComment(commentEditText.getText().toString());
                    if(commentLayout.isShown()) {
                        ViewAnimator.animate(commentLayout)
                                .slideTop()
                                .andAnimate(fabComment)
                                .alpha(1f)
                                .duration(500)
                                .start();
                        fabComment.setEnabled(true);
                        commentEditText.setText(null);
                        commentLayout.setVisibility(View.GONE);

                        View view2 = mbtsdk.com.mybraintech.sdkv2.VProActivity.this.getCurrentFocus();
                        if (view2 != null) {
                            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view2.getWindowToken(), 0);
                        }


                    }
                }
            }
        });

        checkBox = (CheckBox) findViewById(R.id.checkboxAdvancesFeatures);
        advancedLayout = (ExpandableRelativeLayout) findViewById(R.id.advancedFeaturesLayout);
        patientIDEditText = (EditText) findViewById(R.id.patientID);
        conditionEditText = (EditText) findViewById(R.id.condition);
        timerSwitch = (Switch) findViewById(R.id.switchTimer);
        timerEditText = (EditText) findViewById(R.id.timerEditText);
        timerSpinner = (Spinner) findViewById(R.id.spinnerTimer);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                advancedLayout.toggle();
                useAdvancedFeatures = b;
            }
        });

        timerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                timerSwitch.setChecked(b);
                timerEditText.setEnabled(b);
                timerSpinner.setEnabled(b);
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_timer, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timerSpinner.setAdapter(adapter);
        timerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                currentTimerScale = (String) adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //Initial state
        setState(appliState.DISCONNECTED);

        displayDialogForDeviceNumber(null);


    }

    private void flashStreamingButton(){
        stopStreamButton.setTextColor(getResources().getColor(R.color.light_pink));
        new Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                stopStreamButton.setTextColor(getResources().getColor(R.color.pink));
            }
        }, 500);

    }

    private void setQualities(ArrayList<Float> qualities) {
        StringBuilder qualitiesDisplayed = new StringBuilder();
        for (int qualityTextView = 0 ; qualityTextView < qualities.size() ; qualityTextView++){
            qualitiesDisplayed.append(getString(R.string.quality))
                    .append("(")
                    .append(channelName[qualityTextView+1])
                    .append(")")
                    .append( ": ")
                    .append( qualities != null ?
                            qualities.get(qualityTextView)
                            : "--")
                    .append("     ");
        }
        this.qualities.setText(qualitiesDisplayed.toString());

    }

    private void initQualities() {
        qualities = findViewById(R.id.qualities);
        //qualities.setVisibility(View.GONE);
    }

    private void saveNewComment(String comment) {
        if(comments == null)
            comments = new ArrayList<>();

        comments.add(new Comment(comment, System.currentTimeMillis(), null));
    }

    private void initChannelsNames() {
        for(int i = 0; i < MBTConfig.getLocationsInDisplayOrder().length+1; i++){ //getLocationsInDisplayOrder returns the locations listed in the electronic order

            channelName[i] = i == 0 ? "Status" : MBTConfig.locations.get(i-1).toString(); //locations returns a map where the index is the display order associated
            relativeChannelPositions.put(i,i);
        }
        if(cda != null)
            cda.notifyDataSetChanged();
    }

    /**
     * Initialize all charts with predefined data sets. This will instanciate the Adapter used for managing graphs.
     */

    private void initializeCharts() {
        dataList = new ArrayList<LineData>();

        for (int i = 0; i < NB_CHANNEL; i++) {
            dataList.add(generateDataSet(i + 1));
        }

        cda = new ChartDataAdapter(getApplicationContext(), dataList, R.layout.list_item_linechart, R.id.dragButton, false);
        //cda = new ChartDataAdapter(getApplicationContext(), dataList);
        lv.setAdapter(cda, true);
        lv.setCustomDragItem(new MyDragItem(getApplicationContext(), R.layout.list_item_linechart));
        lv.setDisableReorderWhenDragging(false);
    }


    /**
     * This method will show the dialog to allow the user to enter a device number.
     * @param errorMessage
     */

    private void displayDialogForDeviceNumber(String errorMessage) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(mbtsdk.com.mybraintech.sdkv2.VProActivity.this);
        final EditText input = new EditText(mbtsdk.com.mybraintech.sdkv2.VProActivity.this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        if(errorMessage != null){
            input.setError(errorMessage);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);
        alertDialog.setCancelable(false);
        alertDialog.setTitle("Enter VPro headset number");
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deviceName = MbtFeatures.VPRO_DEVICE_NAME_PREFIX + input.getText();
                idleTV.setText("Please connect "+deviceName);
            }
        });
        alertDialog.show();
    }

    private int[] mColorBuffer = { Color.BLACK, Color.BLUE, Color.rgb(0,100,0), Color.DKGRAY, Color.rgb(46,139,87), Color.MAGENTA, Color.RED, Color.GRAY, Color.rgb(255,69,0)};

    /**
     * Used as initialization module for data sets.
     * @param cnt
     * @return
     */
    private LineData generateDataSet(int cnt) {

        ArrayList<Entry> entries = new ArrayList<Entry>();

        entries.add(new Entry(0, 0));

        LineDataSet d;

        if(cnt == 1){
            d = new LineDataSet(entries, "Status");
        }
        else{
            d = new LineDataSet(entries, "" + channelName[cnt-1]);
        }
        d.setDrawCircles(false);
        d.setDrawCircleHole(false);
        d.setDrawValues(false);
        d.setColor(mColorBuffer[cnt-1]);

        LineData cd = new LineData();
        cd.addDataSet(d);
        return cd;
    }

    private long nbPacketsStreamed = 0 ;

    private long nbPacketsToRecord = 0 ;
    private long programmedNbPacketsToRecord = 0 ;

    /**
     * This method replace the current values displayed by the new ones. Use for non history mode
     * @param channelData
     * @param lineData
     */
    private void updateEntry(ArrayList<Float> channelData, ArrayList<Float> bufferedChannelData,  LineData lineData) {
        //Log.d(TAG, "start update");

        lineData.getDataSets().get(0).clear();

        for (Float aFloat : bufferedChannelData) {
            lineData.addEntry(new Entry(lineData.getEntryCount(), (aFloat == null) ? Float.NaN : aFloat), 0);
        }

        for (Float aFloat : channelData) {
            lineData.addEntry(new Entry(lineData.getEntryCount(), (aFloat == null) ? Float.NaN : aFloat), 0);
        }
        //Log.d(TAG, "stop update");
    }

    /**
     * This method adds all new entries to the different data sets. Use for history mode
     * @param channelData
     * @param lineData
     */
    private void addEntry(ArrayList<Float> channelData, LineData lineData){
        if(lineData.getDataSetCount() == 0) {
            LineDataSet set = new LineDataSet(new ArrayList<Entry>(250), "Channel");
            lineData.addDataSet(set);
        }

        for (Float aFloat : channelData) {
            //lineData.addEntry(new Entry(lineData.getEntryCount(), aFloat), 0);
            lineData.addEntry(new Entry(lineData.getEntryCount(), (aFloat == null) ? Float.NaN : aFloat), 0);
        }
    }

    private void addEntryFilter(ArrayList<Float> channelData, LineData lineData){
        lineData.getDataSets().get(0).clear();

        for (Float aFloat : channelData) {
            //lineData.addEntry(new Entry(lineData.getEntryCount(), aFloat), 0);
            lineData.addEntry(new Entry(lineData.getEntryCount(), (aFloat == null) ? Float.NaN : aFloat), 0);

        }
    }
    /**
     * Check whether matrix size is correct or not.
     * @param channelData
     * @return true if correct, false otherwise
     */
    private static boolean sizeChecked(ArrayList<ArrayList<Float>> channelData) {
        for (ArrayList<Float> floats : channelData) {
            if(floats.size() != 250)
                return false;
        }
        return true;
    }



    private void setState(final appliState state){
        mAppliState = state;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch(state) {
                    case CONNECTED:
                        checkBox.setVisibility(View.VISIBLE);

                        patientIDEditText.setEnabled(true);

                        progressBar.setVisibility(View.GONE);

                        connectButton.setVisibility(View.GONE);

                        disconnectButton.setVisibility(View.VISIBLE);

                        buttonLayout.setVisibility(View.VISIBLE);

                        idleTV.setVisibility(View.GONE);

                        stopRecordButton.setVisibility(View.GONE);

                        stopStreamButton.setVisibility(View.GONE);

                        startRecordButton.setVisibility(View.VISIBLE);
                        startRecordButton.setEnabled(false);
                        startRecordButton.setText("Start Recording");
                        startRecordButton.setTextColor(getResources().getColor(R.color.gray));

                        startStreamButton.setVisibility(View.VISIBLE);

                        if (nbPacketsStreamed > 0)
                            nbPacketsStreamed = 0;

                        if (isRecording) {
                            nbPacketsToRecord = 0;
                            programmedNbPacketsToRecord = 0;

                            fabComment.setVisibility(View.GONE);
                            patientIDEditText.setEnabled(false);

                            timerEditText.setEnabled(true);
                            timerSwitch.setEnabled(true);
                            timerSpinner.setEnabled(true);
                            conditionEditText.setEnabled(true);
                            checkBox.setEnabled(true);
                        }

                        isStreaming = false;
                        isRecording = false;

                        if (USE_OAD_IN_VPRO)
                            settingsButton.setVisibility(View.VISIBLE);

                        break;

                    case CONNECTING:
                    case DISCONNECTING:
                        connectButton.setVisibility(View.GONE);

                        progressBar.setVisibility(View.VISIBLE);

                        disconnectButton.setVisibility(View.GONE);

                        if (USE_OAD_IN_VPRO)
                            settingsButton.setVisibility(View.GONE);


                        break;

                    case STREAMING:

                        if (isRecording) {
                            fabComment.setVisibility(View.GONE);

                            stopRecordButton.setVisibility(View.GONE);

                            startRecordButton.setVisibility(View.VISIBLE);
                            startRecordButton.setEnabled(true);
                            startRecordButton.setText("Start Recording");

                            isRecording = false;

                        } else {
                            nbPacketsStreamed = 0;
                            counter = 0;
                        }

                        nbPacketsToRecord = 0;
                        programmedNbPacketsToRecord = 0;
                        timerStreamTextView.setText("Time : " + mFormat.format(new Date(nbPacketsStreamed*1000)));

                        patientIDEditText.setEnabled(false);

                        timerEditText.setEnabled(true);
                        timerSwitch.setEnabled(true);
                        timerSpinner.setEnabled(true);
                        conditionEditText.setEnabled(true);
                        checkBox.setEnabled(true);

                        startStreamButton.setVisibility(View.GONE);

                        stopStreamButton.setVisibility(View.VISIBLE);

                        startRecordButton.setEnabled(true);
                        startRecordButton.setTextColor(getResources().getColor(R.color.blue));

                        timerStreamTextView.setVisibility(View.VISIBLE);

                        timerRecordTextView.setVisibility(View.INVISIBLE);

                        isStreaming = true;

                        break;
                    case RECORDING:

                        fabComment.setVisibility(View.VISIBLE);

                        timerEditText.setEnabled(false);
                        timerSwitch.setEnabled(false);
                        timerSpinner.setEnabled(false);
                        conditionEditText.setEnabled(false);
                        checkBox.setEnabled(false);

                        if (!isStreaming) {
                            nbPacketsStreamed = 0;
                            startStreamButton.setVisibility(View.GONE);

                            stopStreamButton.setVisibility(View.VISIBLE);

                            isStreaming = true;
                        }

                        nbPacketsToRecord = (programmedNbPacketsToRecord > 0) ?
                                programmedNbPacketsToRecord : 0;

                        timerRecordTextView.setText("Time : " + mFormat.format(new Date(nbPacketsToRecord*1000)));

                        startRecordButton.setEnabled(false);
                        startRecordButton.setText("Starting...");

                        startRecordButton.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startRecordButton.setVisibility(View.GONE);
                            }
                        }, 2000);

                        stopRecordButton.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                stopRecordButton.setVisibility(View.VISIBLE);
                            }
                        }, 2000);

                        timerRecordTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                timerRecordTextView.setVisibility(View.VISIBLE);
                            }
                        });

                        isRecording = true;
                        break;
                    case DISCONNECTED:

                        counter = 0;
                        if (USE_OAD_IN_VPRO) {
                            settingsButton.setVisibility(View.GONE);
                        }

                        fabComment.setVisibility(View.GONE);
                        qualities.setVisibility(View.GONE);

                        if (advancedLayout.isExpanded()) {
                            advancedLayout.collapse();
                            checkBox.setChecked(false);
                        }


                        checkBox.setVisibility(View.GONE);

                        progressBar.setVisibility(View.GONE);

                        connectButton.setVisibility(View.VISIBLE);

                        disconnectButton.setVisibility(View.GONE);

                        buttonLayout.setVisibility(View.GONE);

                        idleTV.setVisibility(View.VISIBLE);

                        stopRecordButton.setVisibility(View.GONE);

                        stopStreamButton.setVisibility(View.GONE);

                        startRecordButton.setVisibility(View.VISIBLE);

                        startStreamButton.setVisibility(View.VISIBLE);

                        timerStreamTextView.setVisibility(View.INVISIBLE);

                        timerRecordTextView.setVisibility(View.INVISIBLE);

                        if (isStreaming)
                            nbPacketsStreamed = 0;
                        if (isRecording){
                            programmedNbPacketsToRecord = 0;
                            nbPacketsToRecord = 0;
                        }

                        isStreaming = false;
                        isRecording = false;
                    default:
                        break;
                }
            }
        });

    }

    public float getMax(ArrayList<Float> data){
        float max = -100;
        for (Float aDouble : data) {
            if(aDouble > max){
                max = aDouble;
            }
        }
        return max;
    }

    public float getMin(ArrayList<Float> data){
        float min = 100;
        for (Float aDouble : data) {
            if(aDouble < min){
                min = aDouble;
            }
        }
        return min;
    }


    @Override
    public void onBackPressed() {
        if(commentLayout.isShown()){
            ViewAnimator.animate(commentLayout)
                    .slideTop()
                    .andAnimate(fabComment)
                    .alpha(1f)
                    .duration(500)
                    .start();
            commentLayout.setVisibility(View.GONE);
            fabComment.setEnabled(true);
            View view = mbtsdk.com.mybraintech.sdkv2.VProActivity.this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }else{
            super.onBackPressed();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
//        if(isRecording || isStreaming)
//            disconnectButton.performClick();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if(isRecording || isStreaming)
            disconnectButton.performClick();
        super.onDestroy();
    }


    /**
     * Adapter used for displaying and refreshing all graphs.
     */
    class ChartDataAdapter extends DragItemAdapter<LineData, ChartDataAdapter.ViewHolder> {
        private int mGrabHandleId;
        private boolean mDragOnLongPress;
        private int mLayoutId;
        private Context context;


        //
        // private List<LineData> mData = new ArrayList<LineData>();

        public ChartDataAdapter(Context context, List<LineData> objects, int layoutId, int grabHandleId, boolean dragOnLongPress) {
            mLayoutId = layoutId;
            mGrabHandleId = grabHandleId;
            mDragOnLongPress = dragOnLongPress;
            this.context = context;
            setHasStableIds(true);
            setItemList(objects);
            //this.mData = objects;
        }


        @Override
        public int getItemCount() {
            return NB_CHANNEL;
        }



        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            if(holder.itemView.getTag() == null) {
                holder.chart.getDescription().setEnabled(false);
                holder.chart.setDrawGridBackground(false);

                XAxis xAxis = holder.chart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
                xAxis.setTextSize(10f);
                xAxis.setDrawAxisLine(false);
                xAxis.setDrawGridLines(true);
                xAxis.setDrawLabels(true);

                xAxis.setGranularity(1f);

                YAxis leftAxis = holder.chart.getAxisLeft();
                YAxis rightAxis = holder.chart.getAxisRight();

                leftAxis.setLabelCount(5, false);
                rightAxis.setLabelCount(5, false);

                leftAxis.setTextColor(Color.rgb(19, 160, 236));
                leftAxis.setGridColor(Color.rgb(16, 160, 236));

                if(YMaxValue != -1){
                    rightAxis.setAxisMaximum(YMaxValue);
                    leftAxis.setAxisMaximum(YMaxValue);
                }
                if(YMinValue != -1){
                    rightAxis.setAxisMinimum(YMinValue);
                    leftAxis.setAxisMinimum(YMinValue);
                }

                holder.chart.setDoubleTapToZoomEnabled(false);
                holder.chart.setAutoScaleMinMaxEnabled(bEnableAutoScale);
                holder.chart.getAxisLeft().setDrawGridLines(false);
                holder.chart.getAxisRight().setDrawLabels(false);
                holder.chart.getAxisRight().setDrawGridLines(false);
                holder.chart.getXAxis().setDrawGridLines(false);
                holder.chart.setVisibleXRangeMaximum(1000);


                holder.chart.setOnChartGestureListener(new OnChartGestureListener() {
                    @Override
                    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

                    }

                    @Override
                    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

                    }

                    @Override
                    public void onChartLongPressed(MotionEvent me) {
                        holder.onItemLongClicked(holder.itemView);
                    }

                    @Override
                    public void onChartDoubleTapped(MotionEvent me) {
                        Log.d(TAG, "" + holder.getAdapterPosition());
                        if (freezeMap.containsKey(holder.getAdapterPosition()))
                            freezeMap.put(holder.getAdapterPosition(), !freezeMap.get(holder.getAdapterPosition()));
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
                holder.chart.invalidate();

                holder.itemView.setTag(holder);
            }else {

            }

            LineData data= mItemList.get(relativeChannelPositions.get(position));
            // apply styling

            data.setValueTextColor(Color.BLACK);

            holder.chart.setVisibleXRangeMaximum(1000);

            // set data
            holder.chart.setData(data);

            holder.chart.getLineData().getDataSetByIndex(0).setLabel(channelName[relativeChannelPositions.get(position)]);

            // move to the latest entry
            if(!freezeMap.get(position)){
                if(data.getEntryCount() > 0){
                    holder.chart.notifyDataSetChanged();
                    holder.chart.moveViewToX(data.getEntryCount());
                    //holder.chart.moveViewToX(0);
                }
            }

            //if(position == 1){
                //Log.d(TAG, "entrycount : " + data.getEntryCount());
            //}
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        class ViewHolder extends DragItemAdapter.ViewHolder {
            LineChart chart;

            ViewHolder(final View itemView) {
                super(itemView, mGrabHandleId, mDragOnLongPress);
                chart = (LineChart) itemView.findViewById(R.id.chart);
            }

            @Override
            public void onItemClicked(View view) {
                Toast.makeText(view.getContext(), "Item clicked", Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean onItemLongClicked(View view) {
                //Toast.makeText(view.getContext(), "Item long clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
        }

    }

    private static class MyDragItem extends DragItem {

        MyDragItem(Context context, int layoutId) {
            super(context, layoutId);
        }

        @Override
        public void onBindDragView(View clickedView, View dragView) {
//            CharSequence text = ((TextView) clickedView.findViewById(R.id.text)).getText();
//            ((TextView) dragView.findViewById(R.id.text)).setText(text);
            dragView.findViewById(R.id.layout_chart).setBackgroundColor(dragView.getResources().getColor(R.color.list_item_background));
        }
    }
}
