package utils

import android.util.Log
import androidx.annotation.Keep
import java.util.concurrent.*

/**
 * Class helper offering solution to execute any give operations in an asynchronous way thanks
 * to the usage of threads and threadpools
 * Created by Vincent on 31/07/2015.
 */
@Keep
object AsyncUtils {
  //Executors.newCachedThreadPool();
  val TAG = this::class.java.simpleName

  private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

  /**
   * Execute the given runnable in a threadpool internally managed by a `ExecutorService`
   * which allows asynchronous operations, meaning the code inside the runnable will be performed
   * alongside the (current) calling thread
   * @param runAsync  the runnable to run as an asynchronous operation - Must not be `NULL`!
   * @throws  IllegalArgumentException in case the runnable is null
   * @see ExecutorService
   *
   * @see Executors
   */
  @JvmStatic
  fun executeAsync(runAsync: Runnable) {
    try {
      executor.execute(runAsync)
    } catch (e: RejectedExecutionException) {
      Log.e(TAG, "Rejected execution")
    }
  }

    @JvmStatic
    fun executeAsync(runAsync: () -> Unit) {
    try {
      executor.execute(Runnable { runAsync.invoke() })
    } catch (e: RejectedExecutionException) {
      Log.e(TAG, "Rejected execution")
    }
  }

    @JvmStatic
    fun <T> executeAsync(callable: Callable<T>): Future<T>? {
    var asyncResult: Future<T>? = null
    try {
      asyncResult = executor.submit(callable)
    } catch (e: RejectedExecutionException) {
      Log.e(TAG, "Rejected execution")
    }
    return asyncResult
  }
}
