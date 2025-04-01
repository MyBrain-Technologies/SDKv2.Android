package com.mybraintech.sdk.core.model

class XonStatus() {
    var packetTimeStamp: UInt = 0u // ADC sampling time stamp in ms
    var syncTimeStamp: UInt = 0u // Time stamp of the radio transmit time in ms
    var packetSampleCounter: UInt = 0u // Sample counter value at PacketTimeStamp
    var rssi: Int = 0 // RSSI value in dBm
    var batteryVoltage: UShort = 0u// Battery voltage in mV
    var batteryLevel: UShort =
        0u // battery filling level 0 = unknown, 1 = empty, 2 = low, 3 = good, 4 = charging

    override fun toString(): String {
        return "XonStatus(packetTimeStamp=$packetTimeStamp, syncTimeStamp=$syncTimeStamp, packetSampleCounter=$packetSampleCounter, rssi=$rssi, batteryVoltage=$batteryVoltage, batteryLevel=$batteryLevel)"
    }
}