package indus5

import model.PpgFrame

interface PpgListener {
    fun onPpgStarted()
    fun onPpgStopped()
    fun onPpgFrame(frame: PpgFrame)
    fun onPpgError()
}