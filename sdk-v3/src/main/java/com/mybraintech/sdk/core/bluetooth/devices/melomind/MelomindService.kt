package com.mybraintech.sdk.core.bluetooth.devices.melomind

import java.util.*


enum class MelomindService(val uuid: UUID) {
    DEVICE_INFORMATION(UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")),
    MEASUREMENT(UUID.fromString("0000b2a0-0000-1000-8000-00805f9b34fb"));
}