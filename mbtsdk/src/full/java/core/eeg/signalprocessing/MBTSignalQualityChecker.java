package core.eeg.signalprocessing;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;

/**
 * MBTSignalQualityChecker contains methods for computing the EEG signal quality
 *
 * @author Vincent on 26/08/2015.
 */
public final class MBTSignalQualityChecker {

    /**
     * Initializes the MBT_MainQC object in the JNI which will live throughout all session.
     * Should be destroyed at the end of the session
     */
    @NonNull
    public static String initQualityChecker(){
        return nativeInitQualityChecker();
    }


    /**
     * Destroy the MBT_MainQc object in the JNI at the end of the session.
     */
    public static void deinitQualityChecker(){
        nativeDeinitQualityChecker();
    }

    /**
     * Computes the quality for each provided channels
     * @param samprate the number of value(s) inside each channel
     * @param packetLength how long is a packet (time x samprate)
     * @param channels the channel(s) to be computed
     * @return the qualities for each provided channels
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    @NonNull
    public static double[] computeQualitiesForPacket(final int samprate, final int packetLength,
                                                     @Nullable final double[]... channels ) {

    if (samprate < 0)
            throw new IllegalArgumentException("samprate MUST BE POSITIVE!");
    if (packetLength < 0)
            throw new IllegalArgumentException("packetLength MUST BE POSITIVE!");
    if (channels == null || channels.length == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE channel(s) !");

    final int matrixHeight = channels.length;

    // Creating 2d Matrix-like array
    final double[][] matrix = new double[matrixHeight][samprate];
    for (int it = 0 ; it < matrixHeight ; it++){
        final double[] current = channels[it];
        for (int it2 = 0; it2 < samprate; it2++)
            matrix[it][it2] = current[it2];
    }
    return nativeComputeQualities(matrix, samprate, packetLength);
    }

    /**
     * Computes the quality for each provided channels
     * @param samprate the number of value(s) inside each channel
     * @param packetLength how long is a packet (time x samprate)
     * @param channels the channel(s) to be computed
     * @return the qualities for each provided channels
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    @NonNull
    public static float[] computeQualitiesForPacketNew(final int samprate, final int packetLength,
                                                       @Nullable final ArrayList<ArrayList<Float>> channels ) {

        if (samprate < 0)
            throw new IllegalArgumentException("samprate MUST BE POSITIVE!");
        if (packetLength < 0)
            throw new IllegalArgumentException("packetLength MUST BE POSITIVE!");
        if (channels == null || channels.size() == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE channel(s) !");

        final int matrixHeight = channels.size();

        // Creating 2d Matrix-like array
        return nativeComputeQualityCheckerNew(MBTSignalProcessingUtils.channelsToMatrixFloat(channels), samprate, packetLength);
    }



    @NonNull
    public static Float[][] getModifiedInputData(){
        float [][] modifiedData = nativeGetModifiedInputData();
        Float[][] data = new Float[modifiedData.length][];
        for (int i = 0; i<modifiedData.length; i++){
            Float[] temp = ArrayUtils.toObject(modifiedData[i]);
            data[i] = temp;
        }
        return data;
    }


    @NonNull
    private native static String nativeInitQualityChecker();

    private native static void nativeDeinitQualityChecker();

    @NonNull
    private native static float[][] nativeGetModifiedInputData();


    @NonNull
    private native static double[] nativeComputeQualities(double[][] matrix, int samprate, int packetLength);

    @NonNull
    public native static float[] nativeComputeQualityCheckerNew(float[][] matrix, int samprate, int packetLength);


}
