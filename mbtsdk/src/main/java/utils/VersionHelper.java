package utils;

import android.util.Log;

import androidx.annotation.Keep;

/**
 * Created by Etienne on 2/26/2018.
 * This class wraps the firmware or hardware version into three parts: main major and minor.
 * A valid version format is X.Y.Z where
 * X = main
 * Y = major
 * Z = minor
 * Each Xs Ys and Zs are numbers
 */
@Keep
public final class VersionHelper {

    public final static String TAG = VersionHelper.class.getSimpleName();

    /**
     * Regular expression used to split any firmware or hardware version given as a string .
     */
    static final String VERSION_SPLITTER = "\\.";

    /**
     * Number of digits of any firmware / hardware version given
     * as a string (split with {@link VersionHelper#VERSION_SPLITTER}
     * or as an integer array.
     */
    public static final int VERSION_LENGTH = 3;

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
        if(major == null || minor == null || main == null)
            return false;

        boolean isValid = verifyFeature(feature.main, feature.major, feature.minor);

        Log.i(TAG, "feature test for " + feature.toString() + " is " + (isValid ? "valid" : "invalid"));
        return isValid;
    }

    /**
     * Verifies if a specific feature can be enabled on the bt communication based on the firmware / hardware version.
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
     * Returns true if the version given in input contains at least {@link VersionHelper#VERSION_LENGTH} digits.
     * False otherwise.
     * @param version the version to check
     * @return true if the version given in input contains at least {@link VersionHelper#VERSION_LENGTH} digits.
     */
    public static boolean isVersionLengthValid(String version, String splitter){
        return version.split(splitter).length >= VersionHelper.VERSION_LENGTH;
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
        HEADSET_STATUS                          (1,5,13),
        OAD_WITHOUT_CONNECTION_PRIORITY         (1,5,10),
        BLE_BONDING                             (1,6,7),
        A2DP_FROM_HEADSET                       (1,6,7),
        REGISTER_EXTERNAL_NAME                  (1,7,1),
        RMS                                     (2,5,0),
        SNR                                     (1,0,0),
        FIRMWARE_BASED_ON_HARDWARE              (1,7,8),
        INDUS2                                  (1,0,0),
        INDUS3                                  (1,1,0);

        int main;
        int major;
        int minor;

        Feature(int main, int major, int minor){
            this.main = main;
            this.major = major;
            this.minor = minor;
        }
    }
}
