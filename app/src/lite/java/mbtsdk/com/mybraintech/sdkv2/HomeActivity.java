package mbtsdk.com.mybraintech.sdkv2;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
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
import java.util.Arrays;

import core.bluetooth.BtState;
import config.ConnectionConfig;
import core.device.model.MbtDevice;
import core.device.model.MelomindDevice;
import engine.MbtClient;
import engine.SimpleRequestCallback;
import engine.clientevents.BaseError;

import engine.clientevents.BluetoothStateListener;
import features.MbtDeviceType;
import features.MbtFeatures;

import static features.MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX;
import static features.MbtFeatures.QR_CODE_NAME_PREFIX;
import static features.MbtFeatures.VPRO_DEVICE_NAME_PREFIX;
import static features.MbtDeviceType.MELOMIND;
import static features.MbtDeviceType.VPRO;

public class HomeActivity extends AppCompatActivity{

    private static String TAG = HomeActivity.class.getName();
    /**
     * Duration to find a headset
     */
    private final static int SCAN_DURATION = 20000;

    public final static String DEVICE_NAME = "DEVICE_NAME";
    public final static String DEVICE_QR_CODE = "DEVICE_QR_CODE";
    public final static String DEVICE_TYPE = "DEVICE_TYPE";
    public final static String BLUETOOTH_STATE = "BLUETOOTH_STATE";
    public final static String PREVIOUS_ACTIVITY = "PREVIOUS_ACTIVITY";

    private MbtClient client;

    private EditText deviceNameField;
    private String deviceName;
    private EditText deviceQrCodeField;
    private String deviceQrCode;

    private MbtDeviceType deviceType;

    private Switch connectAudioSwitch;
    private boolean connectAudioIfDeviceCompatible = false;

    private Spinner deviceNamePrefixSpinner;
    private String deviceNamePrefix;
    private ArrayList<String> prefixNameList;
    private ArrayAdapter<String> prefixNameArrayAdapter;

    private Spinner deviceQrCodePrefixSpinner;
    private String deviceQrCodePrefix;
    private ArrayList<String> prefixQrCodeList;
    private ArrayAdapter<String> prefixQrCodeArrayAdapter;

    private Button scanButton;

    private boolean isCancelled = false;
    private boolean isErrorRaised = false;

    private Toast toast;

    private BluetoothStateListener bluetoothStateListener = new BluetoothStateListener() {
        @Override
        public void onNewState(BtState newState, MbtDevice device) {
            Log.d(TAG, "new state received "+newState);
            if(newState.equals(BtState.READING_SUCCESS)){ //SERIAL NUMBER VALUE HAS NOT BEEN READ BEFORE THIS STEP
                client.requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
                    @Override
                    public void onRequestComplete(final MbtDevice device) {
                        Log.d(TAG, "device received "+device);
                        if(device != null) {
                            runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      deviceName = device.getProductName();
                                      String deviceNameToDisplay = deviceName.replace(MELOMIND_DEVICE_NAME_PREFIX,"");
                                      deviceNameField.setText(deviceNameToDisplay);
                                      for(String prefix : prefixNameList){
                                          if(device.getSerialNumber() != null && device.getProductName().startsWith(prefix))
                                              deviceNamePrefixSpinner.setSelection(prefixNameArrayAdapter.getPosition(prefix));
                                      }
                                      deviceQrCode = device.getExternalName();
                                      String deviceQrCodeToDisplay = deviceQrCode.replace(QR_CODE_NAME_PREFIX,"");
                                      deviceQrCodeField.setText(deviceQrCodeToDisplay);
                                      for(String prefix : prefixQrCodeList){
                                          if(device.getExternalName() != null && device.getExternalName().startsWith(prefix))
                                              deviceQrCodePrefixSpinner.setSelection(prefixQrCodeArrayAdapter.getPosition(prefix));
                                      }
                                      deviceType = ( device instanceof MelomindDevice ? MELOMIND : VPRO);
                                  }
                              });
                        }
                    }
                });
            }
        }

        @Override
        public void onError(BaseError error, String additionalInfo) {
            Log.e(TAG, "onError received "+error.getMessage()+ (additionalInfo != null ? additionalInfo : ""));
            isErrorRaised = true;
            updateScanning(false);
            toast = Toast.makeText(HomeActivity.this, error.getMessage()+ (additionalInfo != null ? additionalInfo : ""), Toast.LENGTH_LONG);
            toast.show();
        }

        @Override
        public void onDeviceConnected(MbtDevice device) {
            toast.cancel();
            deinitCurrentActivity(true);
        }

        @Override
        public void onDeviceDisconnected() {
            if(!toast.getView().isShown())
                notifyUser(getString(R.string.no_connected_headset));
            if(isCancelled)
                updateScanning(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initToolBar();
        toast = Toast.makeText(HomeActivity.this, "", Toast.LENGTH_LONG);
        client = MbtClient.getClientInstance();
        isCancelled = false;

        if(getIntent().hasExtra(HomeActivity.PREVIOUS_ACTIVITY)){
            if(getIntent().getStringExtra(HomeActivity.PREVIOUS_ACTIVITY)!=null)
                client.setConnectionStateListener(bluetoothStateListener);
        }

        initDeviceNameField();
        initDeviceQrCodeField();
        initConnectAudioSwitch();
        initScanButton();
        initDeviceNamePrefix();
        initDeviceQrCodePrefix();
    }

    private void initConnectAudioSwitch() {
        connectAudioSwitch = findViewById(R.id.connectAudio);
    }

    private void initDeviceNamePrefix() {
        deviceNamePrefixSpinner = findViewById(R.id.deviceNamePrefix);
        prefixNameList = new ArrayList<>(Arrays.asList(MELOMIND_DEVICE_NAME_PREFIX, VPRO_DEVICE_NAME_PREFIX));
        prefixNameArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, prefixNameList);
        prefixNameArrayAdapter.setDropDownViewResource(R.layout.spinner_item);
        deviceNamePrefixSpinner.setAdapter(prefixNameArrayAdapter);
        deviceNamePrefixSpinner.setSelection(prefixNameArrayAdapter.getPosition(MELOMIND_DEVICE_NAME_PREFIX));
    }

    private void initDeviceQrCodePrefix() {
        deviceQrCodePrefixSpinner = findViewById(R.id.deviceQrCodePrefix);
        prefixQrCodeList = new ArrayList<>(Arrays.asList(QR_CODE_NAME_PREFIX));
        prefixQrCodeArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, prefixQrCodeList);
        prefixQrCodeArrayAdapter.setDropDownViewResource(R.layout.spinner_item);
        deviceQrCodePrefixSpinner.setAdapter(prefixQrCodeArrayAdapter);
        deviceQrCodePrefixSpinner.setSelection(prefixQrCodeArrayAdapter.getPosition(QR_CODE_NAME_PREFIX));
    }


    private void initDeviceNameField() {
        deviceNameField = findViewById(R.id.deviceNameField);
    }

    private void initDeviceQrCodeField() {
        deviceQrCodeField = findViewById(R.id.deviceQrCodeField);
    }

    private void initScanButton(){
        scanButton = findViewById(R.id.scanButton);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyUser(getString(R.string.scan_in_progress));
                deviceNamePrefix = String.valueOf(deviceNamePrefixSpinner.getSelectedItem()); //get the prefix chosen by the user in the Spinner
                deviceName = deviceNamePrefix+deviceNameField.getText().toString(); //get the name entered by the user in the EditText

                deviceQrCodePrefix = String.valueOf(deviceQrCodePrefixSpinner.getSelectedItem()); //get the prefix chosen by the user in the Spinner
                deviceQrCode = deviceQrCodePrefix+deviceQrCodeField.getText().toString(); //get the name entered by the user in the EditText

                deviceType = isMelomindDevice() ? MELOMIND : VPRO;
                connectAudioIfDeviceCompatible = connectAudioSwitch.isChecked();

                if(isCancelled) //Scan in progress : a second click means that the user is trying to cancel the scan
                   cancelScan();
                else // Scan is not in progress : starting a new scan in order to connect to a Mbt Device
                    startScan();
                if(!isErrorRaised)
                    updateScanning(!isCancelled);
            }
        });
    }


    private void startScan() {
        isErrorRaised = false;
        ConnectionConfig.Builder builder = new ConnectionConfig.Builder(bluetoothStateListener)
                .deviceName(
                        (deviceName != null && deviceName.equals(MELOMIND_DEVICE_NAME_PREFIX)) ? //if no name has been entered by the user, the default device name is the headset prefix
                                null : deviceName ) //null is given in parameters if no name has been entered by the user
                .deviceQrCode(
                        ((deviceQrCode != null) && (deviceQrCode.equals(QR_CODE_NAME_PREFIX)) ) ? //if no QR code has been entered by the user, the default device name is the headset prefix
                                null : deviceQrCode )
                .maxScanDuration(SCAN_DURATION);
        if(connectAudioIfDeviceCompatible) {
            builder.connectAudio();
        }
        client.connectBluetooth(builder.create());

    }

    private void cancelScan(){
        client.cancelConnection();
    }

    /**
     * Updates the scanning state boolean and the Scan button text
     * The Scan button text is changed into into "Cancel" if scanning is launched
     * or into "Find a device" if scanning is cancelled
     */
    private void updateScanning(boolean newIsCancelled){
        isCancelled = newIsCancelled;
        if(!isCancelled)
            toast.cancel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            scanButton.setBackgroundColor((isCancelled ? Color.LTGRAY : getColor(R.color.light_blue)));

        scanButton.setText((isCancelled ? R.string.cancel : R.string.scan));
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
        bluetoothStateListener = null;
    }

    private void initToolBar(){
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.logo);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.light_blue)));
        }
    }

    private void deinitCurrentActivity(boolean isConnected){
        bluetoothStateListener = null;
        final Intent intent = new Intent(HomeActivity.this, DeviceActivity.class);
        intent.putExtra(DEVICE_NAME, deviceName);
        intent.putExtra(DEVICE_QR_CODE, deviceQrCode);
        intent.putExtra(DEVICE_TYPE, deviceType);
        intent.putExtra(BLUETOOTH_STATE, isConnected);
        startActivity(intent);
        finish();
    }
}
