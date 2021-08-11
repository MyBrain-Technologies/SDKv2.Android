package indus5

import androidx.annotation.Keep

@Keep
interface FirmwareListener {

    fun onFirmwareVersion(version: String)
}