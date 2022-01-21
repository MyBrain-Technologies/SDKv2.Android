package com.mybraintech.sdk.core.error

sealed class MBTError(error: Throwable) {
    class BluetoothError(val error: Throwable): MBTError(error)
    class AudioError(val error: Throwable): MBTError(error)
}