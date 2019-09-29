package eventbus.events;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import core.bluetooth.BtState;
import core.device.model.MbtDevice;
import core.device.model.MelomindDevice;
import core.device.model.VProDevice;
import features.MbtDeviceType;

import static features.MbtDeviceType.MELOMIND;


public class ConnectionStateEvent {

    private BtState newState;
    private String additionalInfo;

    @Nullable
    private MbtDevice device;

    public ConnectionStateEvent(@NonNull BtState newState){
        this.newState = newState;
    }

    public ConnectionStateEvent(@NonNull BtState newState, @Nullable MbtDevice device){
        this.newState = newState;
        this.device = device;
    }

    public ConnectionStateEvent(@NonNull BtState newState, BluetoothDevice device, MbtDeviceType deviceType){
        this.newState = newState;
        this.device = deviceType.equals(MELOMIND) ?
                new MelomindDevice(device) :
                new VProDevice(device);
    }

    public ConnectionStateEvent(@NonNull BtState newState, String additionalInfo){
        this.newState = newState;
        this.additionalInfo = additionalInfo;
    }

    public BtState getNewState() {
        return newState;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    @Nullable
    public MbtDevice getDevice() {
        return device;
    }

}
