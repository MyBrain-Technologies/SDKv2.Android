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

import config.MbtConfig;
import core.device.model.DeviceInfo;
import core.device.model.MelomindDevice;
import core.oad.OADEvent;

import utils.LogUtils;
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

    private final static String TAG = MbtBluetooth.class.getName();;

    private volatile BtState currentState = BtState.IDLE;

    @Nullable
    protected BluetoothAdapter bluetoothAdapter;

    protected final Context context;

    protected BluetoothDevice scannedDevice;

    protected MbtBluetoothManager mbtBluetoothManager;

    public MbtBluetooth(Context context, MbtBluetoothManager mbtBluetoothManager) {
        this.context = context.getApplicationContext();
        this.mbtBluetoothManager = mbtBluetoothManager;

        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null)
            this.bluetoothAdapter = manager.getAdapter();

        if(this.bluetoothAdapter == null)
            this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //try another way to get the adapter
    }

    @Nullable
    @Override
    public boolean startScanDiscovery() {
        boolean isScanStarted = false;
        if(bluetoothAdapter == null)
            return isScanStarted;

        // at this point, device was not found among bonded devices so let's start a discovery scan
        LogUtils.i(TAG, "Starting Classic Bluetooth Discovery Scan");
        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(new BroadcastReceiver() {
            public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
                final String action = intent.getAction();
                if(action == null)
                    return;
                switch (action) {
                    case BluetoothDevice.ACTION_FOUND:
                        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        final String deviceNameFound = device.getName();
                        if (TextUtils.isEmpty(deviceNameFound)) {
                            LogUtils.w(TAG, "Found device with no name. MAC address is -> " + device.getAddress());
                            notifyConnectionStateChanged(BtState.SCAN_FAILURE);
                            return;
                        }

                        LogUtils.i(TAG, String.format("Discovery Scan -> device detected " +
                                "with name '%s' and MAC address '%s' ", deviceNameFound, device.getAddress()));
                        if (mbtBluetoothManager.getDeviceNameRequested() != null && MelomindDevice.hasMelomindName(device) && (deviceNameFound.equals(mbtBluetoothManager.getDeviceNameRequested()) || deviceNameFound.contains(mbtBluetoothManager.getDeviceNameRequested()))) {
                            LogUtils.i(TAG, "Device " + mbtBluetoothManager.getDeviceNameRequested() +" found. Cancelling discovery & connecting");
                            bluetoothAdapter.cancelDiscovery();
                            context.unregisterReceiver(this);
                            mbtBluetoothManager.updateConnectionState(true); //current state is set to DEVICE_FOUND and future is completed

                        }
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        if (getCurrentState().equals(BtState.DISCOVERING_SERVICES)) // restarting discovery while still waiting
                            bluetoothAdapter.startDiscovery();
                        break;
                }

            }
        }, filter);
        isScanStarted = bluetoothAdapter.startDiscovery();
        LogUtils.i(TAG, "Scan started.");
        if(isScanStarted && getCurrentState().equals(BtState.READY_FOR_BLUETOOTH_OPERATION)){
            mbtBluetoothManager.updateConnectionState(false); //current state is set to SCAN_STARTED
        }
        return isScanStarted;
    }

    @Override
    public void stopScanDiscovery() {
        if(bluetoothAdapter != null && bluetoothAdapter.isDiscovering())
            bluetoothAdapter.cancelDiscovery();
    }

    public void notifyDeviceInfoReceived(@NonNull DeviceInfo deviceInfo, @NonNull String deviceValue){ // This method will be called when a DeviceInfoReceived is posted (fw or hw or serial number) by MbtBluetoothLE or MbtBluetoothSPP
        mbtBluetoothManager.notifyDeviceInfoReceived(deviceInfo, deviceValue);
    }

    void notifyOADEvent(OADEvent eventType, int value){
//        if(this.oadEventListener != null){
//            this.oadEventListener.onOadEvent(eventType, value);
//        }
    }

    /**
     * Set the current bluetooth connection state to the value given in parameter
     * and notify the bluetooth manager of this change.
     * This method should be called if something went wrong during the connection process, as it stops the connection prccess.
     * The {@link MbtBluetoothManager#updateConnectionState(boolean)}  method with no parameter should be call if nothing went wrong and user wants to continue the connection process
     */
    @Override
    public void notifyConnectionStateChanged(@NonNull BtState newState) {
        if(!newState.equals(currentState)){
            BtState previousState = currentState;
            currentState = newState;
            LogUtils.i(TAG," current state is now  =  "+currentState);
            mbtBluetoothManager.notifyConnectionStateChanged(newState);
            if(currentState.isResettableState(previousState)){ //if a disconnection occurred
                resetCurrentState();//reset the current connection state to IDLE
            }
            if(currentState.isDisconnectableState()){ //if a failure occurred
                disconnect(); //disconnect if a headset is connected
            }
        }
    }

    private void resetCurrentState(){
        LogUtils.i(TAG," reset current state");
        notifyConnectionStateChanged(BtState.IDLE);
    }

    public void notifyConnectionStateChanged(BtState newState, boolean notifyManager){
        if(notifyManager)
            notifyConnectionStateChanged(newState);
        else {
            this.currentState = newState;
        }
    }

    void notifyMailboxEvent(byte code, Object value){
//        if(this.mailboxEventListener != null){
//            this.mailboxEventListener.onMailBoxEvent(code, value);
//        }
    }

    protected void notifyBatteryReceived(int value){
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

    @Nullable
    BluetoothAdapter getBluetoothAdapter() {return bluetoothAdapter;}

    // Events Registration

    public void notifyNewDataAcquired(@NonNull final byte[] data) {
        mbtBluetoothManager.handleDataAcquired(data);
    }

    public MbtBluetoothManager getMbtBluetoothManager() {
        return mbtBluetoothManager;
    }

    public synchronized final boolean enableBluetoothOnDevice(){
        if (this.bluetoothAdapter != null && this.bluetoothAdapter.isEnabled())
            return true;

        final MbtLock<Boolean> lock = new MbtLock<>();
        // Registering for bluetooth state change
        final IntentFilter btStateFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(new BroadcastReceiver() {
            public final void onReceive(final Context context, final Intent intent) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if (state == BluetoothAdapter.STATE_ON) {
                    // Bluetooth is now turned on
                    context.unregisterReceiver(this);
                    lock.setResultAndNotify(Boolean.TRUE);
                }
            }
        }, btStateFilter);

        // Turning Bluetooth ON and waiting...
        this.bluetoothAdapter.enable();
        Boolean b = lock.waitAndGetResult(5000);
        if(b == null){
            Log.e(TAG, "impossible to enable BT adapter");
            return false;
        }
        return b;
    }

    public BtState getCurrentState() { return currentState; }

    void setCurrentState(BtState currentState) {
        if(!this.currentState.equals(currentState)){
            LogUtils.i(TAG,"set current state was = "+currentState);
            this.currentState = currentState;
            LogUtils.i(TAG,"is now = "+currentState);
        }
    }
}
