package core.oad;

/**
 * Created by Etienne on 14/10/2016.
 */

public enum OADEvent {
    /**
     * Corresponds to a call to firmware version and length check from fw.
     */
    FW_VERSION_LENGTH_CHECK,


    /**
     * Corresponds to the completion progress of the oad.
     */
    PROGRESS,


    /**
     * Corresponds to the wait till fw computes crc32 and sends back completion code.
     */
    CRC_COMPUTING,


    /**
     * Corresponds to the last checking value, whether oad has completed or not.
     */
    OAD_COMPLETION
}
