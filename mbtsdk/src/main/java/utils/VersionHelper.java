package utils;

import android.util.Log;

import java.util.Arrays;

/**
 * Created by Etienne on 2/26/2018.
 * This class wraps the fw version into three parts: main major and minor.
 * A valid fwVersion format is X.Y.Z where
 * X = main
 * Y = major
 * Z = minor
 * Each Xs Ys and Zs are numbers
 */
public final class VersionHelper {

    public final static String TAG = VersionHelper.class.getSimpleName();

    /**
     * Regular expression used to split any firmware version given as a string .
     */
    public static final String VERSION_SPLITTER = "\\.";

    /**
     * Number of digits of any firmware version given
     * as a string (split with {@link VersionHelper#VERSION_SPLITTER}
     * or as an integer array.
     */
    public static final int VERSION_LENGTH = 3;

    private static final int HEADSET_STATUS_MIN_VERSION_MAIN = 1;
    private static final int HEADSET_STATUS_MIN_VERSION_MAJOR = 5;
    private static final int HEADSET_STATUS_MIN_VERSION_MINOR = 13;

    private static final int OAD_WITHOUT_REQUEST_PRIORITY_HIGH_MIN_VERSION_MAIN = 1;
    private static final int OAD_WITHOUT_REQUEST_PRIORITY_HIGH_MIN_VERSION_MAJOR = 5;
    private static final int OAD_WITHOUT_REQUEST_PRIORITY_HIGH_MIN_VERSION_MINOR = 10;

    private static final int BLE_BONDING_VERSION_MAIN = 1;
    private static final int BLE_BONDING_VERSION_MAJOR = 6;
    private static final int BLE_BONDING_VERSION_MINOR = 7;

    private static final int A2DP_FROM_HEADSET_VERSION_MAIN = 1;
    private static final int A2DP_FROM_HEADSET_VERSION_MAJOR = 6;
    private static final int A2DP_FROM_HEADSET_VERSION_MINOR = 7;

    private static final int WRITE_EXTERNAL_NAME_VERSION_MAIN = 1;
    private static final int WRITE_EXTERNAL_NAME_VERSION_MAJOR = 7;
    private static final int WRITE_EXTERNAL_NAME_VERSION_MINOR = 1;

    private static final int RI_ALGO_SNR_MAIN = 1;
    private static final int RI_ALGO_SNR_MAJOR = 0;
    private static final int RI_ALGO_SNR_MINOR = 0;

    private static final int RI_ALGO_RMS_MAIN = 2;
    private static final int RI_ALGO_RMS_MAJOR = 5;
    private static final int RI_ALGO_RMS_MINOR = 0;

    private final String minor;
    private final String major;
    private final String main;

    public VersionHelper(String version){
        if(!isVersionLengthValid(version)){
            main = null;
            major = null;
            minor = null;
            return;
        }

        String[] versionAsStringArray = convertVersionToStringArray(version);
        main = versionAsStringArray[0];
        major = versionAsStringArray[1];
        minor = versionAsStringArray[2];

    }

    public String getMainVersionAsString(){
        return main;
    }

    public String getMajorVersionAsString(){
        return major;
    }

    public String getMinorVersionAsString(){
        return minor;
    }

    public int getMainVersionAsNumber(){
        return Integer.valueOf(main);
    }

    public int getMajorVersionAsNumber(){
        return Integer.valueOf(major);
    }

    public int getMinorVersionAsNumber(){
        return Integer.valueOf(minor);
    }

    public boolean isValidForFeature(Feature feature){
        boolean b = false;

        if(major == null || minor == null || main == null)
            return false;

        switch (feature){
            case HEADSET_STATUS:
                b = verifyFeature(HEADSET_STATUS_MIN_VERSION_MAIN, HEADSET_STATUS_MIN_VERSION_MAJOR, HEADSET_STATUS_MIN_VERSION_MINOR);
                break;

            case OAD_WITHOUT_CONNECTION_PRIORITY:
                b = verifyFeature(OAD_WITHOUT_REQUEST_PRIORITY_HIGH_MIN_VERSION_MAIN, OAD_WITHOUT_REQUEST_PRIORITY_HIGH_MIN_VERSION_MAJOR, OAD_WITHOUT_REQUEST_PRIORITY_HIGH_MIN_VERSION_MINOR);
                break;

            case BLE_BONDING:
                b = verifyFeature(BLE_BONDING_VERSION_MAIN, BLE_BONDING_VERSION_MAJOR, BLE_BONDING_VERSION_MINOR);
                break;

            case A2DP_FROM_HEADSET:
                b = verifyFeature(A2DP_FROM_HEADSET_VERSION_MAIN, A2DP_FROM_HEADSET_VERSION_MAJOR, A2DP_FROM_HEADSET_VERSION_MINOR);
                break;

            case REGISTER_EXTERNAL_NAME:
                b = verifyFeature(WRITE_EXTERNAL_NAME_VERSION_MAIN, WRITE_EXTERNAL_NAME_VERSION_MAJOR, WRITE_EXTERNAL_NAME_VERSION_MINOR);
                break;

            case SNR:
                b = verifyFeature(RI_ALGO_SNR_MAIN, RI_ALGO_SNR_MAJOR, RI_ALGO_RMS_MINOR);
                break;

            case RMS:
                b = verifyFeature(RI_ALGO_RMS_MAIN, RI_ALGO_RMS_MAJOR, RI_ALGO_RMS_MINOR);
                break;

            default:
                break;
        }
        Log.i(TAG, "feature test for " + feature.toString() + " is " + (b ? "valid" : "invalid"));
        return b;
    }


    /**
     * Verifies if a specific feature can be enabled on the bt communication based on the firmware version.
     * @param featureMain the minimum value for the main, ie X in X.Y.Z nomenclature
     * @param featureMajor the minimum value for the major, ie Y in X.Y.Z nomenclature
     * @param featureMinor the minimum value for the minor, ie Z in X.Y.Z nomenclature
     * @return
     */
    private boolean verifyFeature(int featureMain, int featureMajor, int featureMinor){
        if(checkMain(featureMain) < 0)
            return false;
        else if(checkMain(featureMain) > 0)
            return true;
        else if(checkMajor(featureMajor) < 0)
            return false;
        else if(checkMajor(featureMajor) > 0)
            return true;
        else return checkMinor(featureMinor);
    }

    /**
     * Compare the X parameter in the X.Y.Z version number to the minimum requested value for a specific feature.
     * @param featureMainVersion the minimum value for the specific feature
     * @return -1 if main is inferior to minimum value, 1 if superior or 0 if equal.
     */
    private int checkMain(int featureMainVersion){
        if(getMainVersionAsNumber() < featureMainVersion)
            return -1;
        else return getMainVersionAsNumber() > featureMainVersion ? 1 : 0;
    }

    /**
     * Compare the Y parameter in the X.Y.Z version number to the minimum requested value for a specific feature.
     * @param featureMajorVersion the minimum value for the specific feature
     * @return -1 if main is inferior to minimum value, 1 if superior or 0 if equal.
     */
    private int checkMajor(int featureMajorVersion){
        if(getMajorVersionAsNumber() < featureMajorVersion)
            return -1;
        else return getMajorVersionAsNumber() > featureMajorVersion ? 1 : 0;
    }

    /**
     * Compare the Z parameter in the X.Y.Z version number to the minimum requested value for a specific feature.
     * @param featureMinorVersion the minimum value for the specific feature
     * @return -1 if main is inferior to minimum value, 1 if superior or 0 if equal.
     */
    private boolean checkMinor(int featureMinorVersion){
        return getMinorVersionAsNumber() >= featureMinorVersion;
    }

    /**
     * Returns true if the version given in input contains at least {@link VersionHelper#VERSION_LENGTH} digits.
     * False otherwise.
     * @param version the version to check
     * @return true if the version given in input contains at least {@link VersionHelper#VERSION_LENGTH} digits.
     */
    public static boolean isVersionLengthValid(String version){
        return version.split(VersionHelper.VERSION_SPLITTER).length >= VersionHelper.VERSION_LENGTH;
    }

    /**
     * Return the input version as a String array that contains {@link VersionHelper#VERSION_LENGTH} elements.
     * @return the input version as a String array hat contains {@link VersionHelper#VERSION_LENGTH} elements.
     */
    static String[] convertVersionToStringArray(String version){
        return version.split(VersionHelper.VERSION_SPLITTER);
    }

    /**
     * Return the current version as an integer array that contains {@link VersionHelper#VERSION_LENGTH} elements.
     * @return the current version as an integer array hat contains {@link VersionHelper#VERSION_LENGTH} elements.
     */
    public int[] getVersionAsIntArray(){
        return new int[]{
                this.getMainVersionAsNumber(),
                this.getMajorVersionAsNumber(),
                this.getMinorVersionAsNumber()};
    }

    /**
     * Return the current version as an integer array that contains {@link VersionHelper#VERSION_LENGTH} elements.
     * @return the current version as an integer array hat contains {@link VersionHelper#VERSION_LENGTH} elements.
     */
    public String[] convertVersionToStringArray(){
        return new String[]{
                this.main,
                this.major,
                this.minor};
    }

    public enum Feature {
        HEADSET_STATUS,
        OAD_WITHOUT_CONNECTION_PRIORITY,
        BLE_BONDING,
        A2DP_FROM_HEADSET,
        REGISTER_EXTERNAL_NAME,
        RMS,
        SNR
    }


}
