package core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import config.MbtConfig;
import core.device.model.MbtDevice;
import core.device.model.MelomindDevice;
import core.device.model.VProDevice;
import core.oad.OADEvent;

import core.recordingsession.metadata.DeviceInfo;
import engine.clientevents.MbtClientEvents;
import features.MbtFeatures;
import utils.MbtLock;

/**
 *
 * Abstract class that contains all fields and methods that are common to the different bluetooth types.
 * It implements {@link IScannable} interface and {@link IConnectable} interface as all bluetooth types shares this
 * functionnalities.
 *
 * Created by Etienne on 08/02/2018.
 */

public abstract class MbtBluetooth implements IScannable, IConnectable{

    private final static String TAG = "MBT Bluetooth";
    private BtState currentState = BtState.DISCONNECTED;

    protected BluetoothAdapter bluetoothAdapter;

    protected final Context context;

    protected final MbtLock<BluetoothDevice> scanLock = new MbtLock<>();
    protected List<BluetoothDevice> scannedDevices = new ArrayList<>();

    protected final MbtLock<BtState> connectionLock = new MbtLock<>();

    private MbtDevice melomindDevice;
    private MbtDevice vproDevice;

    protected MbtBluetoothManager mbtBluetoothManager;

    public MbtBluetooth(Context context, MbtBluetoothManager mbtBluetoothManager) {
        this.context = context.getApplicationContext();
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
    public BluetoothDevice startScanDiscovery(final String deviceName) {
//        final Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
//        // If there are paired devices
//        if (pairedDevices.size() > 0) {
//            // Loop through paired devices
//            //TODO change here to use MAC address instead of Name
//            for (BluetoothDevice device : pairedDevices) {
//                if (device.getName().equals(deviceName) /*|| device.getName().contains(deviceName)*/) { // device found
//                    return device;
//                }
//            }
//        }
        // at this point, device was not found among bonded devices so let's start a discovery scan
        final MbtLock<BluetoothDevice> scanLock = new MbtLock<>();
        Log.i(TAG, "Starting Classic Bluetooth Discovery Scan");
        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(new BroadcastReceiver() {
            public final void onReceive(final Context context, final Intent intent) {
                final String action = intent.getAction();
                switch (action) {
                    case BluetoothDevice.ACTION_FOUND:
                        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        final String name = device.getName();
                        if (TextUtils.isEmpty(name)) {
                            Log.w(TAG, "Found device with no name. MAC address is -> " + device.getAddress());
                            return;
                        }

                        Log.i(TAG, String.format("Discovery Scan -> device detected " +
                                "with name '%s' and MAC address '%s' ", device.getName(), device.getAddress()));
                        if (name.equals(deviceName) || name.contains(deviceName)) {
                            Log.i(TAG, "Device " + deviceName +" found. Cancelling discovery & connecting");
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
        }, filter);
        bluetoothAdapter.startDiscovery();
        notifyConnectionStateChanged(BtState.SCAN_STARTED);
        return scanLock.waitAndGetResult();
    }

    @Override
    public void stopScanDiscovery() {
        if(scanLock != null && scanLock.isWaiting()){
            scanLock.setResultAndNotify(null);
        }

        if(bluetoothAdapter.isDiscovering())
            bluetoothAdapter.cancelDiscovery();


    }

    public void notifyDeviceInfoReceived(DeviceInfo deviceInfo, String deviceValue){ // This method will be called when a DeviceInfoReceived is posted (fw or hw or serial number) by MbtBluetoothLE or MbtBluetoothSPP
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

        mbtBluetoothManager.notifyDeviceInfoReceived(deviceInfo, deviceValue);

    }

    void notifyOADEvent(OADEvent eventType, int value){
//        if(this.oadEventListener != null){
//            this.oadEventListener.onOadEvent(eventType, value);
//        }
    }

    @Override
    public void notifyConnectionStateChanged(@NonNull BtState newState) {
        this.currentState = newState;
        mbtBluetoothManager.notifyConnectionStateChanged(newState);

    }

    void notifyMailboxEvent(byte code, Object value){
//        if(this.mailboxEventListener != null){
//            this.mailboxEventListener.onMailBoxEvent(code, value);
//        }
    }

    public void notifyBatteryReceived(int value){
        mbtBluetoothManager.notifyDeviceInfoReceived(DeviceInfo.BATTERY, String.valueOf(value));
    }

    void notifyHeadsetStatusEvent(byte code, int value){
//        if(this.headsetStatusListener != null){
//            if(code == 0x01)
//                this.headsetStatusListener.onSaturationStateChanged(value);
//            else if (code == 0x02)
//                this.headsetStatusListener.onNewDCOffsetMeasured(value);
//        }
    }

    public BtState getCurrentState() { return currentState; }

    public void setCurrentState(BtState state) { this.currentState=state;}

    public void setOadEventListener(MbtClientEvents.OADEventListener oadEventListener) {
        //this.oadEventListener = oadEventListener;
    }

    BluetoothAdapter getBluetoothAdapter() {return bluetoothAdapter;}

    // Events Registration


    public MbtDevice getMelomindDevice() {
        return melomindDevice;
    }

    public MbtDevice getVproDevice() {
        return vproDevice;
    }

    public void notifyNewDataAcquired(@NonNull final byte[] data) {
        mbtBluetoothManager.handleDataAcquired(data);
    }


    public MbtBluetoothManager getMbtBluetoothManager() {
        return mbtBluetoothManager;
    }
}
