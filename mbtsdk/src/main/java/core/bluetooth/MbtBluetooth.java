package core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import android.support.annotation.Nullable;
import android.util.Log;

import core.device.model.DeviceInfo;

import utils.LogUtils;
import utils.MbtLock;

/**
 *
 * Abstract class that contains all fields and methods that are common to the different bluetooth types.
 * It implements{@link IConnectable} interface as all bluetooth types shares this
 * functionnalities.
 *
 * Created by Etienne on 08/02/2018.
 */

public abstract class MbtBluetooth implements IConnectable{

    private final static String TAG = MbtBluetooth.class.getName();

    @NonNull
    protected IStreamable.StreamState streamingState = IStreamable.StreamState.IDLE;

    private volatile BtState currentState = BtState.IDLE;

    protected boolean isUpdating;


    @Nullable
    protected BluetoothAdapter bluetoothAdapter;

    protected final Context context;

    protected BluetoothDevice currentDevice;

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

    public void notifyDeviceInfoReceived(@NonNull DeviceInfo deviceInfo, @NonNull String deviceValue){ // This method will be called when a DeviceInfoReceived is posted (fw or hw or serial number) by MbtBluetoothLE or MbtBluetoothSPP
        mbtBluetoothManager.notifyDeviceInfoReceived(deviceInfo, deviceValue);
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

            if(currentState.isResettableState(previousState)) {  //if a disconnection occurred
                resetCurrentState();//reset the current connection state to IDLE
                if(this instanceof MbtBluetoothA2DP)
                    mbtBluetoothManager.disconnectAllBluetooth(false); //audio has failed to connect : we disconnect BLE
            }if(currentState.isDisconnectableState())  //if a failure occurred
                disconnect(); //disconnect if a headset is connected

        }
    }

    private void resetCurrentState(){
        notifyConnectionStateChanged(BtState.IDLE);
    }

    public void notifyConnectionStateChanged(BtState newState, boolean notifyManager){
        if(notifyManager)
            notifyConnectionStateChanged(newState);
        else {
            this.currentState = newState;
        }
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
            this.currentState = currentState;
        }
    }

    public void resetMobileDeviceBluetoothAdapter() {
        bluetoothAdapter.disable();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        bluetoothAdapter.enable();
    }

    /**
     * This method is used to clean up the cache that Android system uses
     * when connecting to a known Bluetooth peripheral.
     * It is recommanded to use it right after updating the firmware, especially when the bluetooth
     * characteristics have been updated.
     * @return true if the refresh worked, false otherwise
     */
    public abstract boolean clearMobileDeviceCache();
}
