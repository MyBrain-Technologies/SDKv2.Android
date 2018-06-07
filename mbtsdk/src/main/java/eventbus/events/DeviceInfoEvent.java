package eventbus.events;

import android.support.annotation.Nullable;

import java.io.Serializable;

import core.recordingsession.metadata.DeviceInfo;

public class DeviceInfoEvent<T> implements Serializable{

    private T info;
    private DeviceInfo infotype;

    public DeviceInfoEvent(DeviceInfo infotype, @Nullable T info) {
        this.info = info;
        this.infotype = infotype;
    }

    /**
     * Gets information stored in this event class
     * @return the information
     */
    public T getInfo() {
        return info;
    }

    public DeviceInfo getInfotype() {
        return infotype;
    }

}
