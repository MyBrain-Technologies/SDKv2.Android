package utils

import android.os.Build
import androidx.annotation.Keep
import engine.clientevents.BaseError
import engine.clientevents.BaseErrorEvent
import engine.clientevents.BasicError
import java.util.concurrent.*

@Keep
class MbtAsyncWaitOperation<T> {
    private var lockOperation: MbtLock<T>? = null
    private var futureOperation: Future<T>? = null

    companion object {
        @JvmField
        val CANCEL: Any? = null
        private val TAG = MbtAsyncWaitOperation::class.java.name
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) futureOperation = CompletableFuture() else lockOperation = MbtLock()
    }

    /**
     * Wait and block the current thread until the [MbtAsyncWaitOperation.stopWaitingOperation] method is called
     * @param timeout maximum amount of time to wait for [MbtAsyncWaitOperation.stopWaitingOperation] method call
     * For MbtAsyncWaitOperation<Object> :
     * @return the result of waiting operation if the [MbtAsyncWaitOperation.stopWaitingOperation] method has been called within the allocated amount of time
     * For MbtAsyncWaitOperation<Boolean> :
     * @return true if the waiting operation succeeded : in other words, if the [MbtAsyncWaitOperation.stopWaitingOperation] method has been called within the allocated amount of time
     * return false if the waiting operation timed out (the [MbtAsyncWaitOperation.stopWaitingOperation] method has never been called) or has been cancelled
     * @throws ExecutionException
     * @throws TimeoutException
    </Boolean></Object> */
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun waitOperationResult(timeout: Int): T? {
        val result: T?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            futureOperation = CompletableFuture()
            result = futureOperation?.get(timeout.toLong(), TimeUnit.MILLISECONDS)
        } else {
            lockOperation = MbtLock()
            result = lockOperation?.waitAndGetResult(timeout.toLong())
        }
        return result
    }

    /**
     * Set the value of the current waiting instance to null
     */
    fun resetWaitingOperation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) futureOperation = null else lockOperation = null
    }

    /**
     * Complete or cancel the waiting process
     * @param result must be set to null ([) to cancel the waiting process (if something went wrong and the expected result has not been retrieved).][MbtAsyncWaitOperation.CANCEL]
     */
    fun stopWaitingOperation(result: T?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (futureOperation != null && futureOperation?.isDone != true && futureOperation?.isCancelled != true) {
                if (result === CANCEL) futureOperation?.cancel(true) else (futureOperation as CompletableFuture<T>).complete(result)
            }
        } else if (lockOperation != null && lockOperation?.isWaiting == true) lockOperation?.setResultAndNotify(result)
    }

    private val isTerminated: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) futureOperation!!.isDone else !lockOperation!!.isWaiting

    private val isReset: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) futureOperation == null else lockOperation == null

    private val isCancelled: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) futureOperation?.isCancelled == true else isReset

    val isWaiting: Boolean
        get() = !isReset && !isTerminated && !isCancelled


    public fun tryOperation(operation: ()-> Unit, catchCallback: BaseErrorEvent<BaseError>?, finally: (()-> Unit)?, timeout: Int) {
        try {
            AsyncUtils.Companion.executeAsync ( Runnable { operation.invoke() } )
            waitOperationResult(timeout)
        } catch (exception: Exception) {
            LogUtils.e(TAG, "Exception raised : \n $exception")
            catchCallback?.onError(BasicError(exception), null)
        } finally {
            finally?.invoke()
        }
    }

}