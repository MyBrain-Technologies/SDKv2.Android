package eventbus;

import android.os.Handler;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import utils.LogUtils;

/**
 * EventBusManager.java
 * eventbus
 *
 * Created by Sophie ZECRI on 03/05/2018
 * Copyright (c) 2016 myBrain Technologies. All rights reserved.
 * EventBusManager is responsible for managing communication between the different packages by using a publisher/subscriber system
 */
public final class EventBusManager {

    /**
     * Gets a Event Bus instance that will manage the events
     * Each instance is a separate scope in which events are delivered.
     */
    public static final EventBus BUS = EventBus.getDefault();
    private static final String TAG = EventBusManager.class.getSimpleName();

    /**
     * EventBusManager contains all the methods for posting events, registering and unregistering subscribers
     * It helps communication between the different packages
     * Once an instance of EventBusManager is created, the publisher classes can post event to the Event Bus
     * If the current class is a subscriber class, it must register to the Bus for receiving Events.
    private EventBusManager(){ } //empty constructor need to be called by publisher
     */
    private EventBusManager(Object subscriber) {} // non empty constructor for subscriber

    /**
     * Registers or unregisters the given subscriber class to receive events from the Events Bus.
     * Subscribers must unregister once they are no longer interested in receiving events.
     * Subscribers can't receive any event if they are not registered.
     * @param isRegistration is true to register the subscriber class and false to unregister the subscriber class.
     * @param subscriber is the subscriber class to register for receiving events from the Event Bus.
     */
    public static void registerOrUnregister(boolean isRegistration, @NonNull Object subscriber ){
        if (isRegistration) {
            BUS.register(subscriber);
        } else {
            EventBus.getDefault().unregister(subscriber);
        }
    }

    /**
     * Publishs the given event to the Event Bus.
     * The Event Bus will deliver it to registered subscribers.
     * @param event contains data to transmit to the subscriber class.
     */
    public static void postEvent(Object event){
        BUS.post(event);
    }



    public static void postEventWithCallback(Object event, Callback callback){
        BUS.register(callback);
        BUS.post(event);
        BUS.unregister(callback);
    }



    public interface Callback<T> {
        public void onEventCallback(T object);
    }

}
