package eventbus;

import android.support.annotation.NonNull;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import utils.LogUtils;


/**
 * MbtEventBus.java
 * eventbus
 *
 * Created by Sophie ZECRI on 03/05/2018
 * Copyright (c) 2016 myBrain Technologies. All rights reserved.
 * MbtEventBus is responsible for managing communication between the different packages by using a publisher/subscriber system
 */
public final class MbtEventBus {

    /**
     * Gets a Event Bus instance that will manage the events
     * Each instance is a separate scope in which events are delivered.
     */
    public static EventBus BUS = EventBus.getDefault();
    private static final String TAG = MbtEventBus.class.getSimpleName();

    /**
     * MbtEventBus contains all the methods for posting events, registering and unregistering subscribers
     * It helps communication between the different packages
     * Once an instance of MbtEventBus is created, the publisher classes can post event to the Event Bus
     * If the current class is a subscriber class, it must register to the Bus for receiving Events.
    private MbtEventBus(){ } //empty constructor need to be called by publisher
     */
    private MbtEventBus(Object subscriber) {} // non empty constructor for subscriber

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
        if(event == null)
            return;
        LogUtils.d(TAG,"post event "+event.toString());
        BUS.post(event);
    }

    /**
     * Posts event and registers a callback that returns a value when the event is triggered
     * Warning : Its is mandatory to unregister the callback after it returns the value
     * @param event is the event to post
     * @param callback is the class that provide a callback that notify you when the value is returned
     */
    public static void postEvent(Object event, Object callback){
        Log.d(TAG, "Eventbus posts event with callback "+event);
        if(callback == null || event == null)
            return;

        BUS.register(callback);
        BUS.post(event);
    }


    public interface Callback<T> {
        Object onEventCallback(T object);
    }
}
