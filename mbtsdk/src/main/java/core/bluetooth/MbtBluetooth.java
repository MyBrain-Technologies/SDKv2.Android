package core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import android.util.Log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


import androidx.core.util.Pair;
import core.device.model.DeviceInfo;

import utils.LogUtils;
import utils.MbtAsyncWaitOperation;
import utils.MbtLock;

/**
 *
 * Abstract class that contains all fields and methods that are common to the different bluetooth types.
 * It implements{@link BluetoothInterfaces} interface as all bluetooth types shares this
 * functionnalities.
 *
 * Created by Etienne on 08/02/2018.
 */

public abstract class MbtBluetooth implements BluetoothInterfaces.IConnect, BluetoothInterfaces.IStream{ //todo refactor interfaces > abstract classes : see streamstate comments to understand

    private final static String TAG = MbtBluetooth.class.getName();

    @NonNull
    private StreamState streamState = StreamState.IDLE; //todo streamState variable can be inherited from Stream abstract class instead of IStream interface that just implements methods

    private volatile BtState currentState = BtState.IDLE; //todo add @NonNull annotation + rename into bluetoothState
    protected Pair<String, Long> batteryValueAtTimestamp = null;

    protected boolean isUpdating; //todo remove useless


    @Nullable
    protected BluetoothAdapter bluetoothAdapter;

    protected final Context context;

    protected BluetoothDevice currentDevice;

    protected MbtBluetoothManager mbtBluetoothManager;

    protected BtProtocol protocol;

    private MbtAsyncWaitOperation lock = new MbtAsyncWaitOperation<>();

    public MbtBluetooth(Context context, BtProtocol protocol, MbtBluetoothManager mbtBluetoothManager) {
        this.context = context.getApplicationContext();
        this.protocol = protocol;
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

        if(!newState.equals(currentState) && !(newState.isAFailureState() && currentState.equals(BtState.DATA_BT_DISCONNECTED))){
            BtState previousState = currentState;
            currentState = newState;
            LogUtils.i(TAG," current state is now  =  "+currentState);
            mbtBluetoothManager.notifyConnectionStateChanged(newState);

            if(currentState.isResettableState(previousState)) {  //if a disconnection occurred
                resetCurrentState();//reset the current connection state to IDLE
                if(this instanceof MbtBluetoothA2DP && !currentState.equals(BtState.UPGRADING))
                    mbtBluetoothManager.disconnectAllBluetooth(false); //audio has failed to connect : we disconnect BLE
            }if(currentState.isDisconnectableState())  //if a failure occurred //todo check if a "else" is not missing here
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
        batteryValueAtTimestamp = Pair.create(String.valueOf(value), System.currentTimeMillis());
        mbtBluetoothManager.notifyDeviceInfoReceived(DeviceInfo.BATTERY, String.valueOf(value));//todo keep battery value as integer ?
    }

    void notifyHeadsetStatusEvent(byte code, int value){ //todo : only available in Melomind to this day > see if vpro will need it
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

    public void notifyNewDataAcquired(@NonNull final byte[] data) {//todo call this method to notify the device manager (and all the managers in general) when the bluetooth receives data from the headset (saturation & offset for example instead of notifyNewHeadsetStatus located in the MbtBluetoothLe)
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

    public BluetoothDevice getCurrentDevice() {
        return currentDevice;
    }

    public BtState getCurrentState() { return currentState; }//todo rename getBluetoothState

    void setCurrentState(BtState currentState) {//todo rename setBluetoothState
        if(!this.currentState.equals(currentState)){
            this.currentState = currentState;
        }
    }

    protected boolean isAdapterReady() {
        return bluetoothAdapter != null &&
            bluetoothAdapter.isEnabled() &&
            bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON;
    }

    protected boolean isConnectedDeviceReadyForCommand() {
        return (currentState.ordinal() >= BtState.DATA_BT_CONNECTION_SUCCESS.ordinal());
    }

    /**
     * This method waits until the device has returned a response
     * related to the SDK request (blocking method).
     */
    protected Object startWaitingOperation(int timeout){ //todo rename startWait/wait
        Log.d(TAG, "Wait response of device command ");
        try {
            return lock.waitOperationResult(timeout);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LogUtils.e(TAG, "Device command response not received : "+e);
        }
        return null;
    }

    public void stopWaitingOperation(Object response) {
        lock.stopWaitingOperation(response);
    }

    @VisibleForTesting
    public void setLock(MbtAsyncWaitOperation lock) {
        this.lock = lock;
    }

    /**
     *
     * @return true if a streaming is in progress, false otherwise
     */
    @Override
    public boolean isStreaming() {
        return streamState == StreamState.STARTED || streamState.equals(StreamState.STREAMING);
    }



        /**
         * Whenever there is a new stream state, this method is called to notify the bluetooth manager about it.
         * @param newStreamState the new stream state based on {@link StreamState the StreamState enum}
         */
    @Override
    public void notifyStreamStateChanged(StreamState newStreamState) {
        LogUtils.i(TAG, "new stream state " + newStreamState.toString());

        streamState = newStreamState;
        mbtBluetoothManager.notifyStreamStateChanged(newStreamState);
    }

    /**
     * Disable then enable the bluetooth adapter
     */
    boolean resetMobileDeviceBluetoothAdapter() {
        LogUtils.d(TAG, "Reset Bluetooth adapter");

        bluetoothAdapter.disable();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return enableBluetoothOnDevice();
    }

}
