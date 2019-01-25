package core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;

import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import core.device.model.MelomindDevice;
import core.oad.OADEvent;

import core.recordingsession.metadata.DeviceInfo;
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

    private final static String TAG = "MBT Bluetooth";
    protected final static String CONNECT_METHOD = "connect";
    protected final static String CONNECT_GATT_METHOD = "connectGatt";
    protected final static String DISCONNECT_METHOD = "disconnect";
    protected final static String REMOVE_BOND_METHOD = "removeBond";
    private BtState currentState = BtState.IDLE;

    @Nullable
    protected BluetoothAdapter bluetoothAdapter;

    protected final Context context;

    protected final MbtLock<BluetoothDevice> scanLock = new MbtLock<>();
    @NonNull
    protected List<BluetoothDevice> scannedDevices = new ArrayList<>();

    protected final MbtLock<BtState> connectionLock = new MbtLock<>();

    protected MbtBluetoothManager mbtBluetoothManager;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public MbtBluetooth(Context context, MbtBluetoothManager mbtBluetoothManager) {
        this.context = context.getApplicationContext();
        this.mbtBluetoothManager = mbtBluetoothManager;

        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null)
            this.bluetoothAdapter = manager.getAdapter();

        if(this.bluetoothAdapter == null)
            this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //try another way to get the adapter

    }

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    @Nullable
    @Override
    public BluetoothDevice startScanDiscovery(@Nullable final String deviceName) {
        if(bluetoothAdapter == null)
            return null;

        //Checking first if device is already paired.
        final Set<BluetoothDevice> bonded = this.bluetoothAdapter.getBondedDevices();
        if (bonded != null && !bonded.isEmpty()) {
            for (final BluetoothDevice device : bonded) {
                if (MelomindDevice.isMelomind(device) && device.getName().equals(deviceName)) { // device found
                    return device;
                }
            }
        }
        
        // at this point, device was not found among bonded devices so let's start a discovery scan
        LogUtils.i(TAG, "Starting Classic Bluetooth Discovery Scan");
        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
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
                            notifyConnectionStateChanged(BtState.INTERNAL_FAILURE, true);
                            return;
                        }

                        LogUtils.i(TAG, String.format("Discovery Scan -> device detected " +
                                "with name '%s' and MAC address '%s' ", deviceNameFound, device.getAddress()));
                        if (deviceName != null && MelomindDevice.isMelomind(device) && (deviceNameFound.equals(deviceName) || deviceNameFound.contains(deviceName))) {
                            LogUtils.i(TAG, "Device " + deviceName +" found. Cancelling discovery & connecting");
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
        notifyConnectionStateChanged(BtState.SCAN_STARTED, true);
        return scanLock.waitAndGetResult();
    }

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    @Override
    public void stopScanDiscovery() {
        if(scanLock != null && scanLock.isWaiting()){
            scanLock.setResultAndNotify(null);
        }

        if(bluetoothAdapter != null && bluetoothAdapter.isDiscovering())
            bluetoothAdapter.cancelDiscovery();


    }

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    public void notifyDeviceInfoReceived(@NonNull DeviceInfo deviceInfo, @NonNull String deviceValue){ // This method will be called when a DeviceInfoReceived is posted (fw or hw or serial number) by MbtBluetoothLE or MbtBluetoothSPP
        mbtBluetoothManager.notifyDeviceInfoReceived(deviceInfo, deviceValue);
    }

    void notifyOADEvent(OADEvent eventType, int value){
//        if(this.oadEventListener != null){
//            this.oadEventListener.onOadEvent(eventType, value);
//        }
    }

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    @Override
    public void notifyConnectionStateChanged(@NonNull BtState newState, boolean notifyUserClient) {
        this.currentState = newState;
        mbtBluetoothManager.notifyConnectionStateChanged(newState, notifyUserClient);
    }

    void notifyMailboxEvent(byte code, Object value){
//        if(this.mailboxEventListener != null){
//            this.mailboxEventListener.onMailBoxEvent(code, value);
//        }
    }

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
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

    public void setCurrentState(BtState state) { this.currentState = state;}

    @Nullable
    BluetoothAdapter getBluetoothAdapter() {return bluetoothAdapter;}

    // Events Registration

    public void notifyNewDataAcquired(@NonNull final byte[] data) {
        mbtBluetoothManager.handleDataAcquired(data);
    }

    public MbtBluetoothManager getMbtBluetoothManager() {
        return mbtBluetoothManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
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
