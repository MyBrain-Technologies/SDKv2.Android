package core.device.model;

import android.support.annotation.NonNull;

import utils.VersionHelper;

/**
 * Firmware version model that stores all the digits of the firmware version
 * in order to facilitate the firmware version manipulation.
 */
public class FirmwareVersion {

    /**
     * Wrapper used to store the firmware version divided into three parts: main major and minor parts.
     * A valid firmware version format is X.Y.Z where
     * X = main part
     * Y = major part
     * Z = minor part
     * Each Xs Ys and Zs are integer numbers.
     */
    private final VersionHelper firmwareVersionHelper;

    /**
     * Constructor that build a {@link FirmwareVersion} instance
     * from a String type firmware version.
     * @param firmwareVersion is the String type firmware version to convert into a {@link FirmwareVersion} instance.
     */
    public FirmwareVersion(@NonNull String firmwareVersion) {
        this.firmwareVersionHelper = new VersionHelper(firmwareVersion);
    }

    /**
     * Return the Firmware version as a String.
     * @return the Firmware version as a String
     */
    public String getFirmwareVersionAsString(){
        return this.firmwareVersionHelper.getMainVersionAsString() +
                this.firmwareVersionHelper.getMajorVersionAsString() +
                this.firmwareVersionHelper.getMinorVersionAsString();
    }

    /**
     * Return the firmware version as an integer array that contains {@link VersionHelper#VERSION_LENGTH} elements.
     * @return the firmware version as an integer array hat contains {@link VersionHelper#VERSION_LENGTH} elements.
     */
    public int[] getFirmwareVersionAsIntArray(){
        return new int[]{
                this.firmwareVersionHelper.getMainVersionAsNumber(),
                this.firmwareVersionHelper.getMajorVersionAsNumber(),
                this.firmwareVersionHelper.getMinorVersionAsNumber()
        };

    }
}
