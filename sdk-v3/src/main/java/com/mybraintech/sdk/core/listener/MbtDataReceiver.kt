package com.mybraintech.sdk.core.listener

/**
 * MBT device (Melomind/QPlus) send data to sdk via this interface
 */
interface MbtDataReceiver {
    fun onTriggerStatusConfiguration(triggerStatusAllocationSize: Int)
    fun onEEGFrame(data: ByteArray)
    fun setEEGListener(eegListener: EEGListener?)
    fun onIMSFrame(data: ByteArray)
    fun setIMSListener(accelerometerListener: AccelerometerListener?)
    fun onEEGDataError(error: Throwable)
}