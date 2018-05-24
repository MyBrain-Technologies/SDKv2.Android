package eventbus;

import android.app.usage.UsageEvents;

import org.greenrobot.eventbus.EventBus;

public class EventBusManager {

    public static final EventBus BUS = EventBus.getDefault();

    public EventBusManager(){ } //empty constructor need to be called by publisher

    public EventBusManager(Object subscriber) { // constructor for subscriber
        registerOrUnregister(true, subscriber); //register for communication with the bus
    }

    public void registerOrUnregister(boolean isRegistration, Object subscriber ){
        if (isRegistration) {
            BUS.register(subscriber);
        } else {
            BUS.getDefault().unregister(subscriber);
        }
    }

    public void postEvent(Object event){
        BUS.post(event);
    }
}
