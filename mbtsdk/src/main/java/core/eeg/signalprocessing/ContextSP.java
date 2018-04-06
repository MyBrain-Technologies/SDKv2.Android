package core.eeg.signalprocessing;

/**
 * Created by Vincent on 30/03/2016.
 */
public class ContextSP {
    public static MBTCalibrationParameters calibrationParameters;
    public static String SP_VERSION = "";

    /**
     * The prefix used to load the library. The suffix is the SP_VERSION we wish to load. This value is
     * defined by @{@link mbtsdk.com.mybraintech.mbtsdk.BuildConfig} USE_ALGO_VERSION final field
     */
    public final static String LIBRARY_NAME = "mbtalgo_";
}
