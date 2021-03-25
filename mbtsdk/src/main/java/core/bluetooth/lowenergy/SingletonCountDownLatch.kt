package core.bluetooth.lowenergy

import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object SingletonCountDownLatch {

    var countDownBoolean = MyCountDownLatch<Boolean>()

    fun createBoolean(): MyCountDownLatch<Boolean> {
        countDownBoolean = MyCountDownLatch<Boolean>()
        return countDownBoolean
    }

    fun onResultBoolean(result: Boolean) {
        countDownBoolean.onResult(result)
    }
}

open class MyCountDownLatch<T> {

    private var signal: CountDownLatch = CountDownLatch(1)
    private var result: Result? = null

    /**
     * @param timeout in ms
     */
    fun waitForResult(timeout: Long, work: Runnable): MyCountDownLatch<T>.Result {
        signal = CountDownLatch(1)
        return try {
            Thread(work).start()
            Timber.i("waitForResult : start waiting...")
            signal.await(timeout, TimeUnit.MILLISECONDS)
            result ?: Result(false, null)
        } catch (e: InterruptedException) {
            Timber.e(e)
            Result(false, null)
        }
    }

    fun onResult(result: T) {
        Timber.i("onResult : result = $result")
        this.result = Result(true, result)
        signal.countDown()
    }

    inner class Result(val isSuccessful: Boolean, val value: T?)
}