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
import android.text.TextUtils;
import android.util.Log;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import config.MbtConfig;
import core.device.model.DeviceInfo;
import core.device.model.MelomindDevice;
import core.oad.OADEvent;

import utils.AsyncUtils;
import utils.LogUtils;
import utils.MbtLock;
import utils.MbtLockNew;

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

    public BtState currentState = BtState.IDLE;

    @Nullable
    protected BluetoothAdapter bluetoothAdapter;

    protected final Context context;

    protected final MbtLockNew scanLock = new MbtLockNew();
    protected final MbtLockNew<Boolean> connectDataLock = new MbtLockNew<>();
    protected final MbtLockNew<Integer> connectAudioLock = new MbtLockNew<>();
    protected final MbtLockNew<Boolean> bondLock = new MbtLockNew<>();

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

        //Checking first if device is already paired.
        final Set<BluetoothDevice> bonded = this.bluetoothAdapter.getBondedDevices();
        if (bonded != null && !bonded.isEmpty()) {
            for (final BluetoothDevice device : bonded) {
                if (MelomindDevice.isMelomindName(device) && device.getName().equals(MbtConfig.getNameOfDeviceRequested())) { // device found
                    return isScanStarted;
                }
            }
        }
        
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
                        if (MbtConfig.getNameOfDeviceRequested() != null && MelomindDevice.isMelomindName(device) && (deviceNameFound.equals(MbtConfig.getNameOfDeviceRequested()) || deviceNameFound.contains(MbtConfig.getNameOfDeviceRequested()))) {
                            LogUtils.i(TAG, "Device " + MbtConfig.getNameOfDeviceRequested() +" found. Cancelling discovery & connecting");
                            bluetoothAdapter.cancelDiscovery();
                            context.unregisterReceiver(this);
                            scanLock.waitAndGetResult();
                        }
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        if (scanLock.isWaiting()) // restarting discovery while still waiting
                            bluetoothAdapter.startDiscovery();
                        break;
                }

            }
        }, filter);
        isScanStarted = bluetoothAdapter.startDiscovery();
        LogUtils.i(TAG, "Scan started.");
        if(isScanStarted && currentState.equals(BtState.READY_FOR_BLUETOOTH_OPERATION)){
            mbtBluetoothManager.updateConnectionState(); //current state is set to SCAN_STARTED
            scanLock.waitAndGetResult(MbtConfig.getBluetoothScanTimeout(), TimeUnit.MILLISECONDS);
        }
        return isScanStarted;
    }

    @Override
    public void stopScanDiscovery() {
        if(scanLock != null && scanLock.isWaiting())
            scanLock.unlock();

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
     * The updateConnectionState() method with no parameter should be call if nothing went wrong and user wants to continue the connection process
     */
    @Override
    public void notifyConnectionStateChanged(@NonNull BtState newState) {
        this.currentState = newState;
        mbtBluetoothManager.notifyConnectionStateChanged(newState);
    }

    public void notifyConnectionStateChanged(BtState newState, boolean notifyManager){
        if(notifyManager)
            notifyConnectionStateChanged(newState);
        else
            this.currentState = newState;
    }

    void notifyMailboxEvent(byte code, Object value){
//        if(this.mailboxEventListener != null){
//            this.mailboxEventListener.onMailBoxEvent(code, value);
//        }
    }

    public void notifyBatteryReceived(int value){
        if(currentState.equals(BtState.BONDING)){
            if(bondLock.isWaiting())
                bondLock.unlock();
        }
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

}
