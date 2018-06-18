package utils;

/**
 * Created by manon on 26/08/16.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * This class is a small helper to provide feedback on lock wait and notify process
 */
public final class MbtLock<T> {
    private final static String TAG = "MBT Lock";

    @Nullable
    private T result = null;
    private boolean isWaiting = false;
    private boolean wasNotified = false;

    /**
     * Releases the lock on this instance to notify the thread that was waiting and to provide the result
     * @param result    the result of the asynchronous operation that was waited
     */
    public synchronized final void setResultAndNotify(@Nullable final T result) {
        this.result = result;
        if (this.isWaiting) {
            synchronized (this) {
                this.notify();
            }
        } else
            this.wasNotified = true;
    }

    /**
     * This method locks on the current instance and wait for the duration specified in milliseconds
     * @param timeout the duration, in milliseconds, the thread will wait if it does not get notify
     */
    @Nullable
    public synchronized final T waitAndGetResult(final long timeout) {
        if (timeout < 0)
            throw new IllegalArgumentException("timeout cannot be negative");
        if (this.wasNotified) {// it might happen that the notify was performed even before the wait started!
            this.wasNotified = false;
            return this.result;
        }

        try {
            synchronized (this) {
                this.isWaiting = true;
                this.result = null;
                this.wait(timeout);
                this.isWaiting = false;
                return this.result;
            }
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
    public synchronized final T waitAndGetResult() {
        try {
            if (this.wasNotified) {// it might happen that the notify was performed even before the wait started!
                this.wasNotified = false;
                return this.result;
            }

            synchronized (this) {
                this.isWaiting = true;
                this.result = null;
                this.wait();
                this.isWaiting = false;
                return this.result;
            }
        } catch (@NonNull final InterruptedException e) {
            Log.e(TAG, "Failed to wait, Thread got interrupted -> " + e.getMessage());
            Log.getStackTraceString(e);
        }
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
