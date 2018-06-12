package core.bluetooth;

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
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import core.recordingsession.metadata.DeviceInfo;
import utils.AsyncUtils;
import utils.MbtLock;

import static core.bluetooth.MessageStatus.STATE_ACQ;
import static core.bluetooth.MessageStatus.STATE_COMMAND;
import static core.bluetooth.MessageStatus.STATE_COMPRESSION;
import static core.bluetooth.MessageStatus.STATE_FRAME_NB;
import static core.bluetooth.MessageStatus.STATE_IDLE;
import static core.bluetooth.MessageStatus.STATE_LENGTH;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtBluetoothSPP extends MbtBluetooth implements IStreamable {

    private final static String TAG = MbtBluetoothSPP.class.getName();

    private final byte FRAME_HEADER = 0x3C;

    private String deviceAddress;

    private BtState btState;
    private boolean requestDisconnect = false;

    private static final UUID SERVER_UUID = UUID.fromString("0001101-0000-1000-8000-00805f9b34fb");
    private BluetoothSocket btSocket;

    private DataInputStream reader;
    private OutputStream writer;
    private long bytesReceived = 0;

    private MessageStatus currentStatus;

    private Timer keepAliveTimer;

    public MbtBluetoothSPP(@NonNull final Context context, @NonNull MbtBluetoothManager mbtBluetoothManager) {
        super(context, mbtBluetoothManager);
        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = (manager!=null) ? manager.getAdapter() : null;

        this.btState = BtState.DISCONNECTED;
    }

    public MbtBluetoothSPP(@NonNull final Context context, @NonNull final String deviceAddress,@NonNull MbtBluetoothManager mbtBluetoothManager) {
        super(context, mbtBluetoothManager);
        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = manager.getAdapter();

        this.deviceAddress = deviceAddress;
        this.btState = BtState.DISCONNECTED;
    }


    @Override
    public boolean connect(Context context, BluetoothDevice device) {
        if (device != null)
            return connectToDevice(device);
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
        if (this.getCurrentState() == BtState.CONNECTED) {
            Log.i(TAG,"Already connected");
            return false;
        }
        if (!this.bluetoothAdapter.isEnabled()) {
            notifyConnectionStateChanged(BtState.DISABLED);
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
            notifyConnectionStateChanged(BtState.CONNECTING);
            this.btSocket = toConnect.createRfcommSocketToServiceRecord(SERVER_UUID);
            this.btSocket.connect();
            if (retrieveStreams()) {
                AsyncUtils.executeAsync(new Runnable() {
                    public final void run() {
                        listenForIncomingMessages();
                    }
                });
                btState = BtState.CONNECTED;
                notifyConnectionStateChanged(BtState.CONNECTED);
                //notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER, toConnect.getAddress());
                notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER,toConnect.getAddress());
                Log.i(TAG,toConnect.getName() + " Connected");
                return true;
            }
        } catch (final IOException ioe) {
            notifyConnectionStateChanged(BtState.CONNECT_FAILURE);
            Log.e(TAG, "Exception while connecting ->" + ioe.getMessage());
            Log.getStackTraceString(ioe);
        }
        return false;
    }

    @Override
    public boolean disconnect() {
        boolean acquisitionStopped = true;
        if (getCurrentState() != BtState.CONNECTED) {
            Log.i(TAG, "Device already disconnected");
        } else {
            acquisitionStopped = stopStream();
            requestCloseConnexion();
        }
        return acquisitionStopped;
    }

    @Override
    public boolean isConnected() {
        return getCurrentState() == BtState.CONNECTED;
    }

    @Override
    public boolean startStream() {
        Log.i(TAG, "Requested to start stream...");
        final byte[] msg = new byte[] {FRAME_HEADER,0,1,3,0,0,0,1};
        if (btState != BtState.CONNECTED) {
            Log.i(TAG,"Error Not connected!");
            return false;
        }
        else {
            if (sendData(msg)) {
                Log.i(TAG,"Successfully requested to start stream");
                sendKeepAlive(true);
                return true;
            }
            else {
                Log.i(TAG,"Error could not send request to start stream!");
                return false;
            }
        }
    }

    private synchronized boolean sendData(@NonNull final byte[] msg) {
        try {
            if(this.writer != null){
                this.writer.write(msg);
                this.writer.flush();
                Log.i(TAG, "Message sent");
                return true;
            }
            return false;
        } catch (final IOException ioe) {
            this.reader = null;
            this.writer = null;
            Log.e(TAG, "Failed to send data. IOException ->\n" + ioe.getMessage());
            notifyConnectionStateChanged(BtState.DISCONNECTED);
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
                    if (getCurrentState() != BtState.CONNECTED) {
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
        if (this.btState != BtState.CONNECTED )
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
            notifyConnectionStateChanged(BtState.DISCONNECTED);
        } catch (final IOException e) {
            Log.e(TAG, "Error while closing streams -> \n" + e.getMessage());
            notifyConnectionStateChanged(BtState.INTERRUPTED);
            Log.getStackTraceString(e);
        }
    }
    /**
     * Checks if device is already bonded. If not, a Classic Bluetooth Discovery scan
     * will be started to see if it is in range.
     * @return          the melomind if found, <code>null</code> otherwise MBT-VPro
     */
    @Nullable
    private BluetoothDevice scanForDeviceWithDiscovery(final String deviceName) {
        final Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            //TODO change here to use MAC address instead of Name
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(deviceName) /*|| device.getName().contains(deviceName)*/) { // device found
                    return device;
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
                if(action!=null){
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
                            if (name.equals(deviceName) || name.contains(deviceName)) {
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
        return scanLock.waitAndGetResult();
    }

    private boolean retrieveStreams() {
        if (this.btSocket != null) {
            try {
                this.reader = new DataInputStream(this.btSocket.getInputStream());
                this.writer = this.btSocket.getOutputStream();
                boolean is = this.reader!=null && this.writer != null;
                return (is);
            } catch (final IOException ioe) {
                Log.e(TAG, "Failed to retrieve streams ! -> \n" + ioe.getMessage());
                Log.getStackTraceString(ioe);
                notifyConnectionStateChanged(BtState.STREAM_ERROR);
            }
        }
        return false;
    }

    byte[] payloadSizeBuf = new byte[2];
    byte[] data = new byte[0];
    private int command = -1;
    private int counter = 0;
    private int payloadSize = -1;
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
                            Log.e(TAG, "Byte b = " + b);
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
                            Log.i(TAG, "Reading Battery level");
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

            } catch (final Exception e) {
                this.reader = null;
                this.writer = null;
                Log.e(TAG, "Failed to listen. Exception ->\n" + e.getMessage());
                Log.getStackTraceString(e);
                if (this.requestDisconnect) {
                    this.requestDisconnect = false; // consumed
                    notifyConnectionStateChanged(BtState.DISCONNECTED);
                } else
                    notifyConnectionStateChanged(BtState.INTERRUPTED);
                break;
            }
        }
    }


    @Override
    public boolean stopStream() {
        Log.i(TAG, "Requested to stop stream...");
        final byte[] msg = new byte[] {FRAME_HEADER,0,1,3,0,0,0,0};
        if (btState != BtState.CONNECTED) {
            Log.i(TAG,"Error Not connected!");
            return false;
        }
        else {
            if (sendData(msg)) {
                Log.i(TAG,"Successfully requested to stop stream");
                sendKeepAlive(false);
                return true;
            }
            else {
                Log.i(TAG,"Error could not send request to stop stream!");
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
                    if (getCurrentState() != BtState.CONNECTED) {
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

    public void testAcquireDataRandomByte(){ //eeg matrix size
        byte[] data = new byte[250];
        for (int i=0; i<63; i++){//buffer size=6750=62,5*108(Packet size) => matrix size = 6750/3 - 250 (-250 because matrix.get(0) removed for SPP)
            new Random().nextBytes(data); //Generates random bytes and places them into a user-supplied byte array
            this.notifyNewDataAcquired(data);
            Arrays.fill(data,0,0,(byte)0);
        }
    }

}

