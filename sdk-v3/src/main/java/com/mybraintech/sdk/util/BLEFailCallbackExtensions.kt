package com.mybraintech.sdk.util

/**
 * @see [no.nordicsemi.android.ble.callback.FailCallback]
 */
fun Int.getHumanReadable(): String {
    return when (this) {
        -1 -> "REASON_DEVICE_DISCONNECTED"
        -2 -> "REASON_DEVICE_NOT_SUPPORTED"
        -3 -> "REASON_NULL_ATTRIBUTE"
        -4 -> "REASON_REQUEST_FAILED"
        -5 -> "REASON_TIMEOUT"
        -6 -> "REASON_VALIDATION"
        -7 -> "REASON_CANCELLED"
        -100 -> "REASON_BLUETOOTH_DISABLED"
        else -> "REASON_UNKNOWN"
    }
}