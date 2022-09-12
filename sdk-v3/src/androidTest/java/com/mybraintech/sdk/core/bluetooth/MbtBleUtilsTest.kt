package com.mybraintech.sdk.core.bluetooth

import android.bluetooth.BluetoothDevice
import androidx.test.platform.app.InstrumentationRegistry
import com.mybraintech.sdk.R
import org.junit.Assert.*

import org.junit.Test

class MbtBleUtilsTest {

    val testSet = listOf<Pair<String, Boolean>>(
        Pair("qp_2220100000", false),
        Pair("qp_2220100001", true),
        Pair("qp_2220100002", true),
        Pair("qp_2220100003", true),
        Pair("qp_2220100004", false)
    )

    @Test
    fun test_isHyperion() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val hyperions = appContext.resources.getStringArray(R.array.hyperion_devices)
        for (entry in testSet) {
            assertEquals(hyperions.contains(entry.first), entry.second)
        }
    }
}