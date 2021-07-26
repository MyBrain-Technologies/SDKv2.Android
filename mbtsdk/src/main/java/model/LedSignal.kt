package model

import java.math.BigInteger

data class LedSignal(val bytes: ByteArray) {

    val value: Long = BigInteger(bytes).toLong()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LedSignal

        if (this.value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}
