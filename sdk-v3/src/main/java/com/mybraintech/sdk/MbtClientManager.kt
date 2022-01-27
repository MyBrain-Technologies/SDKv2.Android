package com.mybraintech.sdk

import android.content.Context
import com.mybraintech.sdk.core.MbtClientImpl
import com.mybraintech.sdk.core.model.EnumMBTDevice

object MbtClientManager {

    private var mbtClient: MbtClient? = null

    fun getMbtClient(context: Context, deviceType: EnumMBTDevice): MbtClient {
        if (mbtClient == null || mbtClient?.getDeviceType() != deviceType) {
            if (mbtClient == null) {
                mbtClient = MbtClientImpl(context)
            }
            mbtClient?.setDeviceType(deviceType)
        }
        return mbtClient!!
    }
}