package indus5

import model.AccelerometerFrame

interface AccelerometerInterface {

    /**
     * @return true if the start command was send successfully, false otherwise
     */
    fun startAccelerometer(listener: AccelerometerListener): Boolean

    /**
     * @return true if the stop command was send successfully, false otherwise
     */
    fun stopAccelerometer(): Boolean
}

interface AccelerometerListener {

    fun onAccelerometerStarted()

    fun onAccelerometerStopped()

    fun onNewAccelerometerFrame(frame: AccelerometerFrame)

    fun onAccelerometerError(e: Exception)
}