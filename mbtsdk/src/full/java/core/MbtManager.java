package core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import config.DeviceCommandConfig;
import config.EegStreamConfig;
import config.StreamConfig;
import core.bluetooth.BtProtocol;
import core.bluetooth.IStreamable;
import core.bluetooth.requests.StartOrContinueConnectionRequestEvent;
import core.bluetooth.requests.DisconnectRequestEvent;
import core.bluetooth.MbtBluetoothManager;
import core.bluetooth.requests.ReadRequestEvent;
import core.bluetooth.requests.StreamRequestEvent;
import core.bluetooth.requests.UpdateConfigurationRequestEvent;
import core.device.DCOffsets;
import core.device.DeviceEvents;
import core.device.MbtDeviceManager;
import core.device.SaturationEvent;
import core.device.model.DeviceInfo;
import core.device.model.MbtDevice;
import core.device.model.MelomindsQRDataBase;
import core.eeg.MbtEEGManager;
import engine.SimpleRequestCallback;
import engine.clientevents.BaseError;
import engine.clientevents.BluetoothError;
import engine.clientevents.ConnectionStateListener;
import engine.clientevents.BluetoothStateListener;
import engine.clientevents.DeviceBatteryListener;
import engine.clientevents.DeviceStatusListener;
import engine.clientevents.EegError;
import engine.clientevents.EegListener;
import engine.clientevents.HeadsetDeviceError;
import eventbus.EventBusManager;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.ConnectionStateEvent;
import eventbus.events.DeviceInfoEvent;
import features.MbtDeviceType;
import features.MbtFeatures;
import mbtsdk.com.mybraintech.mbtsdk.R;
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
    private DeviceBatteryListener<BaseError> deviceInfoListener;
    @Nullable
    private DeviceStatusListener deviceStatusListener;

    public MbtManager(Context context) {
        this.mContext = context;
        this.registeredModuleManagers = new HashSet<>();

        EventBusManager.registerOrUnregister(true, this);

        if(DEVICE_ENABLED)
            registerManager(new MbtDeviceManager(mContext, MbtManager.this));
        if(BLUETOOTH_ENABLED)
            registerManager(new MbtBluetoothManager(mContext, MbtManager.this));
        if(EEG_ENABLED)
            registerManager(new MbtEEGManager(mContext, MbtManager.this, BtProtocol.BLUETOOTH_LE)); //todo change protocol must not be initialized here : when connectBluetooth is called
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
    public void connectBluetooth(@NonNull ConnectionStateListener<BaseError> connectionStateListener, String deviceNameRequested, String deviceQrCodeRequested, MbtDeviceType deviceTypeRequested){
        this.connectionStateListener = connectionStateListener;
        if(deviceNameRequested != null && (!deviceNameRequested.startsWith(MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX) && !deviceNameRequested.startsWith(MbtFeatures.VPRO_DEVICE_NAME_PREFIX) )){
            this.connectionStateListener.onError(HeadsetDeviceError.ERROR_PREFIX," "+ (deviceTypeRequested.equals(MbtDeviceType.MELOMIND) ? MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX : MbtFeatures.VPRO_DEVICE_NAME_PREFIX));
        }else if(deviceQrCodeRequested != null && (!deviceQrCodeRequested.startsWith(MbtFeatures.QR_CODE_NAME_PREFIX))){
            this.connectionStateListener.onError(HeadsetDeviceError.ERROR_PREFIX," "+MbtFeatures.QR_CODE_NAME_PREFIX);
        }else if(deviceQrCodeRequested != null && deviceNameRequested != null && !deviceNameRequested.equals(new MelomindsQRDataBase(mContext,  true).get(deviceQrCodeRequested))){
            this.connectionStateListener.onError(HeadsetDeviceError.ERROR_MATCHING, mContext.getString(R.string.aborted_connection));
        }else{
            EventBusManager.postEvent(new StartOrContinueConnectionRequestEvent(true, deviceNameRequested, deviceQrCodeRequested, deviceTypeRequested));
        }
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
    public void readBluetooth(@NonNull DeviceInfo deviceInfo, @NonNull DeviceBatteryListener<BaseError> listener){
        this.deviceInfoListener = listener;
        EventBusManager.postEvent(new ReadRequestEvent(deviceInfo));
    }

    /**
     * Posts an event to initiate a stream session.
     */
    public void startStream(StreamConfig streamConfig){
        this.eegListener = streamConfig.getEegListener();
        EegStreamConfig eegStreamConfig = streamConfig.getEegStreamConfig();
        if(eegStreamConfig != null)
            this.deviceStatusListener = eegStreamConfig.getDeviceStatusListener();

        EventBusManager.postEvent(
                new StreamRequestEvent(true,
                        streamConfig.shouldComputeQualities(),
                        (deviceStatusListener != null),
                        eegStreamConfig));
    }

    /**
     * Posts an event to stop the currently started stream session
     */
    public void stopStream(){
        EventBusManager.postEvent(new StreamRequestEvent(false, false, false));
    }

    public void configureHeadset(EegStreamConfig eegStreamConfig){
        EventBusManager.postEvent(new UpdateConfigurationRequestEvent(eegStreamConfig));
    }

    public void sendDeviceCommand(@NonNull DeviceCommandConfig deviceCommandConfig, SimpleRequestCallback<byte[]> callback){
        EventBusManager.postEventWithCallback(new DeviceEvents.SendDeviceCommandEvent(deviceCommandConfig), new EventBusManager.CallbackVoid<DeviceEvents.RawDeviceResponseEvent>(){
            @Override
            @Subscribe
            public void onEventCallback(DeviceEvents.RawDeviceResponseEvent headsetRawResponse) {
                Log.d(TAG, "Callback returned "+ Arrays.toString(headsetRawResponse.getRawResponse()));
                callback.onRequestComplete(headsetRawResponse.getRawResponse());
            }
        });
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
    public void onConnectionStateChanged(ConnectionStateEvent connectionStateEvent) {
        if (connectionStateListener == null)
            return;
        //LogUtils.i(TAG, "New state received : " + connectionStateEvent.getNewState());
        if(connectionStateListener instanceof BluetoothStateListener)
            ((BluetoothStateListener) connectionStateListener).onNewState(connectionStateEvent.getNewState());

        switch (connectionStateEvent.getNewState()) {
            case CONNECTED_AND_READY:
                connectionStateListener.onDeviceConnected();
                break;
            case DATA_BT_DISCONNECTED:
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
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewSaturationState(SaturationEvent saturationEvent){
        if(deviceStatusListener != null){
            deviceStatusListener.onSaturationStateChanged(saturationEvent);
        }
    }

    /**
     * Called when a new DCOffset measure event has been broadcast on the event bus.
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
     * Sets an extended {@link BroadcastReceiver} to the connectionStateListener value
     * @param connectionStateListener the new {@link BluetoothStateListener}. Set it to null if you want to reset the listener
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