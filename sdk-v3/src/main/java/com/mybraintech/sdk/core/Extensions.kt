package com.mybraintech.sdk.core

import android.text.TextUtils
import android.util.Base64
import android.util.Log

fun ByteArray.encodeToHex(): String {
    val hexArray = "0123456789ABCDEF".toCharArray()

    val hexChars = CharArray(this.size * 2)
    for (j in this.indices) {
        val v = this[j].toInt() and 0xFF

        hexChars[j * 2] = hexArray[v ushr 4]
        hexChars[j * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
}


fun String.decodeFromHex(): ByteArray {
    check(length % 2 == 0) { throw IllegalArgumentException("Must have an even length") }
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

fun ByteArray.encodeBase64(): String? {
    var result = ""
    Log.d("ByteArray", "encodeBase64 write:$this")
    try {
        val flag = Base64.NO_WRAP
        Log.d("ByteArray", "encodeBase64 flag$flag")
        result = Base64.encodeToString(this, flag)
    } catch (ase: AssertionError) {
        Log.d("ByteArray", "AssertionError:" + ase.message)
        result = "AssertionErrorException:${ase.message}"
    } catch (e: Exception) {
        Log.d("ByteArray", "Exception:" + e.message)
        result = "ByteArrayException:${e.message}"
    }
    Log.d("ByteArray", "result write:$result")
    return result
}

fun String.decodeBase64(): ByteArray {
    return try {
        if (TextUtils.isEmpty(this)) {
            byteArrayOf()
        } else Base64.decode(this, Base64.NO_WRAP)
    } catch (e: Exception) {
        byteArrayOf()
    }
}