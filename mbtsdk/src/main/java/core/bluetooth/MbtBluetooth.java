package core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import config.MbtConfig;
import core.eeg.acquisition.MbtDataAcquisition;
import core.oad.OADEvent;
import engine.MbtClientEvents;
import core.recordingsession.metadata.DeviceInfo;
import utils.MbtLock;

/**
 * Created by Etienne on 08/02/2018.
 */

public abstract class MbtBluetooth implements IScannable, IConnectable{

    private final static String TAG = "MBT Bluetooth";

    // Events Listeners Callbacks
    private MbtClientEvents.StateListener stateListener;
    private MbtClientEvents.BatteryListener batteryListener;
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

    private String deviceName;
    private MbtDataAcquisition dataAcquisition;

    public MbtBluetooth(Context context) {
        this.context = context.getApplicationContext();
        this.uiAccess = new Handler(this.context.getMainLooper());
        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            this.bluetoothAdapter = manager.getAdapter();
        }
        if(this.bluetoothAdapter == null){
            this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //try another way to get the adapter
        }
        this.dataAcquisition = new MbtDataAcquisition(MbtConfig.SAMPLE_RATE, MbtConfig.CURRENT_NB_CHANNELS, MbtConfig.SAMPLE_PER_NOTIF, BtProtocol.BLUETOOTH_LE, 0);

        if (this.dataAcquisition == null) // meaning this instance of MBT Bluetooth has no need for a dataAcquisition
            return;


        this.dataAcquisition.setDataAcquisitionListener(new MbtDataAcquisition.DataAcquisitionListener() {
            @WorkerThread
            public final void onDataReady(@NonNull final ArrayList<ArrayList<Float>> matrix, final ArrayList<Float> status, final int sampleRate, final int nbChannels) {
                MbtBluetooth.this.uiAccess.post(new Runnable() {
                    public final void run() {
                        notifyNewEeg(matrix, status, nbChannels, matrix.get(0).size(), sampleRate);
                    }
                });
            }
        });
    }

    @Override
    public void startScanDiscovery(Context context) {
        if(scannedDevices.equals(MbtConfig.ScannableDevices.VPRO)){

        }
    }

    @Override
    public void stopScanDiscovery() {

    }

    void notifyDeviceInfoReceived(DeviceInfo infoType, String value){
        switch(infoType){
            case FW_VERSION:
                this.deviceInfoListener.onFwVersionReceived(value);
                break;

            case HW_VERSION:
                this.deviceInfoListener.onHwVersionReceived(value);
                break;

            case SERIAL_NUMBER:
                this.deviceInfoListener.onSerialNumberReceived(value);
                break;

            default:
                break;
        }
    }

    void notifyOADEvent(OADEvent eventType, int value){
        if(this.oadEventListener != null){
            this.oadEventListener.onOadEvent(eventType, value);
        }
    }

    @Override
    public void notifyStateChanged(@NonNull BtState newState) {
        this.currentState = newState;
//        if (this.stateListener != null)
//            this.stateListener.onStateChanged(newState);
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

    void notifyBatteryLevelChanged(@NonNull final int level) {
        if (this.batteryListener != null)
            this.batteryListener.onBatteryChanged(level);
    }

    void notifyNewEeg(final ArrayList<ArrayList<Float>> matrix, final ArrayList<Float> status, final int nbChannels, final int nbSamples, final int sampleRate) {
        if (this.eegListener != null)
            this.eegListener.onNewSamples(matrix, status, nbChannels, nbSamples, sampleRate);
    }

    void acquireData(@NonNull final byte[] data, @NonNull BtProtocol protocol) {
        this.dataAcquisition.handleData(data, protocol);
    }

    public BtState getCurrentState() { return currentState; }
    public void setCurrentState(BtState state) { this.currentState=state;}

    public void setOadEventListener(MbtClientEvents.OADEventListener oadEventListener) {
        this.oadEventListener = oadEventListener;
    }

    BluetoothAdapter getBluetoothAdapter() {return bluetoothAdapter;}
    void setOnStateChangeListener(@Nullable final MbtClientEvents.StateListener listener) {
        this.stateListener = listener;
    }

    void setOnBatteryChangeListener(@Nullable final MbtClientEvents.BatteryListener listener) {
        this.batteryListener = listener;
    }

    // Events Registration

    void setMailboxEventListener(@Nullable final MbtClientEvents.MailboxEventListener listener){
        this.mailboxEventListener = listener;
    }

    void setOnNewEegListener(@Nullable final MbtClientEvents.EegListener listener) {
        this.eegListener = listener;
        if (eegListener == null)
            Log.i(TAG,"eegListener is NULL");
    }

    void setDeviceInfoListener(MbtClientEvents.DeviceInfoListener deviceInfoListener) {
        this.deviceInfoListener = deviceInfoListener;
    }

    void setHeadsetStatusListener(MbtClientEvents.HeadsetStatusListener headsetStatusListener) {
        this.headsetStatusListener = headsetStatusListener;
    }

}
