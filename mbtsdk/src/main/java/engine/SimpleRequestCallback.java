package engine;

import android.support.annotation.Keep;

import engine.clientevents.BaseErrorEvent;


@Keep
public interface SimpleRequestCallback<T> extends BaseErrorEvent {
    void onRequestComplete(T object);
}

