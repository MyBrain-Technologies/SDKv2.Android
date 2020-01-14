package engine;

import androidx.annotation.Keep;

@Keep
public interface SimpleRequestCallback<T> {
    void onRequestComplete(T object);
}

