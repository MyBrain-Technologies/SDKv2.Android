package com.mybraintech.sdk

import java.io.ByteArrayOutputStream


class UnitTestFileReader {

    /**
     * assuming fileName is the full name of a file in /test/resources
     */
    fun readFile(fileName: String): ByteArray {
        javaClass.classLoader?.getResourceAsStream(fileName)?.let {
            val result = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var length = it.read(buffer)
            while (length != -1) {
                result.write(buffer, 0, length)
                length = it.read(buffer)
            }
            return result.toByteArray()
        }
        throw RuntimeException("Couldn't read file")
    }
}
