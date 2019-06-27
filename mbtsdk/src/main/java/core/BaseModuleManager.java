package core;

import android.content.Context;

import eventbus.EventBusManager;

/**
 * Abstract class that represent a simple module manager.
 * All modules in this SDK have the same scheme. One module manager and several satellite classes that communicate to each other through the manager.
 */
public abstract class BaseModuleManager {

    /**
     * The application context
     */
    protected Context mContext;


    protected BaseModuleManager(Context context){
        this.mContext = context;
        EventBusManager.registerOrUnregister(true, this);
    }

}
