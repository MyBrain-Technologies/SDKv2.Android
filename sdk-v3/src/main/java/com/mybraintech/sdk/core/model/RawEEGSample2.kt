package com.mybraintech.sdk.core.model

/**
 * refacto from old sdk
 * Object that contains the EEG raw data for all channels at a given moment and its associated status, if any
 */
data class RawEEGSample2(val eegData: List<ByteArray>?, val statusData: Float) {

    companion object {
        val NAN_PACKET = RawEEGSample2(null, Float.NaN)
    }
    override fun toString(): String {
        val eegDataString = eegData?.joinToString(separator = ", ", prefix = "[", postfix = "]") { byteArray ->
            byteArray.joinToString(separator = " ", prefix = "(", postfix = ")") { byte ->
                "%02X".format(byte) // Hex encoding
            }
        } ?: "null"
        return "RawEEGSample2(eegData=$eegDataString, statusData=$statusData)"
    }
}
