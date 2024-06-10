package com.mybraintech.sdk.core.model

/**
 * TODO: specify error code for all implement.
 */
enum class MBTErrorCode(val code: Int) {
    BLUETOOTH_DISABLED(1011),
    DEVICE_CONNECTED_ALREADY(1013),
    FAILED_TO_CONNECTED_TO_DEVICE(1015),
    NO_CONNECTED_DEVICE_TO_CONNECT(1017),
}