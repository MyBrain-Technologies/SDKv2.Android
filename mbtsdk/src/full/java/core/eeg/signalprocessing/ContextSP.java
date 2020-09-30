package core.eeg.signalprocessing;


import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import utils.VersionHelper;

/**
 * ContextSP contains some static values related to the signal processing
 *
 * @author Vincent on 30/03/2016.
 */
@Keep
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

    public final static int smoothingDuration = 4;

    public static int HISTORY_SIZE = 1;

    /**
     * The neurofeedback algorithm used to be based on SNR on versions lower than 2.4.0 and is based on RMS now
     */

    public static RelaxIndexComputationType getRelaxIndexComputationType(String version){
        return new VersionHelper(version).isValidForFeature(VersionHelper.Feature.RMS)? RelaxIndexComputationType.RMS : RelaxIndexComputationType.SNR;
    }

    public enum RelaxIndexComputationType{

        /**
         * Signal to Noise ratio
         */
        SNR,

        /**
         * Root mean squared value
         */
        RMS
    }
}
