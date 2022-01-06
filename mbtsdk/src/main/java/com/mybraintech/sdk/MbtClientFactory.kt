package com.mybraintech.sdk

import android.content.Context
import com.mybraintech.sdk.core.MbtClientV2

object MbtClientFactory {

    fun createMbtClient(context: Context, isQPlusDevice: Boolean): MbtClient {
        return if (isQPlusDevice) {
            MbtClientV2(context)
        } else {
            TODO("Melomind : to implement")
        }
    }
}