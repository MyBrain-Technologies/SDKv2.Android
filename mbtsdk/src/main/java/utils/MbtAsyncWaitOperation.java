package utils;

import android.os.Build;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class MbtAsyncWaitOperation {

    private MbtLock<Boolean> lockOperation;
    private Future<Boolean> futureOperation;

    /**
     * Wait and block the current thread until the {@link MbtAsyncWaitOperation#stopWaitingOperation(boolean)} method is called
     * @param timeout maximum amount of time to wait for {@link MbtAsyncWaitOperation#stopWaitingOperation(boolean)} method call
     * @return true if the waiting operation succeeded : in other words, if the {@link MbtAsyncWaitOperation#stopWaitingOperation(boolean)} method has been called within the allocated amount of time
     * return false if the waiting operation timed out (the {@link MbtAsyncWaitOperation#stopWaitingOperation(boolean)} method has never been called) or has been cancelled
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public Boolean waitOperationResult(int timeout) throws InterruptedException, ExecutionException, TimeoutException {
        Boolean operationSucceeded;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            futureOperation = new CompletableFuture<>();
            operationSucceeded = futureOperation.get(timeout, TimeUnit.MILLISECONDS);
        }else {
           lockOperation = new MbtLock<>();
           operationSucceeded = lockOperation.waitAndGetResult(timeout);
        }
        return operationSucceeded;
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
     * @param isCancel must be set to true to cancel the waiting process (if something went wrong and the expected result has not been retrieved).
     * isCancel must be set to false to complete the waiting process (if the result has been retrieved).
     */
    public void stopWaitingOperation(boolean isCancel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (futureOperation != null && !futureOperation.isDone() && !futureOperation.isCancelled()){
                if (isCancel)
                    futureOperation.cancel(true);
                else
                    ((CompletableFuture) futureOperation).complete(true);
        }
        } else
            if (lockOperation != null && lockOperation.isWaiting())
                lockOperation.setResultAndNotify(isCancel ? null : true);
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
