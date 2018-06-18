package core.device;

import android.content.Context;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;

import core.BaseModuleManager;
import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.eeg.acquisition.MbtDataConversion;
import eventbus.EventBusManager;

public class MbtDeviceManager extends BaseModuleManager{

    private BtProtocol protocol;


    public MbtDeviceManager(Context context, MbtManager mbtManagerController, BtProtocol protocol){
        super(context, mbtManagerController);
        this.mContext = context;
        //EventBusManager.registerOrUnregister(true, this);
        this.protocol = protocol;
    }


    @Subscribe
    public void onNewDeviceMeasure(@NonNull final RawDeviceMeasure rawDeviceMeasure){
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
            dcOffsets[1] = MbtDataConversion.convertDCOffsetToEEG(temp);
            System.arraycopy(rawDeviceMeasure.getRawMeasure(), 6, temp, 0, 2);
            dcOffsets[0] = MbtDataConversion.convertDCOffsetToEEG(temp);
            EventBusManager.postEvent(new DCOffsets(timestamp, dcOffsets));
        }
    }


}
