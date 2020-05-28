package eventbus.events;

import android.bluetooth.BluetoothDevice;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import core.bluetooth.BluetoothState;
import core.device.model.MbtDevice;
import core.device.model.MelomindDevice;
import core.device.model.VProDevice;
import features.MbtDeviceType;

import static features.MbtDeviceType.MELOMIND;


public class ConnectionStateEvent {

    private BluetoothState newState;
    private String additionalInfo;

    @Nullable
    private MbtDevice device;

    public ConnectionStateEvent(@NonNull BluetoothState newState){
        this.newState = newState;
    }

    public ConnectionStateEvent(@NonNull BluetoothState newState, @Nullable MbtDevice device){
        this.newState = newState;
        this.device = device;
    }

    public ConnectionStateEvent(@NonNull BluetoothState newState, BluetoothDevice device, MbtDeviceType deviceType){
        this.newState = newState;
        this.device = deviceType.equals(MELOMIND) ?
                new MelomindDevice(device) :
                new VProDevice(device);
    }

    public ConnectionStateEvent(@NonNull BluetoothState newState, String additionalInfo){
        this.newState = newState;
        this.additionalInfo = additionalInfo;
    }

    public BluetoothState getNewState() {
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
