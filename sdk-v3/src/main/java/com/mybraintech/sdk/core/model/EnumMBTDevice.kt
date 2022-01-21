package com.mybraintech.sdk.core.model

enum class EnumMBTDevice {
    Q_PLUS, MELOMIND;

    fun getStatusAllocationSize() : Int {
        when (this) {
            Q_PLUS -> return 1
            MELOMIND -> TODO("melomind: not yet implemented")
        }
    }
}