package core.eeg.signalprocessing;


import android.support.annotation.NonNull;

/**
 * ContextSP contains some static values related to the signal processing
 *
 * @author Vincent on 30/03/2016.
 */

public final class ContextSP {
    /**
     * The current set of calibration parameters
     */
    public static MBTCalibrationParameters calibrationParameters;
    /**
     * The current signal processing version. This value is updated as soon as the library is correctly loaded
     */
    @NonNull
    public static String SP_VERSION = "0.0.0";

    /**
     * The prefix used to load the library. The suffix is the SP_VERSION we wish to load. This value is
     * defined by @{@link mbtsdk.com.mybraintech.mbtsdk.BuildConfig} USE_ALGO_VERSION final field
     */
    public final static String LIBRARY_NAME = "mbtalgo_";

    public final static int smoothingDuration = 2;

}
