package com.mybraintech.sdk.core.acquisition.eeg

import java.lang.UnsupportedOperationException

/**
 * Melomind and QPlus are GATT profile
 */
enum class EnumBluetoothProtocol {
    SPP, BLE, A2DP;

    fun getFrameIndexAllocationSize() : Int {
        return when (this) {
            SPP -> 3
            BLE -> 2
            A2DP -> throw UnsupportedOperationException()
        }
    }
}