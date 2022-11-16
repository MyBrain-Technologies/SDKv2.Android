package com.mybraintech.sdk.core.model

import com.google.gson.annotations.SerializedName

class DeviceInformation {

    @SerializedName("productName")
    var productName: String = ""

    /**
     * Json serialization name *uniqueDeviceIdentifier* follows BrainWeb swagger specification.
     *
     * Variable name *serialNumber* follows hardware specification.
     */
    @SerializedName("uniqueDeviceIdentifier")
    var serialNumber: String = ""

    @SerializedName("firmwareVersion")
    var firmwareVersion: String = ""

    @SerializedName("hardwareVersion")
    var hardwareVersion: String = ""

    fun isCompleted(): Boolean {
        return productName.isNotBlank()
                && serialNumber.isNotBlank()
                && firmwareVersion.isNotBlank()
                && hardwareVersion.isNotBlank()
    }
}