package core;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashSet;
import java.util.Set;

import core.bluetooth.BtProtocol;
import core.bluetooth.BtState;
import core.bluetooth.IStreamable;
import core.bluetooth.requests.ConnectRequestEvent;
import core.bluetooth.requests.DisconnectRequestEvent;
import core.bluetooth.MbtBluetoothManager;
import core.bluetooth.requests.ReadRequestEvent;
import core.bluetooth.requests.StreamRequestEvent;
import core.device.DCOffsets;
import core.device.MbtDeviceManager;
import core.device.SaturationEvent;
import core.eeg.MbtEEGManager;
import core.recordingsession.metadata.DeviceInfo;
import engine.DeviceInfoListener;
import engine.DeviceStatusListener;
import engine.ConnectionStateListener;
import eventbus.EventBusManager;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.DeviceInfoEvent;
import engine.EegListener;
import features.MbtFeatures;

/**
 * MbtManager is responsible for managing communication between all the package managers
 *
 * @author Sophie ZECRI on 29/05/2018
 */
public final class MbtManager{
    private static final String TAG = MbtManager.class.getName();

    /**
     * Contains the currently reigstered module managers.
     */
    private Set<BaseModuleManager> registeredModuleManagers;

    /**
     * The application context
     */
    private Context mContext;

    private BtProtocol btProtocol;


    /**
     * the application callbacks. EventBus is not available outside the SDK so the user is notified
     * using custom callback interfaces.
     */
    private ConnectionStateListener connectionStateListener;
    private EegListener eegListener;
    private DeviceInfoListener deviceInfoListener;
    private DeviceStatusListener deviceStatusListener;

    /**
     *
     * @param context
     */
    public MbtManager(Context context) {
        this.mContext = context;
        this.registeredModuleManagers = new HashSet<>();

        EventBusManager.registerOrUnregister(true, this);

        registerManager(new MbtDeviceManager(mContext, this, MbtFeatures.getBluetoothProtocol()));
        registerManager(new MbtBluetoothManager(mContext, this, MbtFeatures.getBluetoothProtocol()));
        registerManager(new MbtEEGManager(mContext, this, MbtFeatures.getBluetoothProtocol()));
    }

    /**
     * Add a new module manager instance to the Hashset
     * @param manager the new module manager to add
     */
    private void registerManager(BaseModuleManager manager){
        registeredModuleManagers.add(manager);
    }


    /**
     * Perform a new Bluetooth connection.
     * @param name the device name to connect to. Might be null if not known by the user.
     * @param listener a set of callback that will notify the user about connection progress.
     */
    public void connectBluetooth(@Nullable String name, @NonNull ConnectionStateListener listener){
        this.connectionStateListener = listener;
        EventBusManager.postEvent(new ConnectRequestEvent(name));
    }

    /**
     * Perform a Bluetooth disconnection.
     */
    public void disconnectBluetooth(){
        EventBusManager.postEvent(new DisconnectRequestEvent());
    }

    /**
     * Perform a bluetooth read operation.
     * @param deviceInfo the type of info to read
     * @param listener a set of callback to notify user about the results.
     */
    public void readBluetooth(@NonNull DeviceInfo deviceInfo, @NonNull DeviceInfoListener listener){
        this.deviceInfoListener = listener;

        EventBusManager.postEvent(new ReadRequestEvent(deviceInfo));
    }

    /**
     * Posts an event to initiate a stream session.
     * @param useQualities whether or not quality check algorithms have to be called (Currently false)
     * @param eegListener the eeg listener
     * @param deviceStatusListener to notify the user about device status real time modifications.
     */
    public void startStream(boolean useQualities, @NonNull EegListener eegListener, @Nullable DeviceStatusListener deviceStatusListener){
        this.eegListener = eegListener;
        this.deviceStatusListener = deviceStatusListener;

        EventBusManager.postEvent(new StreamRequestEvent(true));
    }

    /**
     * Posts an event to stop the currently started stream session
     */
    public void stopStream(){
        EventBusManager.postEvent(new StreamRequestEvent(false));
    }

    /**
     * Called when a new device info event has been broadcast on the event bus.
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceInfoEvent(DeviceInfoEvent event){
        switch(event.getInfotype()){
            case BATTERY:
                if(event.getInfo() == null && deviceInfoListener != null)
                    deviceInfoListener.onError("Unable to get battery level");
                else
                    deviceInfoListener.onBatteryChanged((String)event.getInfo());
                break;
            case FW_VERSION:
                if(event.getInfo() == null && deviceInfoListener != null)
                    deviceInfoListener.onError("Unable to get firmware version");
                else
                    deviceInfoListener.onFwVersionReceived((String) event.getInfo());
                break;
            case HW_VERSION:
                if(event.getInfo() == null && deviceInfoListener != null)
                    deviceInfoListener.onError("Unable to get hardware level");
                else
                    deviceInfoListener.onHwVersionReceived((String) event.getInfo());
                break;
            case SERIAL_NUMBER:
                if(event.getInfo() == null && deviceInfoListener != null)
                    deviceInfoListener.onError("Unable to get serial number");
                else
                    deviceInfoListener.onSerialNumberReceived((String) event.getInfo());
                break;

            case STATE:
                if(event.getInfo() == null && connectionStateListener != null)
                    connectionStateListener.onError("Unable to change ");
                else
                    connectionStateListener.onStateChanged((BtState) event.getInfo());
                break;
        }
    }

    /**
     * Called when a new stream state event has been broadcast on the event bus.
     * @param newState
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStreamStateChanged(IStreamable.StreamState newState){
        if(newState == IStreamable.StreamState.FAILED && eegListener != null){
            eegListener.onError("Unable to start streaming");
        }
    }

    /**
     *Called when a new saturation event has been broadcast on the event bus.
     * @param saturationEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewSaturationState(SaturationEvent saturationEvent){
        if(deviceStatusListener != null){
            deviceStatusListener.onSaturationStateChanged(saturationEvent);
        }
    }

    /**
     * Called when a new DCOffset measure event has been broadcast on the event bus.
     * @param dcOffsets
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewDCOffset(DCOffsets dcOffsets){
        if(deviceStatusListener != null){
            deviceStatusListener.onNewDCOffsetMeasured(dcOffsets);
        }
    }

    /**
     * onEvent is called by the Event Bus when a ClientReadyEEGEvent event is posted
     * This event is published by {@link core.eeg.MbtEEGManager}:
     * this manager handles EEG data acquired by the headset
     * Creates a new MbtEEGPacket instance when the raw buffer contains enough data
     * @param event contains data transmitted by the publisher : here it contains the converted EEG data matrix, the status, the number of acquisition channels and the sampling rate
     */
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 1)
    public void onEvent(final ClientReadyEEGEvent event) { //warning : do not remove this attribute (consider unsused by the IDE, but actually used)
        Log.i(TAG, "event ClientReadyEEGEvent received" );
        if(eegListener != null)
            eegListener.onNewPackets(event.getEegPackets());
    }


}