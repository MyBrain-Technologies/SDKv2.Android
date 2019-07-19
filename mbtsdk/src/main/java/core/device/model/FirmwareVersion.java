package core.device.model;

import android.support.annotation.NonNull;

import utils.LogUtils;
import utils.VersionHelper;

/**
 * Firmware version model that stores all the digits of the firmware version
 * in order to facilitate the firmware version manipulation.
 */
public class FirmwareVersion {

    public final static String TAG = FirmwareVersion.class.getSimpleName();

    /**
     * Wrapper used to store the firmware version divided into three parts: main major and minor parts.
     * A valid firmware version format is X.Y.Z where
     * X = main part
     * Y = major part
     * Z = minor part
     * Each Xs Ys and Zs are integer numbers.
     */
    private VersionHelper firmwareVersionHelper;

    private String firmwareVersionAsString;

    /**
     * Constructor that build a {@link FirmwareVersion} instance
     * from a String type firmware version.
     * @param firmwareVersion is the String type firmware version to convert into a {@link FirmwareVersion} instance.
     */
    public FirmwareVersion(@NonNull String firmwareVersion) {
        if (!VersionHelper.isVersionLengthValid(firmwareVersion)){
            LogUtils.e(TAG, "Invalid version length : minimum number of digits is equal to " + VersionHelper.VERSION_LENGTH);
            return;
        }
        this.firmwareVersionHelper = new VersionHelper(firmwareVersion);
        this.firmwareVersionAsString = firmwareVersion;
    }

    /**
     * Return the version as a String.
     * @return the version as a String
     */
    public String getFirmwareVersionAsString(){
        return firmwareVersionAsString;
    }

    /**
     * Return true if the current firmware version is equal to the input firmware version, false otherwise.
     * @return true if the current firmware version is equal to the input firmware version, false otherwise.
     */
    public boolean compareFirmwareVersion(FirmwareVersion firmwareVersion){
        return this.equals(firmwareVersion);
    }


}
