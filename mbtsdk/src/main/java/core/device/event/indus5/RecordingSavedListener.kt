package core.device.event.indus5

import androidx.annotation.Keep
import config.RecordConfig

@Keep
interface RecordingSavedListener {
    fun onRecordingSaved(recordConfig: RecordConfig)
}