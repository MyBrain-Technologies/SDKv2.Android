package core;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashSet;
import java.util.Set;

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
import core.recordingsession.metadata.DeviceInfo;
import engine.DeviceInfoListener;
import engine.HeadsetStatusListener;
import engine.StateListener;
import eventbus.EventBusManager;
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

    private Set<BaseModuleManager> registeredModuleManagers;

    private Context mContext;

    private StateListener stateListener;
    private EegListener eegListener;
    private DeviceInfoListener deviceInfoListener;
    private HeadsetStatusListener headsetStatusListener;

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
    }

    /**
     *
     * @param manager
     */
    private void registerManager(BaseModuleManager manager){
        registeredModuleManagers.add(manager);
    }


    /**
     *
     * @param name
     * @param listener
     */
    public void connectBluetooth(@Nullable String name, @NonNull StateListener listener){
        this.stateListener = listener;

        EventBusManager.postEvent(new ConnectRequestEvent(name));
    }

    /**
     *
     */
    public void disconnectBluetooth(){
        EventBusManager.postEvent(new DisconnectRequestEvent());
    }

    /**
     *
     * @param deviceInfo
     * @param listener
     */
    public void readBluetooth(@NonNull DeviceInfo deviceInfo, @NonNull DeviceInfoListener listener){
        this.deviceInfoListener = listener;

        EventBusManager.postEvent(new ReadRequestEvent(deviceInfo));
    }

    /**
     *
     * @param useQualities
     * @param eegListener
     * @param headsetStatusListener
     */
    public void startStream(boolean useQualities, @NonNull EegListener eegListener, @Nullable HeadsetStatusListener headsetStatusListener){
        this.eegListener = eegListener;
        this.headsetStatusListener = headsetStatusListener;

        EventBusManager.postEvent(new StreamRequestEvent(true));
    }

    /**
     *
     */
    public void stopStream(){
        EventBusManager.postEvent(new StreamRequestEvent(false));
    }

    /**
     *
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
                if(event.getInfo() == null && stateListener != null)
                    stateListener.onError("Unable to change ");
                else
                    stateListener.onStateChanged((BtState) event.getInfo());
                break;
        }
    }

    /**
     *
     * @param newState
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStreamStateChanged(IStreamable.StreamState newState){
        if(newState == IStreamable.StreamState.FAILED && eegListener != null){
            eegListener.onError("Unable to start streaming");
        }
    }

    /**
     *
     * @param saturationEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewSaturationState(SaturationEvent saturationEvent){
        if(headsetStatusListener != null){
            headsetStatusListener.onSaturationStateChanged(saturationEvent);
        }
    }

    /**
     *
     * @param dcOffsets
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewDCOffset(DCOffsets dcOffsets){
        if(headsetStatusListener != null){
            headsetStatusListener.onNewDCOffsetMeasured(dcOffsets);
        }
    }

}