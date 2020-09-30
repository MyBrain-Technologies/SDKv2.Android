package core.eeg.signalprocessing;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * MBTSignalProcessingUtils contains methods for converting the channels into Float or Double Matrix
 *
 * @author Vincent on 25/11/2015.
 */
 @Keep
public final class MBTSignalProcessingUtils {
    /**
     * Merges the provided channels into a matrix (a 2D array). The height of the matrix is defined by the number
     * of provided channels and its width is defined by the samprate of each channel which must be consistent.
     * In the end, each line of the matrix represents a channel in the order the channels were provided
     * @param channels the channels to transform into a matrix
     * @return the matrix if successful
     * @exception IllegalArgumentException if there are no channels to merge and if the samprate is inconsistent
     */
    @NonNull
    static double[][] channelsToMatrixDouble(@Nullable Float[]... channels) {
        if (channels == null || channels.length == 0){
            throw new IllegalArgumentException("there MUST be at least ONE or MORE channel(s) !");
        }

        final int height = channels.length;
        final int samprate = channels[0].length;

        final double[][] matrix = new double[height][samprate];

        for (int it = 0; it < height; it++) {
            final Float[] current = channels[it];
            if (current.length != samprate)
                throw new IllegalArgumentException("ERRROR : samprate not consistent in all provided channels!");
            for (int it2 = 0; it2 < samprate; it2++)
                matrix[it][it2] = current[it2];
        }
        return matrix;
    }

    /**
     * Merges the provided channels into a matrix (a 2D array). The height of the matrix is defined by the number
     * of provided channels and its width is defined by the samprate of each channel which must be consistent.
     * In the end, each line of the matrix represents a channel in the order the channels were provided
     * @param channels the channels to transform into a matrix
     * @return the matrix if successful
     * @exception IllegalArgumentException if there are no channels to merge and if the samprate is inconsistent
     */
    @NonNull
    static float[][] channelsToMatrixFloat(@Nullable Float[]... channels) {
        if (channels == null || channels.length == 0) {
            throw new IllegalArgumentException("there MUST be at least ONE or MORE channel(s) !");
        }
        final int height = channels.length;
        final int samprate = channels[0].length;

        final float[][] matrix = new float[height][samprate];

        for (int it = 0; it < height; it++) {
            final Float[] current = channels[it];
            if (current.length != samprate)
                throw new IllegalArgumentException("ERRROR : samprate not consistent in all provided channels!");
            for (int it2 = 0; it2 < samprate; it2++)
                matrix[it][it2] = current[it2];
        }
        return matrix;
    }

    /**
     * Merges the provided channels into a matrix (a 2D array). The height of the matrix is defined by the number
     * of provided channels and its width is defined by the samprate of each channel which must be consistent.
     * In the end, each line of the matrix represents a channel in the order the channels were provided
     * @param channels the channels to transform into a matrix
     * @return the matrix if successful
     * @exception IllegalArgumentException if there are no channels to merge and if the samprate is inconsistent
     */
    @NonNull
    public static float[][] channelsToMatrixFloat(@Nullable ArrayList<ArrayList<Float>> channels) {
        if (channels == null || channels.size() == 0) {
            throw new IllegalArgumentException("there MUST be at least ONE or MORE channel(s) !");
        }
        final int height = channels.size();
        final int samprate = channels.get(0).size();

        final float[][] matrix = new float[height][samprate];

        for (int it = 0; it < height; it++) {
            final ArrayList<Float> current = channels.get(it);
            if (current.size() != samprate)
                throw new IllegalArgumentException("ERRROR : samprate not consistent in all provided channels!");
            for (int it2 = 0; it2 < samprate; it2++)
                matrix[it][it2] = current.get(it2);
        }
        return matrix;
    }


}
