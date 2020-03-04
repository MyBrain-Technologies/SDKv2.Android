package core.device.model;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import java.io.Serializable;

import utils.LogUtils;
import utils.VersionHelper;

/**
 * Firmware version model that stores all the digits of the version
 * in order to facilitate the version manipulation.
 */
@Keep
public class MbtVersion implements Serializable {

    public final static String TAG = MbtVersion.class.getSimpleName();

    private String versionAsString;

    /**
     * Constructor that build a {@link MbtVersion} instance
     * from a String type version.
     * Default splitter used is a dot
     * @param version is the String type version to convert into a {@link MbtVersion} instance.
     */
    public MbtVersion(@NonNull String version) {
        if (!VersionHelper.isVersionLengthValid(version)){
            LogUtils.e(TAG, "Invalid version length : minimum number of digits is equal to " + VersionHelper.VERSION_LENGTH);
            return;
        }
        this.versionAsString = version;
    }

    /**
     * Constructor that build a {@link MbtVersion} instance
     * from a String type version.
     * @param version is the String type version to convert into a {@link MbtVersion} instance.
     */
    public MbtVersion(@NonNull String version, String splitter) {
        if (!VersionHelper.isVersionLengthValid(version, splitter)){
            LogUtils.e(TAG, "Invalid version length : minimum number of digits is equal to " + VersionHelper.VERSION_LENGTH);
            return;
        }
        this.versionAsString = version;
    }

    /**
     * Return the version as a String.
     * @return the version as a String
     */
    @Override
    public String toString(){
        return versionAsString;
    }

    /**
     * Return true if the current version is equal to the input version, false otherwise.
     * @return true if the current version is equal to the input version, false otherwise.
     */
    public boolean equals(MbtVersion mbtVersion) {
        return this.toString().equals(mbtVersion.versionAsString);
    }

}
