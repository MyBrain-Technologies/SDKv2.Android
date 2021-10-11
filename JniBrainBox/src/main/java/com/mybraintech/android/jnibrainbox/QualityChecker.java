package com.mybraintech.android.jnibrainbox;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;

public class QualityChecker implements AutoCloseable  {
    @NonNull
    private native void new_quality_checker(int sampleRate);

    @NonNull
    private native void destroy_quality_checker();

    @NonNull
    private native float[] compute_quality_checker(float[][] matrix);
    /**
     * @note DO NOT REMOVE OR MODIFY jniObjectPointer VARIABLE:
     * it is used for mapping java instance of this class and cpp related object.
     */
    @SuppressWarnings("unused")
    private long jniObjectPointer;
    @SuppressWarnings("unused")
    public char jniError;

    static {
        System.loadLibrary("jni_brainboxsdk");
    }

    public QualityChecker(int sampleRate) throws Exception {
         new_quality_checker(sampleRate);
    }
    /**
     * Computes the quality for each provided channels
     * @param channels the channel(s) to be computed
     * @return the qualities for each provided channels
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public float[] computeQualityChecker(@Nullable final ArrayList<ArrayList<Float>> channels ) throws Exception {
        if (channels == null || channels.size() == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE channel(s) !");

        float[] res = compute_quality_checker(TypeConverter.channelsToMatrixFloat(channels));
        return res;
    }

    @Override
    public void close() throws Exception {
        destroy_quality_checker();
    }
}
