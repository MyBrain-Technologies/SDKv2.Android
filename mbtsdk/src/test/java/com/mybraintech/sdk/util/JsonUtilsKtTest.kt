package com.mybraintech.sdk.util

import com.mybraintech.sdk.core.model.DeviceInformation
import org.junit.jupiter.api.Assertions.*

import org.junit.Test

internal class JsonUtilsKtTest {

    @Test
    fun toJson() {
        println("test toJson started")
        val di = DeviceInformation().apply { name = "alpha" }
        val result = di.toJson()
        println(result)
        assert(!result.isNullOrBlank())
    }
}