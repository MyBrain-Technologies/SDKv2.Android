package core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import config.MbtConfig;
import core.bluetooth.IStreamable;
import core.bluetooth.requests.StartOrContinueConnectionRequestEvent;
import core.bluetooth.requests.DisconnectRequestEvent;
import core.bluetooth.MbtBluetoothManager;
import core.bluetooth.requests.ReadRequestEvent;
import core.bluetooth.requests.StreamRequestEvent;
import core.device.DCOffsets;
import core.device.DeviceEvents;
import core.device.MbtDeviceManager;
import core.device.SaturationEvent;
import core.device.model.DeviceInfo;
import core.device.model.MbtDevice;
import core.eeg.MbtEEGManager;
import core.eeg.requests.QualityRequest;
import engine.SimpleRequestCallback;
import engine.clientevents.BaseError;
import engine.clientevents.BluetoothError;
import engine.clientevents.ConnectionStateListener;
import engine.clientevents.ConnectionStateReceiver;
import engine.clientevents.DeviceInfoListener;
import engine.clientevents.DeviceStatusListener;
import engine.clientevents.EegError;
import engine.clientevents.EegListener;
import engine.clientevents.HeadsetDeviceError;
import eventbus.EventBusManager;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.NewConnectionStateEvent;
import eventbus.events.DeviceInfoEvent;
import features.MbtFeatures;
import utils.LogUtils;

import static mbtsdk.com.mybraintech.mbtsdk.BuildConfig.BLUETOOTH_ENABLED;
import static mbtsdk.com.mybraintech.mbtsdk.BuildConfig.DEVICE_ENABLED;
import static mbtsdk.com.mybraintech.mbtsdk.BuildConfig.EEG_ENABLED;

/**
 * MbtManager is responsible for managing communication between all the package managers
 *
 * @author Sophie ZECRI on 29/05/2018
 */
public class MbtManager{
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
    private ConnectionStateListener<BaseError> connectionStateListener;
    private EegListener<BaseError> eegListener;
    private DeviceInfoListener<BaseError> deviceInfoListener;
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

        if(DEVICE_ENABLED)
            registerManager(new MbtDeviceManager(mContext, this, MbtFeatures.getBluetoothProtocol()));
        if(BLUETOOTH_ENABLED)
            registerManager(new MbtBluetoothManager(mContext, this));
        if(EEG_ENABLED)
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
     * @param connectionStateListener a set of callback that will notify the user about connection progress.
     */
    public void connectBluetooth(@NonNull ConnectionStateListener<BaseError> connectionStateListener){
        this.connectionStateListener = connectionStateListener;
        if(MbtConfig.getNameOfDeviceRequested() != null && (!MbtConfig.getNameOfDeviceRequested().startsWith(MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX) && !MbtConfig.getNameOfDeviceRequested().startsWith(MbtFeatures.VPRO_DEVICE_NAME_PREFIX)))
            this.connectionStateListener.onError(HeadsetDeviceError.ERROR_PREFIX_NAME,null);
        else
            EventBusManager.postEvent(new StartOrContinueConnectionRequestEvent(true));
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
    public void startStream(boolean useQualities, @NonNull EegListener<BaseError> eegListener, @Nullable DeviceStatusListener deviceStatusListener){
        this.eegListener = eegListener;
        this.deviceStatusListener = deviceStatusListener;

        EventBusManager.postEvent(new StreamRequestEvent(true, useQualities,deviceStatusListener != null));
    }

    /**
     * Posts an event to stop the currently started stream session
     */
    public void stopStream(){
        EventBusManager.postEvent(new StreamRequestEvent(false, false, false));
    }

    /**
     * Posts an event to compute the signal quality of the EEG signal
     */
    public void computeEEGSignalQuality(ArrayList<ArrayList<Float>> consolidatedEEG){
        if(consolidatedEEG.get(0).size() > MbtFeatures.DEFAULT_NUMBER_OF_DATA_TO_DISPLAY)
            EventBusManager.postEvent(new QualityRequest(consolidatedEEG,null));
        else
            throw new IllegalArgumentException("You must acquire at least 1 second of EEG data to compute its signal quality");
    }

    /**
     * Called when a new device info event has been broadcast on the event bus.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceInfoEvent(DeviceInfoEvent event){
        if(event.getInfotype().equals(DeviceInfo.BATTERY)){
            LogUtils.i(TAG," manager received battery level "+event.getInfo());
            if(deviceInfoListener != null){
                if(event.getInfo() == null )
                    deviceInfoListener.onError(HeadsetDeviceError.ERROR_TIMEOUT_BATTERY,null);
                else {
                    if(event.getInfo().equals(-1))
                        deviceInfoListener.onError(HeadsetDeviceError.ERROR_DECODE_BATTERY, null);
                    else
                        deviceInfoListener.onBatteryChanged((String) event.getInfo());
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectionStateChanged(NewConnectionStateEvent connectionStateEvent) {
        if (connectionStateListener == null)
            return;
        //LogUtils.i(TAG, "New state received : " + connectionStateEvent.getNewState());

        switch (connectionStateEvent.getNewState()) {
            case CONNECTED_AND_READY:
                connectionStateListener.onDeviceConnected();
                break;
            case DISCONNECTED:
                connectionStateListener.onDeviceDisconnected();
                break;
            default:
                if (connectionStateEvent.getNewState().isAFailureState())
                    connectionStateListener.onError(connectionStateEvent.getNewState().getAssociatedError(), connectionStateEvent.getAdditionnalInfo());
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
                eegListener.onError(EegError.ERROR_FAIL_START_STREAMING, null);
            }else if(newState == IStreamable.StreamState.DISCONNECTED){
                eegListener.onError(BluetoothError.ERROR_NOT_CONNECTED,null);
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

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEegProcessingEvent(QualityRequest qualityRequest){
//        if(eegListener != null)
//            eegListener.onNewQualities(qualityRequest.getQualities());
//    }


    /**
     * Sets an extended {@link BroadcastReceiver} to the connectionStateListener value
     * @param connectionStateListener the new {@link ConnectionStateReceiver}. Set it to null if you want to reset the listener
     */
    public void setConnectionStateListener(ConnectionStateListener<BaseError> connectionStateListener) {
        this.connectionStateListener = connectionStateListener;
    }


    /**
     * Sets the {@link EegListener} to the connectionStateListener value
     * @param EEGListener the new {@link EegListener}. Set it to null if you want to reset the listener
     */
    public void setEEGListener(EegListener<BaseError> EEGListener) {
        this.eegListener = EEGListener;
    }


    public void requestCurrentConnectedDevice(final SimpleRequestCallback<MbtDevice> callback) {
        EventBusManager.postEventWithCallback(new DeviceEvents.GetDeviceEvent(), new EventBusManager.CallbackVoid<DeviceEvents.PostDeviceEvent>(){
            @Override
            @Subscribe
            public void onEventCallback(DeviceEvents.PostDeviceEvent object) {
                callback.onRequestComplete(object.getDevice());
            }
        });
    }

    Set<BaseModuleManager> getRegisteredModuleManagers() {
        return registeredModuleManagers;
    }
}