package core;

import android.content.Context;

import eventbus.EventBusManager;

public abstract class BaseModuleManager {

    protected Context mContext;
    protected MbtManager mbtManager;

    protected BaseModuleManager(Context context, MbtManager manager){
        this.mContext = context;
        this.mbtManager = manager;
        EventBusManager.registerOrUnregister(true, this);
    }

}
