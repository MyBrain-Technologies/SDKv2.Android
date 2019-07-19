package core.device;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;


import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import core.BaseModuleManager;
import core.device.model.MbtDevice;
import core.device.model.MelomindDevice;
import core.device.model.VProDevice;
import core.device.event.DCOffsetEvent;
import core.device.event.SaturationEvent;
import core.device.event.EventListener;
import core.device.event.OADEvent;
import core.device.oad.OADManager;
import core.eeg.acquisition.MbtDataConversion;
import eventbus.MbtEventBus;
import eventbus.events.ConfigEEGEvent;
import eventbus.events.DeviceInfoEvent;
import utils.LogUtils;

import static features.MbtDeviceType.MELOMIND;


public class MbtDeviceManager extends BaseModuleManager{

    private static final String TAG = MbtDeviceManager.class.getSimpleName();

    private OADManager oadManager;

    private MbtDevice mCurrentConnectedDevice;
    private EventListener.OADEventListener oadEventListener;

    public MbtDeviceManager(Context context){
        super(context);
        this.mContext = context;
        this.oadManager = new OADManager(context, this);
    }


    @Subscribe
    public void onNewDeviceMeasure(@NonNull final DeviceEvents.RawDeviceMeasure rawDeviceMeasure){
        //TODO complete cases with differents measures.
        if(rawDeviceMeasure.getRawMeasure().length < 2)
            return;

        if(rawDeviceMeasure.getRawMeasure()[0] == 0x01)
            MbtEventBus.postEvent(new SaturationEvent(rawDeviceMeasure.getRawMeasure()[1]));
        else if (rawDeviceMeasure.getRawMeasure()[0] == 0x02){
            if(rawDeviceMeasure.getRawMeasure().length < 8)
                return;
            long timestamp = ((rawDeviceMeasure.getRawMeasure()[1] & 0xFF) << (16)) | ((rawDeviceMeasure.getRawMeasure()[2] & 0xFF) << (8) | ((rawDeviceMeasure.getRawMeasure()[3] & 0xFF))); //parsing first 3 bytes as they represents the device intenal clock

            float[] dcOffsets = new float[2]; // parsing last 4 bytes as they represent the dcOffsets
            byte[] temp = new byte[2];
            System.arraycopy(rawDeviceMeasure.getRawMeasure(), 4, temp, 0, 2);
            dcOffsets[1] = MbtDataConversion.convertRawDataToDcOffset(temp);
            System.arraycopy(rawDeviceMeasure.getRawMeasure(), 6, temp, 0, 2);
            dcOffsets[0] = MbtDataConversion.convertRawDataToDcOffset(temp);
            MbtEventBus.postEvent(new DCOffsetEvent(timestamp, dcOffsets));
        }
    }


    public MbtDevice getmCurrentConnectedDevice() {
        return mCurrentConnectedDevice;
    }

    private void setAudioConnectedDeviceAddress(String audioDeviceAddress) {
        Log.d(TAG,"new connected audio device address stored "+audioDeviceAddress);
        if(mCurrentConnectedDevice != null)
            this.mCurrentConnectedDevice.setAudioDeviceAddress(audioDeviceAddress);
    }

    private void setmCurrentConnectedDevice(MbtDevice mCurrentConnectedDevice) {
        Log.d(TAG,"new connected device stored "+mCurrentConnectedDevice);
        this.mCurrentConnectedDevice = mCurrentConnectedDevice;
    }

    @Subscribe
    public void onNewDeviceDisconnected(DeviceEvents.DisconnectedDeviceEvent deviceEvent) {
        setmCurrentConnectedDevice(null);
    }

    @Subscribe
    public void onNewDeviceAudioDisconnected(DeviceEvents.AudioDisconnectedDeviceEvent deviceEvent) {
        if(getmCurrentConnectedDevice() != null)
            getmCurrentConnectedDevice().setAudioDeviceAddress(null);
    }

    @Subscribe
    public void onNewDeviceConnected(DeviceEvents.FoundDeviceEvent deviceEvent) {

        MbtDevice device = null;
        if(deviceEvent.getDevice() != null){
            device = deviceEvent.getDeviceType().equals(MELOMIND) ?
                    new MelomindDevice(deviceEvent.getDevice()) : new VProDevice(deviceEvent.getDevice());
        }
        setmCurrentConnectedDevice(device);
    }

    @Subscribe
    public void onNewAudioDeviceConnected(DeviceEvents.AudioConnectedDeviceEvent deviceEvent) {
            setAudioConnectedDeviceAddress( (deviceEvent.getDevice() != null) ?
                    deviceEvent.getDevice().getAddress() : null);
    }

    @Subscribe
    public void onNewDeviceConfiguration(ConfigEEGEvent configEEGEvent){
        this.mCurrentConnectedDevice.setInternalConfig(new MbtDevice.InternalConfig(configEEGEvent.getConfig()));
    }

    /**
     * Called when a new device info event has been broadcast on the event bus.
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onDeviceInfoEvent(DeviceInfoEvent event){
        if(mCurrentConnectedDevice != null){
            switch(event.getInfotype()){
                case BATTERY:
                    LogUtils.d(TAG, "received " + event.getInfo() + " for battery level");
                    break;
                case FW_VERSION:
                    if(event.getInfo() != null)
                        mCurrentConnectedDevice.setFirmwareVersion((String) event.getInfo());
                    break;
                case HW_VERSION:
                    if(event.getInfo() != null)
                        mCurrentConnectedDevice.setHardwareVersion((String) event.getInfo());
                    break;
                case SERIAL_NUMBER:
                    if(event.getInfo() != null)
                        mCurrentConnectedDevice.setSerialNumber((String) event.getInfo());
                    break;
                case MODEL_NUMBER:
                    if(event.getInfo() != null)
                        mCurrentConnectedDevice.setExternalName((String) event.getInfo());
                    break;
                case PRODUCT_NAME:
                    if(event.getInfo() != null)
                        mCurrentConnectedDevice.setProductName((String) event.getInfo());
                    break;
            }
        }
    }


    @Subscribe
    public void onGetDevice(DeviceEvents.GetDeviceEvent event){
        MbtEventBus.postEvent(new DeviceEvents.PostDeviceEvent(mCurrentConnectedDevice));
    }

    @Subscribe
    public void onOADEvent(OADEvent event){
        if (oadEventListener != null)
            oadEventListener.onOADEvent(event);
    }

    private boolean isFirmwareVersionUpToDate(){
        return oadManager.isFirmwareVersionUpToDate(mCurrentConnectedDevice.getFirmwareVersion());
    }

    public void setOADEventListener(EventListener.OADEventListener eventListener){
        this.oadEventListener = eventListener;
    }
}
