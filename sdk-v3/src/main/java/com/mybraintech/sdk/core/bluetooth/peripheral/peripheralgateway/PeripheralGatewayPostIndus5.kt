package com.mybraintech.sdk.core.bluetooth.peripheral.peripheralgateway

import android.bluetooth.BluetoothGattCharacteristic
import com.mybraintech.sdk.core.bluetooth.peripheral.IPeripheralListener
import com.mybraintech.sdk.core.model.DeviceInformation
import java.lang.Error

class PeripheralGatewayPostIndus5 : IPeripheralGateway {
    override var isReady: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    override val information: DeviceInformation?
        get() = TODO("Not yet implemented")
    override val peripheralListiner: IPeripheralListener?
        get() = TODO("Not yet implemented")
    override var isA2dpConnected: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    override var ad2pName: String?
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun discover(characteristic: BluetoothGattCharacteristic) {
        TODO("Not yet implemented")
    }

    override fun requestBatteryLevel() {
        TODO("Not yet implemented")
    }

    override fun handleValueUpdate(characteristic: BluetoothGattCharacteristic, error: Error?) {
        TODO("Not yet implemented")
    }

    override fun handleNotificationStateUpdate(
        characteristic: BluetoothGattCharacteristic,
        error: Error?
    ) {
        TODO("Not yet implemented")
    }

    override fun handleValueWrite(characteristic: BluetoothGattCharacteristic, error: Error?) {
        TODO("Not yet implemented")
    }
}