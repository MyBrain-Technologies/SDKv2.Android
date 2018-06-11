package core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import config.MbtConfig;
import core.oad.OADEvent;

import core.recordingsession.metadata.DeviceInfo;
import engine.MbtClientEvents;
import features.MbtFeatures;
import model.MbtDevice;
import model.MelomindDevice;
import model.VProDevice;
import utils.MbtLock;

/**
 * Created by Etienne on 08/02/2018.
 */

public abstract class MbtBluetooth implements IScannable, IConnectable{

    private final static String TAG = "MBT Bluetooth";

    // Events Listeners Callbacks
    private MbtClientEvents.StateListener stateListener;
    private MbtClientEvents.EegListener eegListener;
    private MbtClientEvents.DeviceInfoListener deviceInfoListener;
    private MbtClientEvents.OADEventListener oadEventListener;
    private MbtClientEvents.MailboxEventListener mailboxEventListener;
    private MbtClientEvents.HeadsetStatusListener headsetStatusListener;

    private BtState currentState = BtState.DISCONNECTED;

    protected BluetoothAdapter bluetoothAdapter;

    protected final Context context;
    protected final Handler uiAccess;

    protected final MbtLock<BluetoothDevice> scanLock = new MbtLock<>();
    protected List<BluetoothDevice> scannedDevices = new ArrayList<>();

    protected final MbtLock<BtState> connectionLock = new MbtLock<>();

    private MbtDevice melomindDevice;
    private MbtDevice vproDevice;

    protected MbtBluetoothManager mbtBluetoothManager;

    public MbtBluetooth(Context context, MbtBluetoothManager mbtBluetoothManager) {
        this.context = context.getApplicationContext();
        this.uiAccess = new Handler(this.context.getMainLooper());
        this.mbtBluetoothManager = mbtBluetoothManager;

        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            this.bluetoothAdapter = manager.getAdapter();
        }
        if(this.bluetoothAdapter == null){
            this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //try another way to get the adapter
        }

        switch(MbtConfig.getScannableDevices()) {
            case MELOMIND:
                melomindDevice = new MelomindDevice(MbtFeatures.getDeviceName(), MbtFeatures.getSampleRate(), MbtFeatures.getNbChannels(), MbtFeatures.getLocations(), MbtFeatures.getReferences(), MbtFeatures.getGrounds(), MbtConfig.getEegPacketLength());
                break;
            case VPRO:
                vproDevice = new VProDevice(MbtFeatures.getDeviceName(), MbtFeatures.getSampleRate(), MbtFeatures.getNbChannels(), MbtFeatures.getLocations(), MbtFeatures.getReferences(), MbtFeatures.getGrounds(), MbtConfig.getEegPacketLength());
                break;
            case ALL:
                melomindDevice = new MelomindDevice(MbtFeatures.getDeviceName(), MbtFeatures.getSampleRate(), MbtFeatures.getNbChannels(), MbtFeatures.getLocations(), MbtFeatures.getReferences(), MbtFeatures.getGrounds(), MbtConfig.getEegPacketLength());
                vproDevice = new VProDevice(MbtFeatures.getDeviceName(), MbtFeatures.getSampleRate(), MbtFeatures.getNbChannels(), MbtFeatures.getLocations(), MbtFeatures.getReferences(), MbtFeatures.getGrounds(), MbtConfig.getEegPacketLength());
                break;
        }
    }

    @Override
    public void startScanDiscovery(Context context, final String deviceName) {
        final Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            //TODO change here to use MAC address instead of Name
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(deviceName) /*|| device.getName().contains(deviceName)*/) { // device found
                    if (melomindDevice != null) {
                        this.melomindDevice.setBluetoothDevice(device);
                    } else if (vproDevice != null){
                        this.vproDevice.setBluetoothDevice(device);
                    }
                }
            }
        }
        // at this point, device was not found among bonded devices so let's start a discovery scan
        final MbtLock<BluetoothDevice> scanLock = new MbtLock<>();
        Log.i(TAG, "Starting Classic Bluetooth Discovery Scan");
        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(new BroadcastReceiver() {
            public final void onReceive(final Context context, final Intent intent) {
                final String action = intent.getAction();
                if (action != null) {
                    switch(action) {
                        case BluetoothDevice.ACTION_FOUND:
                            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            final String name = device.getName();
                            if (TextUtils.isEmpty(name)) {
                                Log.w(TAG, "Found device with no name. MAC address is -> " + device.getAddress());
                                return;
                            }

                            Log.i(TAG, String.format("Stopping Discovery Scan -> device detected " +
                                    "with name '%s' and MAC address '%s' ", device.getName(), device.getAddress()));
                            if (name.contains(deviceName)) {
                                Log.i(TAG, "VPro found. Cancelling discovery & connecting");
                                bluetoothAdapter.cancelDiscovery();
                                context.unregisterReceiver(this);
                                scanLock.setResultAndNotify(device);
                            }
                            break;
                        case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                            if (scanLock.isWaiting()) // restarting discovery while still waiting
                                bluetoothAdapter.startDiscovery();
                            break;
                    }
                }

            }
        }, filter);
        bluetoothAdapter.startDiscovery();
        if (melomindDevice != null) {
            this.melomindDevice.setBluetoothDevice(scanLock.waitAndGetResult());
        } else if (vproDevice != null){
            this.vproDevice.setBluetoothDevice(scanLock.waitAndGetResult());
        }
    }

    @Override
    public void stopScanDiscovery() {

    }

    public void deviceInfoReceived(DeviceInfo deviceInfo, String deviceValue){ // This method will be called when a DeviceInfoReceived is posted (fw or hw or serial number) by MbtBluetoothLE or MbtBluetoothSPP
        switch(deviceInfo){
            case FW_VERSION:
                melomindDevice.setFirmwareVersion(deviceValue);
                break;

            case HW_VERSION:
                melomindDevice.setHardwareVersion(deviceValue);
                break;

            case SERIAL_NUMBER:
                melomindDevice.setSerialNumber(deviceValue);
                break;

            default:
                break;
        }
        //requestDeviceInformations(event.getDeviceInfo());

    }

    void notifyOADEvent(OADEvent eventType, int value){
        if(this.oadEventListener != null){
            this.oadEventListener.onOadEvent(eventType, value);
        }
    }

    @Override
    public void notifyStateChanged(@NonNull BtState newState) {
        this.currentState = newState;
        if (this.stateListener != null)
            this.stateListener.onStateChanged(newState);
    }

    void notifyMailboxEvent(byte code, Object value){
        if(this.mailboxEventListener != null){
            this.mailboxEventListener.onMailBoxEvent(code, value);
        }
    }

    void notifyHeadsetStatusEvent(byte code, int value){
        if(this.headsetStatusListener != null){
            if(code == 0x01)
                this.headsetStatusListener.onSaturationStateChanged(value);
            else if (code == 0x02)
                this.headsetStatusListener.onNewDCOffsetMeasured(value);
        }
    }

    public BtState getCurrentState() { return currentState; }
    public void setCurrentState(BtState state) { this.currentState=state;}

    public void setOadEventListener(MbtClientEvents.OADEventListener oadEventListener) {
        this.oadEventListener = oadEventListener;
    }

    BluetoothAdapter getBluetoothAdapter() {return bluetoothAdapter;}

    // Events Registration

    void setMailboxEventListener(@Nullable final MbtClientEvents.MailboxEventListener listener){
        this.mailboxEventListener = listener;
    }

    void setEegListener(@Nullable final MbtClientEvents.EegListener listener) {
        this.eegListener = listener;
        if (eegListener == null)
            Log.i(TAG,"eegListener is NULL");
    }

    void setDeviceInfoListener(@Nullable final MbtClientEvents.DeviceInfoListener deviceInfoListener) {
        this.deviceInfoListener = deviceInfoListener;
    }

    void setHeadsetStatusListener(@Nullable final MbtClientEvents.HeadsetStatusListener headsetStatusListener) {
        this.headsetStatusListener = headsetStatusListener;
    }

    void setStateChangeListener(@Nullable final MbtClientEvents.StateListener listener) {
        this.stateListener = listener;
    }

    /**
     * This method reset all listeners to avoid null pointer exceptions and listener leaks
     */
    public void resetAllListeners(){
        setDeviceInfoListener(null);
        setOadEventListener(null);
        setHeadsetStatusListener(null);
        setEegListener(null);
        setCurrentState(null);

    }

    public MbtDevice getMelomindDevice() {
        return melomindDevice;
    }

    public MbtDevice getVproDevice() {
        return vproDevice;
    }

    public void acquireData(@NonNull final byte[] data) {
        mbtBluetoothManager.handleDataAcquired(data);
    }

    public MbtBluetoothManager getMbtBluetoothManager() {
        return mbtBluetoothManager;
    }
}
