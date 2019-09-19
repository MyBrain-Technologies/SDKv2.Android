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


import org.apache.commons.lang.ArrayUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import command.CommandInterface;
import command.DeviceCommandEvent;

import command.DeviceCommands;
import command.DeviceStreamingCommands;
import core.bluetooth.BtProtocol;
import core.bluetooth.BtState;
import core.bluetooth.MbtBluetoothManager;
import core.bluetooth.MbtDataBluetooth;
import core.bluetooth.StreamState;
import core.device.model.DeviceInfo;
import engine.clientevents.BluetoothError;
import utils.AsyncUtils;
import utils.LogUtils;

import static command.DeviceCommandEvent.CMD_GET_BATTERY_VALUE;
import static command.DeviceCommandEvent.CMD_GET_DEVICE_INFO;
import static command.DeviceCommandEvent.CMD_START_EEG_ACQUISITION;
import static command.DeviceCommandEvent.MBX_GET_EEG_CONFIG;
import static command.DeviceCommandEvent.START_FRAME;
import static core.bluetooth.spp.MessageStatus.STATE_ACQ;
import static core.bluetooth.spp.MessageStatus.STATE_COMMAND;
import static core.bluetooth.spp.MessageStatus.STATE_COMPRESSION;
import static core.bluetooth.spp.MessageStatus.STATE_FRAME_NB;
import static core.bluetooth.spp.MessageStatus.STATE_IDLE;
import static core.bluetooth.spp.MessageStatus.STATE_LENGTH;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtBluetoothSPP
        extends MbtDataBluetooth {

    private final static String TAG = MbtBluetoothSPP.class.getName();

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

    private boolean isStreaming = false;

    private MessageStatus currentStatus;

    private Timer keepAliveTimer;

    private final static int DEFAULT_COMMAND_BUFFER_LENGTH = 11;

    private BroadcastReceiver scanReceiver = new BroadcastReceiver() {
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
                        return;
                    }

                    LogUtils.i(TAG, String.format("Discovery Scan -> device detected " +
                            "with name '%s' and MAC address '%s' ", deviceNameFound, device.getAddress()));
                    if (mbtBluetoothManager.getDeviceNameRequested() != null
                            && (deviceNameFound.equals(mbtBluetoothManager.getDeviceNameRequested()) || deviceNameFound.contains(mbtBluetoothManager.getDeviceNameRequested()))) {
                        LogUtils.i(TAG, "Device " + mbtBluetoothManager.getDeviceNameRequested() +" found. Cancelling discovery & connecting");
                        currentDevice = device;
                        bluetoothAdapter.cancelDiscovery();
                        mbtBluetoothManager.updateConnectionState(true); //current state is set to DEVICE_FOUND and future is completed

                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    bluetoothAdapter.startDiscovery();

                    break;
            }

        }
    };

    public MbtBluetoothSPP(@NonNull final Context context, @NonNull MbtBluetoothManager mbtBluetoothManager) {
        super(context, BtProtocol.BLUETOOTH_SPP, mbtBluetoothManager);
        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = (manager!=null) ? manager.getAdapter() : null;
    }

    public MbtBluetoothSPP(@NonNull final Context context, @NonNull final String deviceAddress,@NonNull MbtBluetoothManager mbtBluetoothManager) {
        super(context, BtProtocol.BLUETOOTH_SPP, mbtBluetoothManager);
        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = manager.getAdapter();
        this.deviceAddress = deviceAddress;
    }

    @Override
    public boolean startScan() {
        boolean isScanStarted = false;
        if(bluetoothAdapter == null)
            return isScanStarted;

        // at this point, device was not found among bonded devices so let's start a discovery scan
        LogUtils.i(TAG, "Starting Classic Bluetooth Discovery Scan");
        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(scanReceiver, filter);
        isScanStarted = bluetoothAdapter.startDiscovery();
        LogUtils.i(TAG, "Scan started.");
        if(isScanStarted && getCurrentState().equals(BtState.READY_FOR_BLUETOOTH_OPERATION)){
            mbtBluetoothManager.updateConnectionState(false); //current state is set to SCAN_STARTED
        }
        return isScanStarted;
    }

    @Override
    public void stopScan() {
        if(bluetoothAdapter != null && bluetoothAdapter.isDiscovering())
            bluetoothAdapter.cancelDiscovery();
        context.unregisterReceiver(scanReceiver);
    }

    @Override
    public boolean connect(Context context, @Nullable BluetoothDevice device) {
        if (device != null) {
            LogUtils.i(TAG," Connect  "+device.getName());
            this.requestDisconnect = false;
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
                    @Override
                    public void run() {
                        listenForIncomingMessages();
                    }
                });
                notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
                notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER, toConnect.getAddress());
                sendCommand(new DeviceCommands.GetDeviceInfo());
                LogUtils.i(TAG,toConnect.getName() + " Connected");
                return true;
            }else
                notifyConnectionStateChanged(BtState.CONNECTION_FAILURE);

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
        final byte[] msg = new DeviceStreamingCommands.StartEEGAcquisition().serialize();
        LogUtils.i(TAG, "Requested to start stream... "+ Arrays.toString(msg));
        if (!isConnected()) {
            LogUtils.i(TAG,"Error Not connected!");
            return false;
        }
        else {
            if (sendData(msg)) {
                LogUtils.i(TAG,"Successfully requested to start stream");
                sendKeepAlive(true);
                notifyStreamStateChanged(StreamState.STARTED);
                isStreaming = true;
                return true;

            } else {
                LogUtils.i(TAG,"Error could not send request to start stream!");
                return false;
            }
        }
    }

    private synchronized boolean sendData(@NonNull final byte[] msg) {
        LogUtils.i(TAG, "Send data "+ Arrays.toString(msg));
        try {
            if(this.writer != null){
                this.writer.write(msg);
                this.writer.flush();
                //LogUtils.i(TAG, "Message sent");
                return true;
            }
            return false;
        } catch (@NonNull final IOException ioe) {
            this.reader = null;
            this.writer = null;
            LogUtils.e(TAG, "Failed to send dataBuffer. IOException ->\n" + ioe.getMessage());
            notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
            Log.getStackTraceString(ioe);
            return false;
        }
    }

    private void sendKeepAlive(boolean keepAlive) {
        final byte[] msg = new DeviceStreamingCommands.StartEEGAcquisition().serialize();
        if (keepAlive) {
            if (this.keepAliveTimer != null)
                this.keepAliveTimer.cancel();
            this.keepAliveTimer = new Timer(true);
            this.keepAliveTimer.scheduleAtFixedRate(new TimerTask() {
                public final void run() {
                    if (!isConnected()) {
                        LogUtils.e(TAG, "Disconnected");
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

    public static final int START_FRAME_NB_BYTES = 1;
    public static final int PAYLOAD_LENGTH_NB_BYTES = 2;
    public static final int COMPRESS_NB_BYTES = 1;
    public static final int PACKET_ID_NB_BYTES = 2;
    public static final int PAYLOAD_NB_BYTES = 0;
    public static final int VERSION_NB_BYTES = 2;
    public static final int SERIAL_NUMBER_NB_BYTES = 4;

    @NonNull
    byte[] payloadLengthBuffer = new byte[PAYLOAD_LENGTH_NB_BYTES];
    @NonNull
    byte[] dataBuffer = new byte[PAYLOAD_NB_BYTES];
    private int command = -1;
    private int counter = 0;
    private int payloadSize = -1;
    @NonNull
    private byte[] packetIdBuffer = new byte[PACKET_ID_NB_BYTES];
    private void listenForIncomingMessages() {

        currentStatus = STATE_IDLE;
        while (this.reader != null && this.writer != null) {
            try {
                byte currentByte = reader.readByte();

                    switch(currentStatus){

                        case STATE_IDLE:
                            if(currentByte == START_FRAME.getIdentifierCode())
                                currentStatus = STATE_LENGTH;
                            break;

                        case STATE_LENGTH:
                            payloadLengthBuffer[counter++] = currentByte;
                            if(counter == PAYLOAD_LENGTH_NB_BYTES){
                                counter = 0;
                                payloadSize = ((payloadLengthBuffer[0] & 0xFF) << 8) + (payloadLengthBuffer[1] & 0xFF);

                                dataBuffer = new byte[payloadSize + COMPRESS_NB_BYTES + PACKET_ID_NB_BYTES];
                                currentStatus = STATE_COMMAND;
                            }
                            break;

                        case STATE_COMMAND:
                            command = currentByte ;

                            currentStatus =
                                    ((command != CMD_START_EEG_ACQUISITION.getIdentifierCode()
                                            && command != CMD_GET_BATTERY_VALUE.getIdentifierCode()
                                            && command != MBX_GET_EEG_CONFIG.getIdentifierCode()
                                            && command !=  CMD_GET_DEVICE_INFO.getIdentifierCode()) ?
                                            STATE_IDLE : STATE_COMPRESSION) ;
                            break;

                        case STATE_COMPRESSION:
                            if(currentByte == 0x00 || currentByte == 0x01){
                                dataBuffer[counter++] = currentByte;
                                currentStatus = STATE_FRAME_NB;
                            } else
                                currentStatus = STATE_IDLE;
                            break;

                        case STATE_FRAME_NB:
                            dataBuffer[counter++] = currentByte;
                            if(counter == COMPRESS_NB_BYTES + PACKET_ID_NB_BYTES){
                                packetIdBuffer[0] = dataBuffer[1];
                                packetIdBuffer[1] = dataBuffer[2];
                                currentStatus = STATE_ACQ;
                            }
                            break;

                        case STATE_ACQ:

                            if(command == CMD_START_EEG_ACQUISITION.getIdentifierCode() && isStreaming) {

                                dataBuffer[counter++] = currentByte;

                                if (counter == payloadSize + COMPRESS_NB_BYTES + PACKET_ID_NB_BYTES) {
                                    counter = 0;
                                    currentStatus = STATE_IDLE;

                                    final byte[] finalData =  dataBuffer.clone();//Arrays.copyOf(dataBuffer, dataBuffer.length);
                                    AsyncUtils.executeAsync(new Runnable() {
                                        @Override
                                        public void run() {
                                            notifyNewDataAcquired(finalData);
                                        }
                                    });
                                }

                            }else if(command == MBX_GET_EEG_CONFIG.getIdentifierCode()
                            || command == CMD_GET_DEVICE_INFO.getIdentifierCode()) {

                                dataBuffer[counter++] = currentByte;

                                if (counter == payloadSize + COMPRESS_NB_BYTES + PACKET_ID_NB_BYTES) {
                                    counter = 0;
                                    currentStatus = STATE_IDLE;

                                    final byte[] finalData = dataBuffer.clone();//Arrays.copyOf(dataBuffer, dataBuffer.length);
                                    AsyncUtils.executeAsync(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(!isStreaming)
                                                notifyCommandResponseReceived(finalData);
                                        }
                                    });
                                }

                            }else if(command == DeviceCommandEvent.CMD_GET_BATTERY_VALUE.getIdentifierCode()) {
                                LogUtils.i(TAG, "Reading Battery level");
                                dataBuffer = new byte[payloadSize];
                                counter = 0;
                                currentStatus = STATE_IDLE;
                                int percent = -1;
                                switch((int) currentByte) {
                                    case 0:
                                        percent = 0;
                                        break;
                                    case 1:
                                        percent = 15;
                                        break;
                                    case 2:
                                        percent = 30;
                                        break;
                                    case 3:
                                        percent = 50;
                                        break;
                                    case 4:
                                        percent = 65;
                                        break;
                                    case 5:
                                        percent = 85;
                                        break;
                                    case 6:
                                        percent = 100;
                                        break;
                                    default:
                                        break;
                                }
                                notifyCommandResponseReceived(percent);

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
                    notifyConnectionStateChanged(BtState.CONNECTION_INTERRUPTED);
                } else
                    notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
                break;
            }
        }
    }


    private byte[] fillBuffer(byte currentByte){
        dataBuffer[counter++] = currentByte;

        if (counter == payloadSize + COMPRESS_NB_BYTES + PACKET_ID_NB_BYTES) {
            counter = 0;
            currentStatus = STATE_IDLE;
            return dataBuffer.clone();//Arrays.copyOf(dataBuffer, dataBuffer.length);
        }
        return null;
    }

    @Override
    public boolean stopStream() {
        final byte[] msg = new DeviceStreamingCommands.StopEEGAcquisition().serialize();
        LogUtils.i(TAG, "Requested to stop stream... "+Arrays.toString(msg));
        if (!isConnected()) {
            LogUtils.i(TAG,"Error Not connected!");
            return false;

        } else {
            if (sendData(msg)) {

                LogUtils.i(TAG,"Successfully requested to stop stream");
                sendKeepAlive(false);
                isStreaming = false;
                notifyStreamStateChanged(StreamState.STOPPED);
                return true;
            }
            else {
                LogUtils.i(TAG,"Error could not send request to stop stream!");
                return false;
            }
        }
    }

    /**
     * Ask to get the battery level
     */
    @Override
    public boolean readBattery() {
        sendCommand(new DeviceCommands.GetBattery());
        return true;
    }


    @Override
    public void sendCommand(CommandInterface.MbtCommand command) {
        Object response = null;

        if (!isConnectedDeviceReadyForCommand()){ //error returned if no headset is connected
            LogUtils.w(TAG, "Command not sent : "+command);
            command.onError(BluetoothError.ERROR_NOT_CONNECTED, null);
        } else { //any command is not sent if no device is connected
            if (command.isValid()){//any invalid command is not sent : validity criteria are defined in each Bluetooth implemented class , the onError callback is triggered in the constructor of the command object
                LogUtils.i(TAG, "Send command : "+command);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                boolean requestSent = sendData((byte[])command.serialize());

                if(!requestSent) {
                    LogUtils.e(TAG, "Command sending failed");
                    command.onError(BluetoothError.ERROR_REQUEST_OPERATION, null);

                }else {
                    command.onRequestSent();

                    if (command.isResponseExpected()) {
                        response = waitResponseForCommand(11000);
                        command.onResponseReceived(response);
                    }
                }
            }else
                LogUtils.w(TAG, "Command not sent : "+command);
        }

        mbtBluetoothManager.notifyResponseReceived(response, command);//return null response to the client if request has not been sent
    }


    /**
     * Assemble all the input codes in a single array
     * @param code code to assemble
     * @return the assembled array
     */
    public static byte[] assembleCodes(DeviceCommandEvent code){
        return DeviceCommandEvent.assembleCodes(
                DeviceCommandEvent.START_FRAME.getAssembledCodes(),
                DeviceCommandEvent.PAYLOAD_LENGTH.getAssembledCodes(),
                new byte[]{code.getIdentifierCode()},
                DeviceCommandEvent.COMPRESS.getAssembledCodes(),
                DeviceCommandEvent.PACKET_ID.getAssembledCodes(),
                DeviceCommandEvent.PAYLOAD.getAssembledCodes()
                );
    }
}

