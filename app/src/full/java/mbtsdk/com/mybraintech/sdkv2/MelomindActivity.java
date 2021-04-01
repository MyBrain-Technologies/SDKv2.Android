package mbtsdk.com.mybraintech.sdkv2;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
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
import com.woxthebox.draglistview.DragListView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import command.CommandInterface;
import command.DeviceStreamingCommands;
import config.AmpGainConfig;
import config.ConnectionConfig;
import config.FilterConfig;
import config.RecordConfig;
import config.StreamConfig;
import core.Indus5FastMode;
import core.bluetooth.StreamState;
import core.device.event.DCOffsetEvent;
import core.device.event.SaturationEvent;
import core.device.model.MbtDevice;
import core.device.model.MbtVersion;
import core.device.oad.OADState;
import core.eeg.storage.MbtEEGPacket;
import core.recording.metadata.Comment;
import engine.MbtClient;
import engine.clientevents.BaseError;
import engine.clientevents.ConnectionStateListener;
import engine.clientevents.DeviceBatteryListener;
import engine.clientevents.DeviceStatusListener;
import engine.clientevents.EegListener;
import engine.clientevents.OADStateListener;
import features.MbtDeviceType;
import utils.AsyncUtils;
import utils.MatrixUtils;

public class MelomindActivity extends AppCompatActivity implements ConnectionStateListener<BaseError>, EegListener<BaseError>, DeviceBatteryListener<BaseError>, OADStateListener<BaseError>, DeviceStatusListener<BaseError> {

    private static String TAG = mbtsdk.com.mybraintech.sdkv2.MelomindActivity.class.getName();

    private static final boolean USE_OAD_IN_MELOMIND = true;
    private static final boolean SHOULD_KEEP_HISTORY = false;
    private static final boolean USE_QUALITIES = true;
    private static final String DIRECTORY = "MBT";

    MbtClient sdkClient;

    private DragListView lv;
    private VProActivity.ChartDataAdapter cda;
    private ArrayList<LineData> dataList;

    private SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm:ss");

    private LineData lineData = new LineData();
    private LineChart mLineChart;

    private ArrayList<ArrayList<Float>> bufferedChartData;

    private String patientID = null;
    private String condition = null;

    private boolean isStreaming = false;
    private boolean isRecording = false;
    private boolean freezeGraph = false;

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

    private TextView channelsQualities;
    //private TextView channel2Quality;

    private LinearLayout buttonLayout;
    private TextView idleTV;
    private TextView appliVersion;

    private TextView qualities;

    private TextView progressOAD;

    //Comments
    private FrameLayout commentLayout;
    private FloatingActionButton fabComment;
    private EditText commentEditText;
    private ArrayList<Comment> comments = new ArrayList<>();
    private ImageButton commentSend;
    ArrayList<DeviceStreamingCommands> commands = new ArrayList<>();

    private long counter  = 0;

    private Dialog mOADDialog;
    private CircularFillableLoaders mLoader;

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

    private long nbPacketsStreamed = 0 ;

    private long nbPacketsToRecord = 0 ;
    private long programmedNbPacketsToRecord = 0 ;

    private mbtsdk.com.mybraintech.sdkv2.MelomindActivity.appliState mAppliState = mbtsdk.com.mybraintech.sdkv2.MelomindActivity.appliState.DISCONNECTED;

    Map<Integer, Integer> relativeChannelPositions;
    private final int NB_CHANNEL = 2;
    String[] channelName = new String[NB_CHANNEL];
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
    public void onDeviceConnected(final MbtDevice device) {
        setState(appliState.CONNECTED);
        fwVersionTextView.post(new Runnable() {
            @Override
            public void run() {
                fwVersionTextView.setText("FW Version : " + device.getFirmwareVersion().toString());
            }
        });

        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //vpro.updateDeviceConfiguration(MBTConfig.locations, MBTConfig.references, MBTConfig.grounds);
                MBTConfig.loadConfig("MM");
                initChannelsNames();

                if (!Indus5FastMode.INSTANCE.isEnabled()) {
                    //reading battery is not yet implemented for indus5
                    sdkClient.readBattery(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this);
                }
            }
        });
        t.setDaemon(true); // ça c'est important ça veut dire que le thread ne va pas vivre en dehors du process de l'app
        t.start();

        //Get the current config of the analog frontend EEG, embedded into the
        // headset -> needed to configure the current Gain in EEG voltage converting formula.
//                AsyncUtils.executeAsync(new Runnable() {
//                        @Override
//                        public void run() {
//                            melomind.getEEGConfiguration();
//                        }
//                    });
    }

    @Override
    public void onDeviceDisconnected(MbtDevice device) {
        setState(appliState.DISCONNECTED);
    }

    @Override
    public void onSaturationStateChanged(SaturationEvent saturationEvent) {
        final int[] colors = new int[2];
        switch (saturationEvent.getSaturationCode()){
            case 3:
                colors[0] = colors[1] = Color.RED;
                break;

            case 2:
                colors[0] = Color.GREEN;
                colors[1] = Color.RED;
                break;

            case 1:
                colors[0] = Color.RED;
                colors[1] = Color.GREEN;
                break;
            case 0:
            default:
                colors[0] = colors[1] = Color.GREEN;
                break;
        }

        channelsQualities.post(new Runnable() {
            @Override
            public void run() {
                channelsQualities.setTextColor(colors[0]);
            }
        });
//        channel2Quality.post(new Runnable() {
//            @Override
//            public void run() {
//                channel2Quality.setTextColor(colors[1]);
//            }
//        });
    }

    @Override
    public void onNewDCOffsetMeasured(DCOffsetEvent dcOffsets/*DCOffsetEvent dcOffsetEvent*/) {

    }

    @Override
    public void onNewPackets(final @NonNull MbtEEGPacket mbteegPackets) {
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

        mbteegPackets.setChannelsData(MatrixUtils.invertFloatMatrix(mbteegPackets.getChannelsData()));

        //Updating chart
        mLineChart.post(new Runnable() {
            @Override
            public void run() {
                counter++;
                if(!freezeGraph){
                    if(SHOULD_KEEP_HISTORY || counter <= 2)
                        addEntry(mLineChart, mbteegPackets.getChannelsData(), mbteegPackets.getStatusData());
                    else
                        updateEntry(mLineChart,mbteegPackets.getChannelsData(), bufferedChartData, mbteegPackets.getStatusData());
                }
                bufferedChartData = (ArrayList<ArrayList<Float>>) mbteegPackets.getChannelsData().clone();
                bufferedChartData.add(0, mbteegPackets.getStatusData());
            }
        });

        //Updating qualities if present

        channelsQualities.post(new Runnable() {
            @Override
            public void run() {
//                channelsQualities.setText("Channel 1 QC : " + (mbteegPackets.getQualities() != null ? mbteegPackets.getQualities().get(0) : "--"));
                setQualities(mbteegPackets.getQualities());
            }
        });
//        channel2Quality.post(new Runnable() {
//            @Override
//            public void run() {
//                channel2Quality.setText("Channel 2 QC : " + (mbteegPackets.getQualities() != null ? mbteegPackets.getQualities().get(1) : "--"));
//            }
//        });
    }

    @Override
    public void onNewStreamState(@NonNull StreamState streamState) {

    }

    static int prevProgress = -1;

    @Override
    public void onError(BaseError baseError, String s) {
        Toast.makeText(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this, "Error: "+baseError.getMessage(), Toast.LENGTH_LONG).show();
        if(mAppliState.equals(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.appliState.CONNECTING))
            setState(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.appliState.DISCONNECTED);
    }


    @Override
    public void onStateChanged(final OADState oadState) {
        progressOAD.post(new Runnable() {
            @Override
            public void run() {
                progressOAD.setText("OAD state: "+oadState);
            }
        });
        switch (oadState){
            case COMPLETED:
            case ABORTED:
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(mOADDialog.isShowing()){
                    mOADDialog.dismiss();
                }
                break;
        }
    }

    @Override
    public void onProgressPercentChanged(final int progress) {
        if(progress > prevProgress){
            prevProgress = progress;
            mLoader.post(new Runnable() {
                @Override
                public void run() {
                    mLoader.setProgress(100-progress);
                }
            });
        }
        progressOAD.post(new Runnable() {
            @Override
            public void run() {
                progressOAD.setText("Progress : " + progress + " %");
            }
        });
    }



//    private String stringCodeToString(String stringCode){
//        switch (stringCode){
//            case "10":
//                return "AMP_GAIN_X12_DEFAULT";
//                break;
//            case "20":
//                return "AMP_GAIN_X8_MEDIUM";
//            break;
//            case "30":
//                return "AMP_GAIN_X6_LOW";
//            break;
//            case "40":
//                return "AMP_GAIN_X4_VLOW";
//            break;
//        }
//        return "Error";
//    }

    private enum appliState {DISCONNECTING, DISCONNECTED, CONNECTING, CONNECTED, STREAMING, RECORDING}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();

        sdkClient = MbtClient.getClientInstance();

        commands.add(new DeviceStreamingCommands.EegConfig.Builder(new CommandInterface.CommandCallback<byte[]>() {
            @Override
            public void onError(CommandInterface.MbtCommand mbtCommand, BaseError baseError, String s) {

            }

            @Override
            public void onRequestSent(CommandInterface.MbtCommand mbtCommand) {

            }

            @Override
            public void onResponseReceived(CommandInterface.MbtCommand mbtCommand, final byte[] bytes) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(Arrays.toString(bytes) == "true" || Arrays.toString(bytes) == "false")
                        {
                            Toast.makeText(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this, "MBX_GET_EEG_CONFIG is : " + (Arrays.toString(bytes)) , Toast.LENGTH_SHORT).show();
                        }else
                        {
                            AlertDialog alertDialog = new AlertDialog.Builder(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this).create();
                            alertDialog.setTitle("MBX_GET_EEG_CONFIG input notification");
                            alertDialog.setMessage(Arrays.toString(bytes) + "\n[ 14, NOTCH, BANDPASS, GAIN ]" +
                                    "\n\n------ mailbox notch ------\n" +
                                    "50\t: NOTCH 50HZ 100Hz\n" +
                                    "60\t: NOTCH 60HZ 120Hz\n" +
                                    "70\t: NOTCH DEFAULT 50Hz 60Hz\n" +
                                    "\n" +
                                    "------ mailbox Bandpass ------\n" +
                                    "10\t: BANDPASS DEFAULT 1-40Hz\n" +
                                    "20\t: BANDPASS NARROW 5-15Hz\n" +
                                    "30\t: BANDPASS WIDE 0.16-40Hz\n" +
                                    "\n" +
                                    "------ mailbox Amplifying Gain ------\n" +
                                    "10\t: GAIN X12 DEFAULT\n" +
                                    "20\t: GAIN X8 MEDIUM\n" +
                                    "30\t: GAIN X6 LOW\n" +
                                    "40\t: GAIN X4 VLOW");
                            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                        }
                    }
                });
            }
        }).createForDevice(MbtDeviceType.MELOMIND));

        //vpro = new VProEngine(getApplicationContext());
        setContentView(R.layout.activity_melomind);

        mFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        mLineChart = (LineChart) findViewById(R.id.chart);

        channelsQualities = (TextView) findViewById(R.id.channels_qualities);
       // channel2Quality = (TextView) findViewById(R.id.channel_2_quality);
//        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
//        btAdapter.disable();

        fwVersionTextView = (TextView) findViewById(R.id.fwVersion);

        mOADDialog = new Dialog(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this);
        mOADDialog.setCancelable(false);
        mOADDialog.setContentView(R.layout.oad_dialog_content);
        mOADDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mLoader = (CircularFillableLoaders) mOADDialog.findViewById(R.id.circularFillableLoaders);
        progressOAD =(TextView) mOADDialog.findViewById(R.id.oadProgress);
        mLoader.setProgress(100);

        settingsButton = (ImageButton) findViewById(R.id.settingsButton);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu settingsMenu = new PopupMenu(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this, settingsButton);
                settingsMenu.getMenuInflater().inflate(R.menu.menu_scrolling, settingsMenu.getMenu());

                settingsMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()){
                            case R.id.action_filters:
                                if(mAppliState == appliState.CONNECTED || mAppliState == appliState.STREAMING){
                                    //TODO
                                    final CharSequence[] filterConfigs = new CharSequence[FilterConfig.values().length];
                                    for(int i = 0; i < FilterConfig.values().length; i++){
                                        filterConfigs[i] = FilterConfig.values()[i].toString();
                                    }
                                    final AlertDialog.Builder builder = new AlertDialog.Builder(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this);
                                    builder.setTitle("Choose filter configuration to apply to the next EEG stream")
                                            .setItems(filterConfigs, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, final int which) {
                                                    // The 'which' argument contains the index position
                                                    // of the selected item
                                                    dialog.cancel();
                                                    commands.add(new DeviceStreamingCommands.NotchFilter(FilterConfig.values()[which])) ;
                                                    ;
                                                }
                                            });
                                    builder.create();
                                    builder.show();
                                }
                                else{
                                    Toast.makeText(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this, "Cannot change while recording", Toast.LENGTH_SHORT).show();

                                }
                                break;

                            case R.id.action_AmpGain:
                                if(mAppliState == appliState.CONNECTED || mAppliState == appliState.STREAMING || mAppliState == appliState.RECORDING){
                                    final CharSequence[] ampGainConfig = new CharSequence[AmpGainConfig.values().length];
                                    for(int i = 0; i < AmpGainConfig.values().length; i++){
                                        ampGainConfig[i] = AmpGainConfig.values()[i].toString();
                                    }
                                    final AlertDialog.Builder builder = new AlertDialog.Builder(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this);
                                    builder.setTitle("Choose amplifier gain to apply to the next EEG stream")
                                            .setItems(ampGainConfig, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, final int which) {
                                                    // The 'which' argument contains the index position
                                                    // of the selected item
                                                    dialog.cancel();
                                                    commands.add(new DeviceStreamingCommands.AmplifierGain((AmpGainConfig.values()[which]))) ;
                                                }
                                            });
                                    builder.create();
                                    builder.show();
                                }
                                else{
                                    Toast.makeText(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this, "Error: Cannot change if not connected", Toast.LENGTH_SHORT).show();
                                }
                                break;

                            case R.id.action_getAfeConfig:
                                if(mAppliState == appliState.CONNECTED) {

                                    AsyncUtils.executeAsync(new Runnable() {
                                        @Override
                                        public void run() {
                                            commands.add(new DeviceStreamingCommands.EegConfig.Builder(new CommandInterface.CommandCallback<byte[]>() {
                                                @Override
                                                public void onError(CommandInterface.MbtCommand mbtCommand, BaseError baseError, String s) {

                                                }

                                                @Override
                                                public void onRequestSent(CommandInterface.MbtCommand mbtCommand) {

                                                }

                                                @Override
                                                public void onResponseReceived(CommandInterface.MbtCommand mbtCommand, final byte[] bytes) {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                AlertDialog alertDialog = new AlertDialog.Builder(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this).create();
                                                                alertDialog.setTitle("MBX_GET_EEG_CONFIG input notification");
                                                                alertDialog.setMessage(Arrays.toString(bytes) + "\n[ 14, NOTCH, BANDPASS, GAIN ]" +
                                                                        "\n\n------ mailbox notch ------\n" +
                                                                        "50\t: NOTCH 50HZ 100Hz\n" +
                                                                        "60\t: NOTCH 60HZ 120Hz\n" +
                                                                        "70\t: NOTCH DEFAULT 50Hz 60Hz\n" +
                                                                        "\n" +
                                                                        "------ mailbox Bandpass ------\n" +
                                                                        "10\t: BANDPASS DEFAULT 1-40Hz\n" +
                                                                        "20\t: BANDPASS NARROW 5-15Hz\n" +
                                                                        "30\t: BANDPASS WIDE 0.16-40Hz\n" +
                                                                        "\n" +
                                                                        "------ mailbox Amplifying Gain ------\n" +
                                                                        "10\t: GAIN X12 DEFAULT\n" +
                                                                        "20\t: GAIN X8 MEDIUM\n" +
                                                                        "30\t: GAIN X6 LOW\n" +
                                                                        "40\t: GAIN X4 VLOW");
                                                                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                                                        new DialogInterface.OnClickListener() {
                                                                            public void onClick(DialogInterface dialog, int which) {
                                                                                dialog.dismiss();
                                                                            }
                                                                        });
                                                                alertDialog.show();
                                                            }
                                                        });
                                                }
                                            }).createForDevice(MbtDeviceType.MELOMIND));
                                        }
                                    });
                                }
                                else{
                                    Toast.makeText(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this, "Error: Cannot access if not connected or streaming", Toast.LENGTH_SHORT).show();
                                }
                                break;

                            case R.id.action_config:
                                Toast.makeText(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this, "Unavailable for melomind...", Toast.LENGTH_SHORT).show();

                                break;
                            case R.id.action_DcMeasure:
                                if(mAppliState == appliState.CONNECTED) {
                                    new AlertDialog.Builder(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this)
                                            .setTitle("Enable DC offset measure ?")
                                            .setCancelable(false)
                                            .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    commands.add(new DeviceStreamingCommands.DcOffset(true));
                                                }
                                            })
                                            .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    commands.add(new DeviceStreamingCommands.DcOffset(false));

                                                }
                                            })
                                            .show();
                                }else{
                                    Toast.makeText(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this, "Error: Cannot acces if not connected or streaming", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case R.id.action_mtu:
                                if(mAppliState != appliState.CONNECTED){
                                    Toast.makeText(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this, "Melomind must be neither disconnected nor streaming", Toast.LENGTH_SHORT).show();
                                }else
                                    displayDialogForDeviceNumber(null);
                                break;


                            case R.id.action_oad:
                                try {
                                    final AssetManager assetManager = getAssets();
                                    String[] myFiles = assetManager.list("");
                                    if(myFiles.length > 0){
                                        final ArrayList<String> binFilesList = new ArrayList<String>();
                                        for(int i= 0; i < myFiles.length; i++){
                                            if(myFiles[i].endsWith(".bin")){
                                                binFilesList.add(myFiles[i]);
                                            }
                                        }
                                        //File file = new File(myFiles[0]);
                                        String[] binariesArray = new String[binFilesList.size()];
                                        binariesArray = binFilesList.toArray(binariesArray);
                                        if(!binFilesList.isEmpty()){

                                            final AlertDialog.Builder builder = new AlertDialog.Builder(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this);
                                            builder.setTitle("Choose file")
                                                    .setItems(binariesArray, new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, final int which) {
                                                            // The 'which' argument contains the index position
                                                            // of the selected item
                                                            dialog.cancel();
                                                            mOADDialog.show();
                                                            progressOAD.setText("Starting OAD...");
                                                            AsyncUtils.executeAsync(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    sdkClient.updateFirmware(new MbtVersion(binFilesList.get(which)), mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this);
                                                                }
                                                            });

                                                        }
                                                    });
                                            builder.create();
                                            builder.show();

                                        }

                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                        return true;
                    }
                });
                settingsMenu.show();
            }
        });


        findViewById(R.id.btsettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentOpenBluetoothSettings = new Intent();
                intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivityForResult(intentOpenBluetoothSettings, 0);
            }
        });


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
                setState(appliState.CONNECTING);

                AsyncUtils.executeAsync(new Runnable() {
                    @Override
                    public void run() {
                        sdkClient.connectBluetooth(new ConnectionConfig.Builder(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this)
                                .createForDevice(MbtDeviceType.MELOMIND));

                    }
                });
            }

        });

        View btn_eeg_indus5 = findViewById(R.id.btn_eeg_indus5);
        btn_eeg_indus5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncUtils.executeAsync(new Runnable() {
                    @Override
                    public void run() {
//                        sdkClient.requestEeg();
                    }
                });
            }

        });

        disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "init melomind disconnection process");
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

                setState(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.appliState.DISCONNECTING);


            }
        });

        startStreamButton = (Button) findViewById(R.id.startStreamingButton);
        startStreamButton.setOnClickListener(new View.OnClickListener() {

            private void startStreamAfterDialogClick(final boolean b){
                comments = new ArrayList<>();

                if(useAdvancedFeatures)
                    patientID = patientIDEditText.getText().toString();

                commands.add(new DeviceStreamingCommands.Triggers(b));
                AsyncUtils.executeAsync(new Runnable() {
                    @Override
                    public void run() {
                        StreamConfig.Builder streamConfigBuilder = new StreamConfig.Builder(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this)
                                .useQualities()
                                .setDeviceStatusListener(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this);

                        if(!commands.isEmpty()) {
                            DeviceStreamingCommands[] commandsAsArray = new DeviceStreamingCommands[commands.size()];
                            for (int command = 0 ; command <commands.size(); command++){
                                commandsAsArray[command] = commands.get(command);
                            }
                            streamConfigBuilder.configureAcquisitionFromDeviceCommand(commandsAsArray);
                        }

                        sdkClient.startStream(streamConfigBuilder.createForDevice(MbtDeviceType.MELOMIND));
                        setState(appliState.STREAMING);
                    }
                });

            }

            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this)
                        .setTitle("Use P300 ?")
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startStreamAfterDialogClick(true);
                            }
                        })
                        .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startStreamAfterDialogClick(false);
                            }
                        })
                        .show();

            }
        });

        stopStreamButton = (Button) findViewById(R.id.stopStreamingButton);
        stopStreamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetChart(mLineChart);

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

                setState(appliState.CONNECTED);
            }
        });

        startRecordButton = (Button) findViewById(R.id.startRecordingButton);
        startRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(useAdvancedFeatures) {
                    int multiplier = 0;
                    condition = conditionEditText.getText().toString();
                    if (timerSwitch.isChecked()) {
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

                Log.i(TAG, "init melomind recording process");
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
                Log.i(TAG, "stopping melomind recording process");
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

                    comments.add(new Comment(commentEditText.getText().toString(), System.currentTimeMillis(), null));

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

                        View view2 = mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this.getCurrentFocus();
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
    }

    private void resetChart(LineChart mLineChart) {

    }


    private void setQualities(ArrayList<Float> qualities) {
        StringBuilder qualitiesDisplayed = new StringBuilder();
        for (int qualityTextView = 0 ; qualityTextView < qualities.size() ; qualityTextView++){
            qualitiesDisplayed.append(getString(R.string.quality))
                    .append("(")
                    .append(channelName[qualityTextView])
                    .append(")")
                    .append( ": ")
                    .append( qualities != null ?
                            qualities.get(qualityTextView)
                            : "--")
                    .append("     ");
        }
        this.channelsQualities.setText(qualitiesDisplayed.toString());

    }

    private void saveNewComment(String comment) {
        if(comments == null)
            comments = new ArrayList<>();

        comments.add(new Comment(comment, System.currentTimeMillis(), null));
    }

    private void initChannelsNames() {
        for(int i = 0; i < MBTConfig.getLocationsInDisplayOrder().length; i++){
            channelName[i] =  MBTConfig.getLocationsInDisplayOrder()[i].toString();
            //relativeChannelPositions.put(i,i);
        }
        if(cda != null)
            cda.notifyDataSetChanged();
    }

    private void displayDialogForDeviceNumber(String errorMessage) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this);
        final EditText input = new EditText(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        int maxLength = 3;
        InputFilter[] fArray = new InputFilter[1];
        fArray[0] = new InputFilter.LengthFilter(maxLength);
        input.setFilters(fArray);
        input.setMaxEms(3);
        input.setEms(3);
        if(errorMessage != null){
            input.setError(errorMessage);
        }

        alertDialog.setView(input);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);
        alertDialog.setCancelable(true);
        alertDialog.setTitle("Enter MTU (23 to 121)");
        alertDialog.setMessage("The entered value will be applied for the next EEG acquisition.");
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(input.getText().toString().equals("") || Integer.parseInt(input.getText().toString()) < 23 || Integer.parseInt(input.getText().toString()) > 121){
                    displayDialogForDeviceNumber("Please enter correct number");

                }else {
                    //commands.add(new BluetoothCommands.Mtu(Integer.parseInt(input.getText().toString())));
                    Log.d(TAG, "Requesting new MTU of " + input.getText());
                    AsyncUtils.executeAsync(new Runnable() {
                        @Override
                        public void run() {
                            //final boolean b = sdkClient.changeMTU(mtu);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                                    if(b)
                                    Toast.makeText(mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this, "MTU change not implemented yet in the SDK", Toast.LENGTH_SHORT).show();
//                                    else
//                                        Toast.makeText(MelomindActivity.this, "MTU change failure", Toast.LENGTH_SHORT).show();
                                }
                            });


                        }
                    });
                }
            }
        });
        alertDialog.show();
    }

    public void initiatilizeGraph(LineChart lineChart){
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
        lineData.removeDataSet(2);
        lineData.removeDataSet(1);
        lineData.removeDataSet(0);
        lineData.addDataSet(dataSetChan1);
        lineData.addDataSet(dataSetChan2);
        lineData.addDataSet(dataSetChan3);

        lineChart.setData(lineData);
        // this.chart.setAutoScaleMinMaxEnabled(true);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(Color.rgb(255, 192, 56));
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(1f); // one hour

        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.setAutoScaleMinMaxEnabled(true);
        lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.getAxisLeft().setDrawLabels(true);
        lineChart.getAxisRight().setDrawLabels(true);
        lineChart.getAxisRight().setDrawGridLines(false);
        lineChart.getAxisRight().setAxisMinimum(-0.05f);
        lineChart.getAxisRight().setAxisMaximum(1f);
        lineChart.getXAxis().setDrawGridLines(false);

        //lineChart.getXAxis().setDrawLabels(true);

        /*chart.getAxisLeft().setAxisMaxValue(-10000f);
        chart.getAxisLeft().setAxisMaxValue(-10000f);*/
        lineChart.setOnChartGestureListener(new OnChartGestureListener() {
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
                freezeGraph = !freezeGraph;
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

        lineChart.invalidate();
    }

    private void addEntry(LineChart mChart, ArrayList<ArrayList<Float>> channelData, ArrayList<Float> statusData) {

        LineData data = mChart.getData();
        //System.currentTimeMillis();
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

            if(!freezeGraph){


                // let the chart know it's data has changed
                mChart.notifyDataSetChanged();

                // limit the number of visible entries
                mChart.setVisibleXRangeMaximum(500);

                // move to the latest entry
                mChart.moveViewToX((data.getEntryCount()/2));
            }

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

            if(!freezeGraph){


                // let the chart know it's data has changed
                mChart.notifyDataSetChanged();

                // limit the number of visible entries
                mChart.setVisibleXRangeMaximum(500);

                // move to the latest entry
                mChart.moveViewToX((data.getEntryCount()/2));
            }

        }else{
            throw new IllegalStateException("Graph not correctly initialized");
        }
    }

    private void setState(final appliState state){
        mAppliState = state;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch(state){

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

                        if(isRecording){
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

                        settingsButton.setVisibility(View.VISIBLE);


                        break;

                    case CONNECTING:
                    case DISCONNECTING:
                        connectButton.setVisibility(View.GONE);

                        progressBar.setVisibility(View.VISIBLE);

                        disconnectButton.setVisibility(View.GONE);


                        settingsButton.setVisibility(View.VISIBLE);


                        break;

                    case STREAMING:
                        initiatilizeGraph(mLineChart);
                        if(isRecording){
                            fabComment.setVisibility(View.GONE);

                            stopRecordButton.setVisibility(View.GONE);

                            startRecordButton.setVisibility(View.VISIBLE);
                            startRecordButton.setEnabled(true);
                            startRecordButton.setText("Start Recording");

                            isRecording = false;

                        }else{
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

                        if(!isStreaming){
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
                        },2000);


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
                        settingsButton.setVisibility(View.VISIBLE);


                        fabComment.setVisibility(View.GONE);

                        if(advancedLayout.isExpanded()){
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
            View view = mbtsdk.com.mybraintech.sdkv2.MelomindActivity.this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }else{
            super.onBackPressed();
        }

    }

    @Override
    protected void onStop() {
//        if(isRecording || isStreaming)
//            disconnectButton.performClick();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case 0:

                if(resultCode == RESULT_CANCELED){
                    Log.i(TAG, "BT settings back with cancel result");
                }else{
                    Log.i(TAG, "BT settings back with ok result");
                }

                break;

            default:
                break;

        }
    }


}
