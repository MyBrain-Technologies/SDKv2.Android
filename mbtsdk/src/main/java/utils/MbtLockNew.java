package utils;

/**
 * Created by manon on 26/08/16.
 */

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is a small helper to provide feedback on lock wait and notify process
 */
@Keep
public final class MbtLockNew<T> {
    private final static String TAG = "MBT Lock";

    @Nullable
    private T result = null;
    private boolean isWaiting = false;
    private boolean wasNotified = false;
    private Lock lock = new ReentrantLock();

    /**
     * Releases the lock on this instance to notify the thread that was waiting
     */
    public final void unlock() {
        Log.i(TAG, "set result and notify");
        if (this.isWaiting) {
            lock.unlock();
        } else
            this.wasNotified = true;
    }

    /**
     * Releases the lock on this instance to notify the thread that was waiting and to provide the result
     * @param result    the result of the asynchronous operation that was waited
     */
    public final void setResultAndNotify(@Nullable final T result) {
        Log.i(TAG, "set result and notify");
        this.result = result;
        if (this.isWaiting) {
            lock.unlock();
        } else
            this.wasNotified = true;
    }

    /**
     * This method locks on the current instance and wait for the duration specified in milliseconds
     * @param timeout the duration, in milliseconds, the thread will wait if it does not get notify
     */
    @Nullable
    public final T waitAndGetResult(final long timeout, TimeUnit unit) {
        Log.i(TAG, "wait and get result ");
        if (timeout < 0)
            throw new IllegalArgumentException("timeout cannot be negative");
        if (this.wasNotified) {// it might happen that the notify was performed even before the wait started!
            this.wasNotified = false;
            return this.result;
        }

        try {
            lock.tryLock(timeout, unit);
        } catch (@NonNull final InterruptedException e) {
            Log.e(TAG, "Failed to wait, Thread got interrupted -> " + e.getMessage());
            Log.getStackTraceString(e);
        }
        return null;
    }

    /**
     * This method locks on the current instance and wait as long necessary (no timeout)
     */
    @Nullable
    public final T waitAndGetResult() {
        if (this.wasNotified) {// it might happen that the notify was performed even before the wait started!
            this.wasNotified = false;
            return this.result;
        }
        lock.tryLock();
        return null;
    }

    /**
     * Releases the lock on this instance to notify the thread that was waiting and to provide the result
     * @param result    the result of the asynchronous operation that was waited
     */
    public synchronized final void setResult(@Nullable final T result) {
        this.result = result;
    }


    /**
     * Tells whether this instance is currently waiting or not
     * @return <code>true</code> if waiting, <code>false</code> otherwise
     */
    public final boolean isWaiting() {
        return this.isWaiting;
    }
}
