package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.TimedBLEFrame

/**
 * MBT device (Melomind/QPlus) send data to sdk via this interface
 */
interface MbtDataReceiver {
    fun onTriggerStatusConfiguration(triggerStatusAllocationSize: Int)
    fun onEEGFrame(data: TimedBLEFrame)
    fun setEEGListener(eegListener: EEGListener?)
    fun onIMSFrame(data: ByteArray)
    fun setIMSListener(accelerometerListener: AccelerometerListener?)
    fun onEEGDataError(error: Throwable)
}