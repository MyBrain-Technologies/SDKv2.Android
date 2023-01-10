package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.AccelerometerConfig
import com.mybraintech.sdk.core.model.TimedBLEFrame

/**
 * MBT device (Melomind/QPlus) send data to sdk via this interface
 */
interface MbtDataReceiver {
    fun onTriggerStatusConfiguration(triggerStatusAllocationSize: Int)
    fun onAccelerometerConfiguration(accelerometerConfig: AccelerometerConfig)
    fun onEEGFrame(data: TimedBLEFrame)
    fun setEEGListener(eegListener: EEGListener?)
    fun setEEGRealtimeListener(eegRealtimeListener: EEGRealtimeListener?)
    fun onAccelerometerFrame(data: ByteArray)
    fun setAccelerometerListener(accelerometerListener: AccelerometerListener?)
    fun onEEGDataError(error: Throwable)
}