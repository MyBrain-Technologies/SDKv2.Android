package mbtsdk.com.mybraintech.sdkv2;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;

import core.bluetooth.BtState;
import engine.ConnectionConfig;
import engine.MbtClient;
import engine.clientevents.BaseError;
import engine.clientevents.ConnectionStateReceiver;
import features.MbtFeatures;

import static features.MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX;
import static features.MbtFeatures.VPRO_DEVICE_NAME_PREFIX;
import static features.ScannableDevices.MELOMIND;
import static features.ScannableDevices.VPRO;

public class HomeActivity extends AppCompatActivity{

    private static String TAG = HomeActivity.class.getName();
    private final static int SCAN_DURATION = 30000;
    public final static String DEVICE_NAME = "DEVICE_NAME";
    public final static String BLUETOOTH_STATE = "BLUETOOTH_STATE";

    private MbtClient client;

    private EditText deviceNameField;
    private String deviceName;

    private Switch connectAudioSwitch;
    private boolean connectAudioIfDeviceCompatible ;

    private Spinner devicePrefixSpinner;
    private String devicePrefix;

    private Button scanButton;

    private boolean isCancel = false;

    private Toast toast;

    private ConnectionStateReceiver connectionStateReceiver = new ConnectionStateReceiver() {
        @Override
        public void onError(BaseError error, String additionnalInfo) {
            notifyUser(error.getMessage()+ (additionnalInfo != null ? additionnalInfo : ""));
            updateScanning(false);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            BtState newState = (BtState) intent.getSerializableExtra("newState");
            Log.i(TAG, "Received broadcast "+newState);

            if (newState.equals(BtState.CONNECTED_AND_READY) ){
                toast.cancel();
                deinitCurrentActivity(newState);
            }else{
                if(!toast.getView().isShown())
                    notifyUser(getString(R.string.no_connected_headset));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initToolBar();
        toast = Toast.makeText(HomeActivity.this, "", Toast.LENGTH_LONG);
        client = MbtClient.getClientInstance();
        isCancel = false;

        initDeviceNameField();
        initConnectAudioSwitch();
        initScanButton();
        initDevicePrefix();
    }

    private void initConnectAudioSwitch() {
        connectAudioSwitch = findViewById(R.id.connectAudio);
    }

    private void initDevicePrefix() {
        devicePrefixSpinner = findViewById(R.id.devicePrefix);
        ArrayList<String> prefixList = new ArrayList<>();
        prefixList.add(MELOMIND_DEVICE_NAME_PREFIX);
        prefixList.add(VPRO_DEVICE_NAME_PREFIX);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, prefixList);
        arrayAdapter.setDropDownViewResource(R.layout.spinner_item);
        devicePrefixSpinner.setAdapter(arrayAdapter);
        devicePrefixSpinner.setSelection(arrayAdapter.getPosition(MELOMIND_DEVICE_NAME_PREFIX));
    }


    private void initDeviceNameField() {
        deviceNameField = findViewById(R.id.deviceNameField);
    }

    private void initScanButton(){
        scanButton = findViewById(R.id.scanButton);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyUser(getString(R.string.scan_in_progress));
                devicePrefix = String.valueOf(devicePrefixSpinner.getSelectedItem()); //get the prefix chosed by the user in the Spinner
                deviceName = devicePrefix+deviceNameField.getText().toString(); //get the name entered by the user in the EditText
                connectAudioIfDeviceCompatible = connectAudioSwitch.isChecked();
                if(isCancel){ //Scan in progress : a second click means that the user is trying to cancel the scan
                   cancelScan();
                }else{ // Scan is not in progress : starting a new scan in order to connect to a Mbt Device
                    startScan();
                }
                updateScanning(!isCancel);

            }
        });
    }


    private void startScan() {
        LocalBroadcastManager.getInstance(this).registerReceiver(connectionStateReceiver,
                new IntentFilter(MbtFeatures.INTENT_CONNECTION_STATE_CHANGED));
            client.connectBluetooth(new ConnectionConfig.Builder(connectionStateReceiver)
                    .deviceName(
                            ((deviceName != null) && (deviceName.equals(MELOMIND_DEVICE_NAME_PREFIX) || deviceName.equals(VPRO_DEVICE_NAME_PREFIX)) ) ? //if no no name has been entered by the user, the default device name is the headset prefix
                            null : deviceName ) //null is given in parameters if no name has been entered by the user
                    .maxScanDuration(SCAN_DURATION)
                    .scanDeviceType(isMelomindDevice() ? MELOMIND : VPRO)
                    .connectAudioIfDeviceCompatible(connectAudioIfDeviceCompatible)
                    .create());

    }

    private void findAvailableDevice() {
        notifyUser(getString(R.string.find_first_available_headset));
        //deviceName =  ;//todo replace  by the method that detects the first available device
        deviceNameField.setText(deviceName);
        scanButton.setText(getString(R.string.connect));
        scanButton.setTextColor(getResources().getColor(R.color.white));
        scanButton.setBackgroundColor(getResources().getColor(R.color.light_blue));
    }

    private void cancelScan(){
        client.cancelConnection();
    }

    /**
     * Updates the scanning state boolean and the Scan button text
     * The Scan button text is changed into into "Cancel" if scanning is launched
     * or into "Find a device" if scanning is cancelled
     * @param newIsCancel
     */
    private void updateScanning(boolean newIsCancel){
        isCancel = newIsCancel;
        if(!isCancel)
            toast.cancel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            scanButton.setBackgroundColor((isCancel ? Color.LTGRAY : getColor(R.color.light_blue)));

        scanButton.setText((isCancel ? R.string.cancel : R.string.scan));
    }

    /**
     * Returns true if the device name contains "melo_" or "vpro_", false otherwise
     * @return true if the device name contains "melo_" or "vpro_", false otherwise
     */
    private boolean isMbtDeviceName(){
        return isMelomindDevice() || isVproDevice();
    }

    private boolean isMelomindDevice(){
        return deviceName.startsWith(MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX);
    }

    private boolean isVproDevice(){
        return deviceName.startsWith(MbtFeatures.VPRO_DEVICE_NAME_PREFIX);
    }

    private void notifyUser(String message){
        toast.setText("");
        toast.show();
        toast.setText(message);
        toast.show();
    }

    @Override
    public void onBackPressed() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(connectionStateReceiver);
        connectionStateReceiver = null;
    }

    private void initToolBar(){
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.logo);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.light_blue)));
        }
    }

    private void deinitCurrentActivity(BtState newState){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(connectionStateReceiver);
        connectionStateReceiver = null;
        final Intent intent = new Intent(HomeActivity.this, DeviceActivity.class);
        intent.putExtra(DEVICE_NAME, deviceName);
        intent.putExtra(BLUETOOTH_STATE, newState);
        startActivity(intent);
        finish();
    }
}
