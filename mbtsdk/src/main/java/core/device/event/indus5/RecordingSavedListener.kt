package core.device.event.indus5

import config.RecordConfig

interface RecordingSavedListener {
    fun onRecordingSaved(recordConfig: RecordConfig)
}