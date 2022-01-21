package com.mybraintech.sdk

import android.content.Context
import com.mybraintech.sdk.core.MbtClientV2
import com.mybraintech.sdk.core.model.EnumMBTDevice

object MbtClientFactory {

    fun createMbtClient(context: Context, deviceType: EnumMBTDevice): MbtClient {
        return MbtClientV2(context, deviceType)
    }
}