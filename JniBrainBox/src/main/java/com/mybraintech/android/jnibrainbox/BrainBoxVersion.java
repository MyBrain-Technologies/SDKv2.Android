package com.mybraintech.android.jnibrainbox;

public class BrainBoxVersion {

    /**
     * the version number must be validated by research team and data team
     */
    private static String BRAIN_BOX_VERSION = "3.0.0";

    /**
     * ATTENTION: 10 July 2021: in the file Version.hpp, the version is noted as 1.0.0 but we decided to put the value 3.0.0 since the previous version was 2.14.0
     * @see BrainBox/Version.hpp
     */
    public static String getVersion() {
        return BRAIN_BOX_VERSION;
    }
}
