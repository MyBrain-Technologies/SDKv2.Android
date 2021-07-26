package indus5

import model.AccelerometerFrame
import model.Position3D
import timber.log.Timber

object AccelerometerSingleton {

    val accelerometerPositions = arrayListOf<Position3D>()

    var accelerometerListener: AccelerometerListener = object : AccelerometerListener {
        override fun onAccelerometerStarted() {
            Timber.i("onAccelerometerStarted")
        }

        override fun onAccelerometerStopped() {
            Timber.i("onAccelerometerStopped")
        }

        override fun onNewAccelerometerFrame(frame: AccelerometerFrame) {
//            Timber.v("received IMS frame : ${frame.toString()}")
            addFrameWithCorrection(accelerometerPositions, frame)
        }

        override fun onAccelerometerError(e: Exception) {
            Timber.e(e)
        }
    }

    //TODO: add interpolar calculate for mission frame or something else ?
    private fun addFrameWithCorrection(buffer: ArrayList<Position3D>, frame: AccelerometerFrame) {
        val currentIndex = buffer.size -1 //count from 0
        val frameIndex = frame.packetIndex
        buffer.addAll(frame.positions)
    }


}