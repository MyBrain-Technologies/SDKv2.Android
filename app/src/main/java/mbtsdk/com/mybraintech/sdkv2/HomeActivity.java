package mbtsdk.com.mybraintech.sdkv2;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

import core.bluetooth.BtState;
import engine.ConnectionConfig;
import engine.MbtClient;
import engine.clientevents.ConnectionException;
import engine.clientevents.ConnectionStateListener;
import features.MbtFeatures;

import static features.MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX;
import static features.MbtFeatures.VPRO_DEVICE_NAME_PREFIX;
import static features.ScannableDevices.MELOMIND;
import static features.ScannableDevices.VPRO;

public class HomeActivity extends AppCompatActivity{

    private static String TAG = HomeActivity.class.getName();
    private final static int SCAN_DURATION = 30000;
    public final static String DEVICE_NAME = "DEVICE_NAME";

    private MbtClient client;

    private EditText deviceNameField;
    private String deviceName;

    private Spinner devicePrefixSpinner;
    private String devicePrefix;

    private Button scanButton;
    private Button scanAllButton;

    private ScrollView devicesFoundList;

    private boolean isScanning = false;

    private Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initToolBar();
        toast= Toast.makeText(HomeActivity.this, "", Toast.LENGTH_SHORT);
        client = MbtClient.getClientInstance();

        initDeviceNameField();
        initScanButton();
        initScanAllButton();
        initDevicePrefix();
        initDevicesFoundList();
    }

    private void initDevicePrefix() {
        devicePrefixSpinner = findViewById(R.id.devicePrefix);
        ArrayList<String> prefixList = new ArrayList<>();
        prefixList.add(MELOMIND_DEVICE_NAME_PREFIX);
        prefixList.add(VPRO_DEVICE_NAME_PREFIX);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, prefixList);
        devicePrefixSpinner.setAdapter(arrayAdapter);
        devicePrefixSpinner.setSelection(arrayAdapter.getPosition(MELOMIND_DEVICE_NAME_PREFIX));
    }

    private void initDeviceNameField() {
        deviceNameField = findViewById(R.id.deviceNameField);
    }

    private void initDevicesFoundList() {
        devicesFoundList = findViewById(R.id.devicesFoundList);
    }

    private void initScanButton(){
        scanButton = findViewById(R.id.scanButton);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                devicePrefix = String.valueOf(devicePrefixSpinner.getSelectedItem()); //get the prefix chosed by the user in the Spinner
                deviceName = devicePrefix+deviceNameField.getText().toString(); //get the name entered by the user in the EditText
                if(isScanning){ //Scan in progress : a second click means that the user is trying to cancel the scan
                   cancelScan();
                }else{ // Scan is not in progress : starting a new scan in order to connect to a Mbt Device
                    startScan();
                }
            }
        });
    }

    private void initScanAllButton(){
        scanAllButton = findViewById(R.id.scanAllButton);

        scanAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*if(devicesFoundList.getVisibility() == View.VISIBLE)
                    devicesFoundList.setVisibility(View.INVISIBLE);
                else
                    devicesFoundList.setVisibility(View.VISIBLE);*/
                notifyUser("Scanning method not implemented yet");

            }
        });
    }

    private void startScan() {

        if(deviceName.equals(MELOMIND_DEVICE_NAME_PREFIX) || deviceName.equals(VPRO_DEVICE_NAME_PREFIX) ){ //no name entered by the user
            findAvailableDevice();
        }else{ //the user entered a name
            if((true) /*isMbtDeviceName() && deviceName.length() == DEVICE_NAME_MAX_LENGTH */) { //check the device name format
                notifyUser(getString(R.string.connect_in_progress));
                updateScanning(true); //changes isScanning to true and updates button text "Find a device" into "Cancel"

                client.connectBluetooth(new ConnectionConfig.Builder(new ConnectionStateListener<ConnectionException>() {
                    @Override
                    public void onStateChanged(@NonNull BtState newState) {
                        Log.e(TAG, "Current state updated in Home Activity"+newState);
                        if (newState.equals(BtState.CONNECTED_AND_READY)){
                            notifyUser("Device ' " + deviceName + " ' connected");
                            final Intent intent = new Intent(HomeActivity.this, DeviceActivity.class);
                            intent.putExtra(DEVICE_NAME, deviceName);
                            startActivity(intent);
                            finish();
                        }else if (newState.equals(BtState.SCAN_TIMEOUT)||(newState.equals(BtState.CONNECT_FAILURE))){
                            notifyUser(getString(R.string.connect_failed) + deviceName);
                        }
                    }

                    @Override
                    public void onError(ConnectionException exception) {
                        notifyUser(exception.toString());
                        exception.printStackTrace();
                    }
                }).deviceName(deviceName).maxScanDuration(SCAN_DURATION).scanDeviceType(isMelomindDevice() ? MELOMIND : VPRO).create());

            }else{ //if the device name entered by the user is empty or is not starting with a mbt prefix
                notifyUser(getString(R.string.wrong_device_name)+" "+deviceName);
                deviceNameField.setText(getString(R.string.example_device_name));
            }
        }
    }

    private void findAvailableDevice() {
        notifyUser(getString(R.string.find_first_available_headset));
        deviceName = getString(R.string.example_device_name) ;//todo replace getString... by the method that detects the first available device
        deviceNameField.setText(deviceName);
        scanButton.setText(getString(R.string.connect));
        scanButton.setTextColor(getResources().getColor(R.color.white));
        scanButton.setBackgroundColor(getResources().getColor(R.color.light_blue));
    }

    private void cancelScan(){
        updateScanning(false); // isScanning is false , and the scan button is updated
        client.cancelConnection();
    }

    /**
     * Updates the scanning state boolean and the Scan button text
     * The Scan button text is changed into into "Cancel" if scanning is launched
     * or into "Find a device" if scanning is cancelled
     * @param newIsScanning
     */
    private void updateScanning(boolean newIsScanning){
        isScanning = newIsScanning;
        scanButton.setText((isScanning ? R.string.cancel : R.string.scan));
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
        startActivity(new Intent(HomeActivity.this,WelcomeActivity.class));
    }

    private void initToolBar(){
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        //getSupportActionBar().setIcon(R.drawable.logo);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.light_blue)));
        }
    }
}
