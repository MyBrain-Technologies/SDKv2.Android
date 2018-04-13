package core.eeg.signalprocessing;

/**
 * Created by Vincent on 25/11/2015.
 */
final class MBTSignalProcessingUtils {
    /**
     * Merges the provided channels into a matrix (a 2D array). The height of the matrix is defined by the number
     * of provided channels and its width is defined by the samprate of each channel which must be
     * consistent.
     * In the end, each line of the matrix represents a channel in the order the channels were provided
     * @param channels the channels to transform into a matrix
     * @return the matrix if successful
     * @exception IllegalArgumentException if there are no channels to merge and if the samprate is
     * inconsistent
     */
    final static double[][] channelsToMatrixDouble(Float[]... channels) {
        if (channels == null || channels.length == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE channel(s) !");

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

    final static float[][] channelsToMatrixFloat(Float[]... channels) {
        if (channels == null || channels.length == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE channel(s) !");

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
}
