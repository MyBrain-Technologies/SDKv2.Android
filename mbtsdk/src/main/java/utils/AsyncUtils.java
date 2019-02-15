package utils;

import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * Class helper offering solution to execute any give operations in an asynchronous way thanks
 * to the usage of threads and threadpools
 * Created by Vincent on 31/07/2015.
 */
@Keep
public final class AsyncUtils {
    private final static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            //Executors.newCachedThreadPool();
    private final static String TAG = "AsyncUtils";

    /**
     * Execute the given runnable in a threadpool internally managed by a <code>ExecutorService</code>
     * which allows asynchronous operations, meaning the code inside the runnable will be performed
     * alongside the (current) calling thread
     * @param runAsync  the runnable to run as an asynchronous operation - Must not be <code>NULL</code>!
     * @throws  IllegalArgumentException in case the runnable is null
     * @see ExecutorService
     * @see Executors
     */
    public static void executeAsync(@Nullable final Runnable runAsync) {
        if (runAsync == null)
            throw new IllegalArgumentException("runAsync MUST NOT be NULL");
        try{
            executor.execute(runAsync);
        }catch (RejectedExecutionException e){
            Log.e(TAG, "Rejected execution");
        }
    }

    @Nullable
    public static <T> Future<T> executeAsync(@Nullable final Callable<T> callable){
        Future<T> asyncResult = null;
        if (callable == null)
            throw new IllegalArgumentException("callable MUST NOT be NULL");
        try{
           asyncResult = executor.submit(callable);
        }catch (RejectedExecutionException e){
            Log.e(TAG, "Rejected execution");
        }
        return asyncResult;
    }
}
