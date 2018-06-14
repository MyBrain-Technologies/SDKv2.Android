package mbtsdk.com.mybraintech.sdkv2;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.Toast;

import core.bluetooth.BtState;
import engine.ConnectionConfig;
import engine.MbtClient;
import engine.clientevents.ConnectionStateListener;
import features.MbtFeatures;

import static features.MbtFeatures.DEVICE_NAME_MAX_LENGTH;
import static features.ScannableDevices.MELOMIND;
import static features.ScannableDevices.VPRO;

public class HomeActivity extends AppCompatActivity{

    private static String TAG = HomeActivity.class.getName();
    private final static int SCAN_DURATION = 30000;
    public final static String DEVICE_NAME = "DEVICE_NAME";

    private MbtClient client;

    private EditText deviceNameField;
    private String deviceName;

    private Button scanButton;
    private Button scanAllButton;
    private ScrollView devicesFoundList;

    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        client = MbtClient.init(getApplicationContext());

        deviceNameField = findViewById(R.id.deviceNameField);

        initScanButton();
        initScanAllButton();
        initDevicesFoundList();
    }

    private void initDevicesFoundList() {
        devicesFoundList = findViewById(R.id.devicesFoundList);
    }

    private void initScanButton(){
        scanButton = findViewById(R.id.scanButton);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceName = deviceNameField.getText().toString();

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

        if(deviceName.isEmpty() ){ //no name entered by the user
            findAvailableDevice();
        }else{ //the user entered a name
            if( isMbtDeviceName() && deviceName.length() == DEVICE_NAME_MAX_LENGTH ) { //check the device name format
                notifyUser(getString(R.string.scan_in_progress));
                updateScanning(true); //changes isScanning to true and updates button text "Find a device" into "Cancel"

                client.connectBluetooth(new ConnectionConfig.Builder(new ConnectionStateListener() {
                    @Override
                    public void onStateChanged(@NonNull BtState newState) {
                        if (newState.equals(BtState.CONNECTED_AND_READY)){
                            notifyUser("Device ' " + deviceName + " ' found");
                            final Intent intent = new Intent(HomeActivity.this, DeviceActivity.class);
                            intent.putExtra(DEVICE_NAME, deviceName);
                            startActivity(intent);
                            finish();
                        }else if (newState.equals(BtState.SCAN_TIMEOUT)||(newState.equals(BtState.CONNECT_FAILURE))){
                            notifyUser(getString(R.string.connect_failed) + deviceName);
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        exception.printStackTrace();
                    }
                }).deviceName(deviceName).maxScanDuration(SCAN_DURATION).scanDeviceType(isMelomindDevice() ? MELOMIND : VPRO).create());

            }else{ //if the device name entered by the user is empty or is not starting with a mbt prefix
                notifyUser(getString(R.string.wrong_device_name));
                deviceNameField.setText(getString(R.string.example_device_name));
            }
        }

    }

    private void findAvailableDevice() {
        notifyUser("The application is about to find the first available headset");
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
        Toast.makeText(HomeActivity.this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(HomeActivity.this,WelcomeActivity.class));
    }
}
