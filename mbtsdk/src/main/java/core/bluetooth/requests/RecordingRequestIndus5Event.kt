package core.bluetooth.requests

import config.RecordConfig

data class RecordingRequestIndus5Event @JvmOverloads constructor(
        val isStart: Boolean = false,
        val recordConfig: RecordConfig? = null) {}