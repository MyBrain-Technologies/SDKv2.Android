package core.bluetooth.requests;

/**
 * Base class for events that holds a response received by a SDK unit, that need to be transferred to an other unit.
 * These events are sent using {@link org.greenrobot.eventbus.EventBus} framework.
 * The response consists of a message/notification associated to a nullable data value.
 * The data are bundled in a map where every data values are associated to a unique key.
 * K is the type of the data identifier (key)
 * V is the type of the data (value)
 */
public abstract class ResponseEvent<K,V> {

    private K eventDataKey;
    private V eventDataValue;

    public ResponseEvent(K eventDataKey, V eventDataValue) {
        this.eventDataKey = eventDataKey;
        this.eventDataValue = eventDataValue;
    }

    public K getId() {
        return eventDataKey;
    }

    public V getDataValue() {
        return eventDataValue;
    }

    public void setEventData(K eventDataKey, V eventDataValue) {
        this.eventDataKey = eventDataKey;
        this.eventDataValue = eventDataValue;
    }

    @Override
    public String toString() {
        return "ResponseEvent{" +
                "eventDataKey=" + eventDataKey +
                "eventDataValue=" + eventDataValue +
                '}';
    }
}
