package core.bluetooth.lowenergy

import utils.MbtAsyncWaitOperation

/**
 * This class is to help to communicate between threads.
 * This is a bad practice but we will refactor later
 */
object FlagHelper {

    private var isRxDescriptorChanged = false
    var asyncWaitOperation = MbtAsyncWaitOperation<Boolean>()

    @Synchronized
    fun setRxDescriptorFlag(isChanged: Boolean) {
        isRxDescriptorChanged = isChanged
    }

    @Synchronized
    fun isRxDescriptorOk(): Boolean {
        return isRxDescriptorChanged
    }
}