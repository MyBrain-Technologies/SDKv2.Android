package engine;

import android.support.annotation.Keep;

@Keep
public interface SimpleRequestCallback<T> {
    void onRequestComplete(T object);
}
