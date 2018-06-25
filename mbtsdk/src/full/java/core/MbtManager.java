package core;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashSet;
import java.util.Set;

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
import engine.clientevents.ConnectionException;
import engine.clientevents.DeviceInfoListener;
import engine.clientevents.DeviceStatusListener;
import engine.clientevents.ConnectionStateListener;
import engine.clientevents.EEGException;
import engine.clientevents.ReadException;
import eventbus.EventBusManager;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.ConnectionStateEvent;
import eventbus.events.DeviceInfoEvent;
import engine.clientevents.EegListener;
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

    /**
     * the application callbacks. EventBus is not available outside the SDK so the user is notified
     * using custom callback interfaces.
     */
    private ConnectionStateListener<ConnectionException> connectionStateListener;
    private EegListener<EEGException> eegListener;
    private DeviceInfoListener<ReadException> deviceInfoListener;
    @Nullable
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
        registerManager(new MbtBluetoothManager(mContext, this));
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
    public void connectBluetooth(@Nullable String name, @NonNull ConnectionStateListener<ConnectionException> listener){
        if(name != null && (!name.startsWith(MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX) && !name.startsWith(MbtFeatures.VPRO_DEVICE_NAME_PREFIX))){
            listener.onError(new ConnectionException(ConnectionException.INVALID_NAME));
            return;
        }



        this.connectionStateListener = listener;
        EventBusManager.postEvent(new ConnectRequestEvent(name));
    }

    /**
     * Perform a Bluetooth disconnection.
     */
    public void disconnectBluetooth(boolean isAbortion){
        EventBusManager.postEvent(new DisconnectRequestEvent(isAbortion));
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

        EventBusManager.postEvent(new StreamRequestEvent(true, deviceStatusListener != null));
    }

    /**
     * Posts an event to stop the currently started stream session
     */
    public void stopStream(){
        EventBusManager.postEvent(new StreamRequestEvent(false, false));
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
                    deviceInfoListener.onError(new ReadException(ReadException.READ_TIMEOUT));
                else
                    deviceInfoListener.onBatteryChanged((String)event.getInfo());
                break;
            case FW_VERSION:
                if(event.getInfo() == null && deviceInfoListener != null)
                    deviceInfoListener.onError(new ReadException(ReadException.READ_TIMEOUT));
                else
                    deviceInfoListener.onFwVersionReceived((String) event.getInfo());
                break;
            case HW_VERSION:
                if(event.getInfo() == null && deviceInfoListener != null)
                    deviceInfoListener.onError(new ReadException(ReadException.READ_TIMEOUT));
                else
                    deviceInfoListener.onHwVersionReceived((String) event.getInfo());
                break;
            case SERIAL_NUMBER:
                if(event.getInfo() == null && deviceInfoListener != null)
                    deviceInfoListener.onError(new ReadException(ReadException.READ_TIMEOUT));
                else
                    deviceInfoListener.onSerialNumberReceived((String) event.getInfo());
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectionStateChanged(ConnectionStateEvent connectionStateEvent){
        if(connectionStateListener == null)
            return;

        switch(connectionStateEvent.getNewState()){
            case DISABLED:
                connectionStateListener.onError(new ConnectionException(ConnectionException.BT_NOT_ACTIVATED));
                break;

            case LOCATION_IS_REQUIRED:
                connectionStateListener.onError(new ConnectionException(ConnectionException.GPS_DISABLED));
                break;

            case LOCATION_PERMISSION_NOT_GRANTED:
                connectionStateListener.onError(new ConnectionException(ConnectionException.GPS_PERMISSIONS_NOT_GRANTED));
                break;

            case SCAN_FAILED_ALREADY_STARTED:
            case SCAN_FAILED_FEATURE_UNSUPPORTED:
            case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                connectionStateListener.onError(new ConnectionException(ConnectionException.LE_SCAN_FAILURE));
                break;

            case CONNECT_FAILURE:
                connectionStateListener.onError(new ConnectionException(ConnectionException.CONNECTION_FAILURE));
                break;

            case ANOTHER_DEVICE_CONNECTED:
                connectionStateListener.onError(new ConnectionException(ConnectionException.ANOTHER_DEVICE_CONNECTED));
                break;

            case DISCONNECTED:
                connectionStateListener.onStateChanged(connectionStateEvent.getNewState());
                break;
            default:
                //This newState is not an error, notifying user with the correct callback
                connectionStateListener.onStateChanged(connectionStateEvent.getNewState());
                break;
        }

    }

    /**
     * Called when a new stream state event has been broadcast on the event bus.
     * @param newState
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStreamStateChanged(IStreamable.StreamState newState){
        if(eegListener != null){
            if(newState == IStreamable.StreamState.FAILED){
                eegListener.onError(new EEGException(EEGException.STREAM_START_FAILED));
            }else if(newState == IStreamable.StreamState.DISCONNECTED){
                eegListener.onError(new EEGException(EEGException.DEVICE_NOT_CONNECTED));
            }else if(newState == IStreamable.StreamState.STOPPED){
                eegListener = null;
            }
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
     * This event is published by {@link MbtEEGManager}:
     * this manager handles EEG data acquired by the headset
     * Creates a new MbtEEGPacket instance when the raw buffer contains enough data
     * @param event contains data transmitted by the publisher : here it contains the converted EEG data matrix, the status, the number of acquisition channels and the sampling rate
     */
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 1)
    public void onEvent(@NonNull final ClientReadyEEGEvent event) { //warning : do not remove this attribute (consider unsused by the IDE, but actually used)
        if(eegListener != null)
            eegListener.onNewPackets(event.getEegPackets());
    }


    /**
     * Sets the {@link ConnectionStateListener} to the connectionStateListener value
     * @param connectionStateListener the new {@link ConnectionStateListener}. Set it to null if you want to reset the listener
     */
    public void setConnectionStateListener(ConnectionStateListener<ConnectionException> connectionStateListener) {
        this.connectionStateListener = connectionStateListener;
    }


    /**
     * Sets the {@link EegListener} to the connectionStateListener value
     * @param EEGListener the new {@link EegListener}. Set it to null if you want to reset the listener
     */
    public void setEEGListener(EegListener<EEGException> EEGListener) {
        this.eegListener = EEGListener;
    }
}