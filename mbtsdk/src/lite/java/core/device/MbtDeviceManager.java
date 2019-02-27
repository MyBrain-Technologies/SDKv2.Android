package core.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;


import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import config.MbtConfig;
import core.BaseModuleManager;
import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.device.model.MbtDevice;
import core.device.model.MelomindDevice;
import core.device.model.VProDevice;
import core.eeg.acquisition.MbtDataConversion;
import eventbus.EventBusManager;
import eventbus.events.DeviceInfoEvent;
import utils.LogUtils;


public class MbtDeviceManager extends BaseModuleManager{

    private static final String TAG = MbtDeviceManager.class.getSimpleName();
    private BtProtocol protocol;

    private MbtDevice mCurrentDevice;

    public MbtDeviceManager(Context context, MbtManager mbtManagerController, BtProtocol protocol){
        super(context, mbtManagerController);
        this.mContext = context;
        this.protocol = protocol;
    }


    @Subscribe
    public void onNewDeviceMeasure(@NonNull final DeviceEvents.RawDeviceMeasure rawDeviceMeasure){
        //TODO complete cases with differents measures.
        if(rawDeviceMeasure.getRawMeasure().length < 2)
            return;

        if(rawDeviceMeasure.getRawMeasure()[0] == 0x01)
            EventBusManager.postEvent(new SaturationEvent(rawDeviceMeasure.getRawMeasure()[1]));
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
            EventBusManager.postEvent(new DCOffsets(timestamp, dcOffsets));
        }
    }


    public MbtDevice getmCurrentDevice() {
        return mCurrentDevice;
    }

    public void setmCurrentDevice(MbtDevice mCurrentDevice) {
        this.mCurrentDevice = mCurrentDevice;
    }

    @Subscribe
    public void onNewDeviceConnected(DeviceEvents.NewBluetoothDeviceEvent device){
        LogUtils.d(TAG, "new device connected");
        if(MbtConfig.getDeviceType() == MbtDeviceType.MELOMIND)
            setmCurrentDevice(device.getDevice() != null ? new MelomindDevice(device.getDevice()) : null);
        else if(MbtConfig.getDeviceType() == MbtDeviceType.VPRO)
            setmCurrentDevice(device.getDevice() != null ? new VProDevice(device.getDevice()) : null);
    }

    /**
     * Called when a new device info event has been broadcast on the event bus.
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onDeviceInfoEvent(DeviceInfoEvent event){
        switch(event.getInfotype()){
            case BATTERY:
                LogUtils.d(TAG, "received" + (String) event.getInfo() + " for battery level");
                break;
            case FW_VERSION:
                if(event.getInfo() != null)
                    mCurrentDevice.setFirmwareVersion((String) event.getInfo());
                break;
            case HW_VERSION:
                if(event.getInfo() != null)
                    mCurrentDevice.setHardwareVersion((String) event.getInfo());
                break;
            case SERIAL_NUMBER:
                if(event.getInfo() != null)
                    mCurrentDevice.setSerialNumber((String) event.getInfo());
                break;
        }
    }


    @Subscribe
    public void onGetDevice(DeviceEvents.GetDeviceEvent event){
        EventBusManager.postEvent(new DeviceEvents.PostDeviceEvent(mCurrentDevice));
    }

}
