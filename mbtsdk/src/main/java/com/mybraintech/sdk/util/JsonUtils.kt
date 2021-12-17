package com.mybraintech.sdk.util

import android.bluetooth.BluetoothDevice
import com.google.gson.Gson

fun Any?.toJson() : String {
    return if (this != null) {
        Gson().toJson(this)
    } else {
        ""
    }
}

fun BluetoothDevice?.getString() : String {
    return if (this != null) {
        "[ name = ${this.name} | address = ${this.address} | type = ${this.type} ]"
    } else {
        ""
    }
}