package engine;

public interface SimpleRequestCallback<T> {
    void onRequestComplete(T object);
}
