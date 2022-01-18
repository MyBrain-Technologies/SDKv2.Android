package com.mybraintech.sdk.core.model

import com.google.gson.annotations.SerializedName

class DeviceInformation {

    @SerializedName("productName")
    var productName: String = ""

    @SerializedName("uniqueDeviceIdentifier")
    var uniqueDeviceIdentifier: String = ""

    @SerializedName("firmwareVersion")
    var firmwareVersion: String = ""

    @SerializedName("hardwareVersion")
    var hardwareVersion: String = ""

    fun isCompleted(): Boolean {
        return productName.isNotBlank()
                && uniqueDeviceIdentifier.isNotBlank()
                && firmwareVersion.isNotBlank()
                && hardwareVersion.isNotBlank()
    }
}