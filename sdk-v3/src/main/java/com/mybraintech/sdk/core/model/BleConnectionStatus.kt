package com.mybraintech.sdk.core.model

data class BleConnectionStatus(
    /**
     * is not null if there is a connected MbtDevice.
     */
    val mbtDevice: MbtDevice?,

    /**
     * true if there is a connected MbtDevice and connection process is finished.
     *
     * false if there is no connected MbtDevice or connection process is not finished.
     */
    val isConnectionEstablished: Boolean
)