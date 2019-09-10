package eventbus.events;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import core.bluetooth.BtState;
import features.MbtDeviceType;

public class ConnectionStateEvent {
    private BtState newState;
    private String additionalInfo;

    @Nullable
    private BluetoothDevice device;
    @Nullable
    private MbtDeviceType deviceType;


    public ConnectionStateEvent(@NonNull BtState newState){
        this.newState = newState;
    }
    public ConnectionStateEvent(@NonNull BtState newState, String additionalInfo){
        this.newState = newState;
        this.additionalInfo = additionalInfo;
    }

    public ConnectionStateEvent(BtState newState, @Nullable BluetoothDevice device, @Nullable MbtDeviceType deviceType) {
        this.newState = newState;
        this.additionalInfo = additionalInfo;
        this.device = device;
        this.deviceType = deviceType;
    }

    public BtState getNewState() {
        return newState;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    @Nullable
    public BluetoothDevice getDevice() {
        return device;
    }

    @Nullable
    public MbtDeviceType getDeviceType() {
        return deviceType;
    }
}
