package com.mybraintech.android.jnibrainbox

object BrainBoxVersion {

    /**
     * the version number must be validated by research team and data team
     */
    private const val BRAIN_BOX_VERSION = "3.0.0"

    /**
     * ATTENTION: 10 July 2021: in the file Version.hpp, the version is noted as 1.0.0 but we decided to put the value 3.0.0 since the previous version was 2.14.0
     * @see Version.hpp
     */
    @JvmStatic
    fun getVersion(): String {
        return BRAIN_BOX_VERSION
    }
}