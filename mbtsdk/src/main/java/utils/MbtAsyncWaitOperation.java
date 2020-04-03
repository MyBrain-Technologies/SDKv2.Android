package utils;

import android.os.Build;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.annotation.Keep;

@Keep
public final class MbtAsyncWaitOperation<T> {

    public static final Object CANCEL = null;

    private MbtLock<T> lockOperation;
    private Future<T> futureOperation;

    private static String TAG = MbtAsyncWaitOperation.class.getName();

    public MbtAsyncWaitOperation() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            futureOperation = new CompletableFuture<>();
            else
                this.lockOperation = new MbtLock<>();
    }

    /**
     * Wait and block the current thread until the {@link MbtAsyncWaitOperation#stopWaitingOperation(T)} method is called
     * @param timeout maximum amount of time to wait for {@link MbtAsyncWaitOperation#stopWaitingOperation(T)} method call
     * For MbtAsyncWaitOperation<Object> :
     * @return the result of waiting operation if the {@link MbtAsyncWaitOperation#stopWaitingOperation(T)} method has been called within the allocated amount of time
     * For MbtAsyncWaitOperation<Boolean> :
     * @return true if the waiting operation succeeded : in other words, if the {@link MbtAsyncWaitOperation#stopWaitingOperation(T)} method has been called within the allocated amount of time
     * return false if the waiting operation timed out (the {@link MbtAsyncWaitOperation#stopWaitingOperation(T)} method has never been called) or has been cancelled
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public T waitOperationResult(int timeout) throws InterruptedException, ExecutionException, TimeoutException {
        T result;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            futureOperation = new CompletableFuture<>();
            result = futureOperation.get(timeout, TimeUnit.MILLISECONDS);
        }else {
           lockOperation = new MbtLock<>();
           result  = lockOperation.waitAndGetResult(timeout);
        }
        return result;
    }

    /**
     * Set the value of the current waiting instance to null
     */
    public void resetWaitingOperation(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            futureOperation = null;
        else
            lockOperation = null;
    }

    /**
     * Complete or cancel the waiting process
     * @param result must be set to null ({@link MbtAsyncWaitOperation#CANCEL) to cancel the waiting process (if something went wrong and the expected result has not been retrieved).
     * isCancel must be set to false to complete the waiting process (if the result has been retrieved).
     */
    public void stopWaitingOperation(T result) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (futureOperation != null && !futureOperation.isDone() && !futureOperation.isCancelled()){
                if (result == CANCEL)
                    futureOperation.cancel(true);
                else
                    ((CompletableFuture) futureOperation).complete(result);
            }
        } else
            if (lockOperation != null && lockOperation.isWaiting())
                lockOperation.setResultAndNotify(result);

    }

    private boolean isTerminated(){
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? futureOperation.isDone() : (!lockOperation.isWaiting()));
    }

    private boolean isReset(){
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? (futureOperation == null) : (lockOperation == null));
    }

    private boolean isCancelled(){
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? (futureOperation.isCancelled()) : this.isReset());
    }

    public boolean isWaiting(){
        return (!this.isReset() && !this.isTerminated() && !this.isCancelled());
    }
}
