package core.bluetooth.spp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import config.EegStreamConfig;
import core.bluetooth.BtState;
import core.bluetooth.IStreamable;
import core.bluetooth.MbtBluetooth;
import core.bluetooth.MbtBluetoothManager;
import core.device.model.DeviceInfo;
import core.device.model.MelomindDevice;
import utils.AsyncUtils;
import utils.LogUtils;

import static core.bluetooth.spp.MessageStatus.STATE_ACQ;
import static core.bluetooth.spp.MessageStatus.STATE_COMMAND;
import static core.bluetooth.spp.MessageStatus.STATE_COMPRESSION;
import static core.bluetooth.spp.MessageStatus.STATE_FRAME_NB;
import static core.bluetooth.spp.MessageStatus.STATE_IDLE;
import static core.bluetooth.spp.MessageStatus.STATE_LENGTH;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtBluetoothSPP extends MbtBluetooth implements IStreamable {

    private final static String TAG = MbtBluetoothSPP.class.getName();

    private final byte FRAME_HEADER = 0x3C;

    private String deviceAddress;

    private boolean requestDisconnect = false;

    private static final UUID SERVER_UUID = UUID.fromString("0001101-0000-1000-8000-00805f9b34fb");
    @Nullable
    private BluetoothSocket btSocket;

    @Nullable
    private DataInputStream reader;
    @Nullable
    private OutputStream writer;
    private long bytesReceived = 0;

    private MessageStatus currentStatus;

    private Timer keepAliveTimer;

    public MbtBluetoothSPP(@NonNull final Context context, @NonNull MbtBluetoothManager mbtBluetoothManager) {
        super(context, mbtBluetoothManager);
        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = (manager!=null) ? manager.getAdapter() : null;
    }

    public MbtBluetoothSPP(@NonNull final Context context, @NonNull final String deviceAddress,@NonNull MbtBluetoothManager mbtBluetoothManager) {
        super(context, mbtBluetoothManager);
        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = manager.getAdapter();
        this.deviceAddress = deviceAddress;
    }


    @Nullable
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

    public void stopScanDiscovery() {
        if(bluetoothAdapter != null && bluetoothAdapter.isDiscovering())
            bluetoothAdapter.cancelDiscovery();
    }

    @Override
    public boolean connect(Context context, @Nullable BluetoothDevice device) {
        if (device != null) {
            LogUtils.i(TAG," connect  "+device.getName());
            return connectToDevice(device);
        }
        return false;
    }
    /**
     * Connects to the specified device.
     * <p><strong>Node:</strong> This is a blocking call. It should therefore NOT be made in the <code>Main UI Thread</code>
     * as stated by the <code>@WorkerThread</code> annotation</p>
     * @param device the device hardware MAC address. Cannot be <code>null</code>
     * @return <code>true</code> upon success, <code>false</code> otherwise
     */
    @WorkerThread
    synchronized final boolean connectToDevice(@NonNull final BluetoothDevice device) {
        BluetoothDevice toConnect = null;
        if (isConnected()) {
            LogUtils.i(TAG,"Already connected");
            notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
            return false;
        }
        if (!this.bluetoothAdapter.isEnabled()) {
            notifyConnectionStateChanged(BtState.BLUETOOTH_DISABLED);
            return false;
        }

        for (final BluetoothDevice bonded : this.bluetoothAdapter.getBondedDevices())
            if (bonded.getAddress().equals(device.getAddress())) {
                toConnect = bonded;
                break;
            }

        if (toConnect == null)
            toConnect = device;

        try {
            notifyConnectionStateChanged(BtState.DATA_BT_CONNECTING);
            this.btSocket = toConnect.createRfcommSocketToServiceRecord(SERVER_UUID);
            this.btSocket.connect();
            if (retrieveStreams()) {
                AsyncUtils.executeAsync(new Runnable() {
                    public final void run() {
                        listenForIncomingMessages();
                    }
                });
                notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
                notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER,toConnect.getAddress());
                LogUtils.i(TAG,toConnect.getName() + " Connected");
                return true;
            }
        } catch (@NonNull final IOException ioe) {
            notifyConnectionStateChanged(BtState.CONNECTION_FAILURE);
            LogUtils.e(TAG, "Exception while connecting ->" + ioe.getMessage());
            Log.getStackTraceString(ioe);
        }
        return false;
    }

    @Override
    public boolean disconnect() {
        boolean acquisitionStopped = true;
        if (!isConnected()) {
            LogUtils.i(TAG, "Device already disconnected");
        } else {
            acquisitionStopped = stopStream();
            requestCloseConnexion();
        }
        return acquisitionStopped;
    }

    @Override
    public boolean isConnected() {
        return getCurrentState() == BtState.CONNECTED_AND_READY;
    }

    @Override
    public boolean startStream() {
        LogUtils.i(TAG, "Requested to start stream...");
        final byte[] msg = new byte[] {FRAME_HEADER,0,1,3,0,0,0,1};
        if (!isConnected()) {
            LogUtils.i(TAG,"Error Not connected!");
            return false;
        }
        else {
            if (sendData(msg)) {
                LogUtils.i(TAG,"Successfully requested to start stream");
                sendKeepAlive(true);
                return true;
            }
            else {
                LogUtils.i(TAG,"Error could not send request to start stream!");
                return false;
            }
        }
    }

    private synchronized boolean sendData(@NonNull final byte[] msg) {
        try {
            if(this.writer != null){
                this.writer.write(msg);
                this.writer.flush();
                LogUtils.i(TAG, "Message sent");
                return true;
            }
            return false;
        } catch (@NonNull final IOException ioe) {
            this.reader = null;
            this.writer = null;
            LogUtils.e(TAG, "Failed to send data. IOException ->\n" + ioe.getMessage());
            notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
            Log.getStackTraceString(ioe);
            return false;
        }
    }

    private void sendKeepAlive(boolean keepAlive) {
        final byte[] msg = {FRAME_HEADER,0,1,3,0,0,0,1};
        if (keepAlive) {
            if (this.keepAliveTimer != null)
                this.keepAliveTimer.cancel();
            this.keepAliveTimer = new Timer(true);
            this.keepAliveTimer.scheduleAtFixedRate(new TimerTask() {
                public final void run() {
                    if (!isConnected()) {
                        cancel(); // something is wrong
                        return;
                    }
                    // safe to call
                    sendData(msg);
                }
            }, 0, 1000);
        } else {
            if (this.keepAliveTimer != null)
                this.keepAliveTimer.cancel();
        }
    }

    final void requestCloseConnexion() {
        if (!isConnected())
            throw new IllegalStateException("Cannot Stop Services : no services or no ongoing connexion!");

        this.requestDisconnect = true;

        // Now attempting to properly disconnect
        try {
            if (this.reader != null) {
                this.reader.close();
                this.reader = null;
            }
            if (this.writer != null) {
                this.writer.close();
                this.writer = null;
            }
            if (this.btSocket != null) {
                this.btSocket.close();
                this.btSocket = null;
            }
            notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
        } catch (@NonNull final IOException e) {
            LogUtils.e(TAG, "Error while closing streams -> \n" + e.getMessage());
            notifyConnectionStateChanged(BtState.CONNECTION_INTERRUPTED);
            Log.getStackTraceString(e);
        }
    }

    private boolean retrieveStreams() {
        if (this.btSocket != null) {
            try {
                this.reader = new DataInputStream(this.btSocket.getInputStream());
                this.writer = this.btSocket.getOutputStream();
                boolean is = this.reader!=null && this.writer != null;
                return (is);
            } catch (@NonNull final IOException ioe) {
                LogUtils.e(TAG, "Failed to retrieve streams ! -> \n" + ioe.getMessage());
                Log.getStackTraceString(ioe);
                notifyConnectionStateChanged(BtState.STREAM_ERROR);
            }
        }
        return false;
    }

    @NonNull
    byte[] payloadSizeBuf = new byte[2];
    @NonNull
    byte[] data = new byte[0];
    private int command = -1;
    private int counter = 0;
    private int payloadSize = -1;
    @NonNull
    private byte[] pcktNumber = new byte[2];

    private void listenForIncomingMessages() {

        currentStatus = STATE_IDLE;
        while (this.reader != null && this.writer != null) {

            try {
                byte b = this.reader.readByte();

                switch(currentStatus){

                    case STATE_IDLE:
                        if(b == FRAME_HEADER){
                            currentStatus = STATE_LENGTH;
                        }else if(b != 0){
                            LogUtils.e(TAG, "Byte b = " + b);
                        }
                        break;

                    case STATE_LENGTH:
                        payloadSizeBuf[counter++] = b;
                        if(counter == 2){
                            counter = 0;
                            payloadSize = ((payloadSizeBuf[0] & 0xFF) << 8) + (payloadSizeBuf[1] & 0xFF);
                            data = new byte[payloadSize + 3];
                            currentStatus = STATE_COMMAND;
                        }
                        break;

                    case STATE_COMMAND:
                        command = b ;
                        if(command != 3 && command != 4){
                            currentStatus = STATE_IDLE;
                        }else{
                            currentStatus = STATE_COMPRESSION;
                        }
                        break;

                    case STATE_COMPRESSION:
                        if(b == 0x00 || b == 0x01){
                            data[counter++] = b;
                            currentStatus = STATE_FRAME_NB;
                        }
                        else{
                            currentStatus = STATE_IDLE;
                        }
                        break;

                    case STATE_FRAME_NB:

                        data[counter++] = b;
                        if(counter == 3){
                            pcktNumber[0] = data[1];
                            pcktNumber[1] = data[2];
                            currentStatus = STATE_ACQ;
                        }
                        break;

                    case STATE_ACQ:
                        if(command == 3) {
                            data[counter++] = b;

                            if (counter == payloadSize + 3) {
                                counter = 0;
                                currentStatus = STATE_IDLE;


                                final byte[] finalData =  (byte[])data.clone();//Arrays.copyOf(data, data.length);
                                AsyncUtils.executeAsync(new Runnable() {
                                    //private final byte[] toAcquire = Arrays.copyOf(finalData, finalData.length);
                                    public void run() {
                                        notifyNewDataAcquired(finalData);
                                    }
                                });
                            }

                        }else if(command == 4) {
                            LogUtils.i(TAG, "Reading Battery level");
                            data = new byte[payloadSize];
                            int level = b;
                            counter = 0;
                            currentStatus = STATE_IDLE;
                            int pourcent = -1;
                            switch(level) {
                                case 0:
                                    pourcent = 0;
                                    break;
                                case 1:
                                    pourcent = 15;
                                    break;
                                case 2:
                                    pourcent = 30;
                                    break;
                                case 3:
                                    pourcent = 50;
                                    break;
                                case 4:
                                    pourcent = 65;
                                    break;
                                case 5:
                                    pourcent = 85;
                                    break;
                                case 6:
                                    pourcent = 100;
                                    break;
                                default:
                                    break;
                            }
                            //mbtBluetoothManager.updateBatteryLevel(pourcent);
                        }else {
                            //TODO here are non implemented cases. Please see the MBT SPP protocol for infos.
                        }
                        break;
                }

            } catch (@NonNull final Exception e) {
                this.reader = null;
                this.writer = null;
                LogUtils.e(TAG, "Failed to listen. Exception ->\n" + e.getMessage());
                Log.getStackTraceString(e);
                if (this.requestDisconnect) {
                    this.requestDisconnect = false; // consumed
                    notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
                } else
                    notifyConnectionStateChanged(BtState.CONNECTION_INTERRUPTED);
                break;
            }
        }
    }


    @Override
    public boolean stopStream() {
        LogUtils.i(TAG, "Requested to stop stream...");
        final byte[] msg = new byte[] {FRAME_HEADER,0,1,3,0,0,0,0};
        if (!isConnected()) {
            LogUtils.i(TAG,"Error Not connected!");
            return false;
        }
        else {
            if (sendData(msg)) {
                LogUtils.i(TAG,"Successfully requested to stop stream");
                sendKeepAlive(false);
                return true;
            }
            else {
                LogUtils.i(TAG,"Error could not send request to stop stream!");
                return false;
            }
        }
    }

    @Override
    public void notifyStreamStateChanged(StreamState streamState) {

    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    /**
     * Ask to get the battery level
     * @param start is true for starting the battery timer and false for cancelling the battery timer
     */
    private Timer batteryTimer;
    void askForBatteryLevel(final boolean start) {
        final byte[] msg = {FRAME_HEADER,0,1,4,0,0,0,1};
        if (start) {
            if (this.batteryTimer != null)
                this.batteryTimer.cancel();
            this.batteryTimer = new Timer(true);
            this.batteryTimer.scheduleAtFixedRate(new TimerTask() {
                public final void run() {
                    if (!isConnected()) {
                        cancel(); // something is wrong
                        return;
                    }
                    // safe to call
                    sendData(msg);
                }
            }, 0, 300000); // 5 minutes
        } else {
            if (this.batteryTimer != null)
                this.batteryTimer.cancel();
        }
    }

    //todo
    public void sendDeviceCommand(EegStreamConfig config) {
    }
}

