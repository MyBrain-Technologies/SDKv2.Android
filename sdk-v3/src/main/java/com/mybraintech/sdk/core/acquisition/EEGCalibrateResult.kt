package com.mybraintech.sdk.core.acquisition

data class EEGCalibrateResult(val success: Boolean, val errorMessage: String?) {
    internal var iaf: FloatArray = floatArrayOf()
    internal var rms: FloatArray = floatArrayOf()
}

