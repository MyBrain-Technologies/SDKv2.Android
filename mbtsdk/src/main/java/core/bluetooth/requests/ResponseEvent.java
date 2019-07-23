package core.bluetooth.requests;

import android.util.Pair;

/**
 * Base class for events that holds a response received by a SDK unit, that need to be transferred to an other unit.
 * These events are sent using {@link org.greenrobot.eventbus.EventBus} framework.
 * The response consists of a message/notification associated to a nullable data value.
 * The data are bundled in a map where every data values are associated to a unique key.
 * K is the type of the data identifier (key)
 * V is the type of the data (value)
 */
public abstract class ResponseEvent<K,V> {

    private Pair<K,V> eventData;

    public ResponseEvent(Pair<K, V> eventData) {
        this.eventData = eventData;
    }

    public Pair<K, V> getEventData() {
        return eventData;
    }

    public K getEventIdentifier() {
        return eventData.first;
    }

    public V getEventDataValue() {
        return eventData.second;
    }

    public void setEventData(Pair<K, V> eventData) {
        this.eventData = eventData;
    }
}
