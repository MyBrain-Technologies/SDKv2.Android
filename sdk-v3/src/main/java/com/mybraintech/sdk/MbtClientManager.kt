package com.mybraintech.sdk

import android.content.Context
import com.mybraintech.sdk.core.MbtClientImpl
import com.mybraintech.sdk.core.model.EnumMBTDevice

object MbtClientManager {

    private var clientMap = hashMapOf<EnumMBTDevice, MbtClient>()
    private val lock = Unit

    fun getMbtClient(context: Context, deviceType: EnumMBTDevice): MbtClient {
        synchronized(lock) {
            if (!clientMap.containsKey(deviceType)) {
                val newClient = MbtClientImpl(context, deviceType)
                clientMap[deviceType] = newClient
            }
            return clientMap[deviceType]!!
        }
    }
}