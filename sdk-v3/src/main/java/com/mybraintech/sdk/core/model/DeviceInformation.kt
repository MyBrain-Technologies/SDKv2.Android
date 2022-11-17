package com.mybraintech.sdk.core.model

import com.google.gson.annotations.SerializedName

class DeviceInformation {

    /**
     * Json serialization name *productName* follows BrainWeb swagger specification.
     *
     * Variable name [bleName] is the Bluetooth Low Energy name.
     */
    @SerializedName("productName")
    var bleName: String = ""

    var audioName: String = ""

    /**
     * Json serialization name *uniqueDeviceIdentifier* follows BrainWeb swagger specification.
     *
     * Variable name *serialNumber* follows firmware specification.
     */
    @SerializedName("uniqueDeviceIdentifier")
    var serialNumber: String = ""

    @SerializedName("firmwareVersion")
    var firmwareVersion: String = ""

    @SerializedName("hardwareVersion")
    var hardwareVersion: String = ""

    fun isCompleted(): Boolean {
        return bleName.isNotBlank()
                && serialNumber.isNotBlank()
                && firmwareVersion.isNotBlank()
                && hardwareVersion.isNotBlank()
    }
}