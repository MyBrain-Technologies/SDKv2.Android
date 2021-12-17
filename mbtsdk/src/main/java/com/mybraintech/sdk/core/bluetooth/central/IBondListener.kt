package com.mybraintech.sdk.core.bluetooth.central

interface IBondListener {
    fun onBondNone()
    fun onBondBonded()
}