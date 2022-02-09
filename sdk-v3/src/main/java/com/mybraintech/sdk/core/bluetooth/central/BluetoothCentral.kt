package com.mybraintech.sdk.core.bluetooth.central

import com.mybraintech.sdk.core.listener.BatteryLevelListener

interface IBluetoothUsage {
    fun readBatteryLevelMbt()
    fun setBatteryLevelListener(batteryLevelListener: BatteryLevelListener?)
}
