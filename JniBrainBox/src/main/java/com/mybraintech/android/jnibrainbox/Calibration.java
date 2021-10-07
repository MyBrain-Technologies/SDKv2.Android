package com.mybraintech.android.jnibrainbox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class Calibration implements AutoCloseable  {
    @NonNull
    private native void new_calibration(int simpling_rate, int sliding_windows_sec);

    @NonNull
    private native void destroy_calibration();

    @NonNull
    private native int compute_calibration(float[][] signals, float[][] qualities);

    @NonNull
    private native float[] get_rms();

    @NonNull
    private native float[] get_relative_rms();

    @NonNull
    private native float[] get_smoothed_rms();

    @NonNull
    private native float[] get_hist_freq();

    @NonNull
    private native float[] get_iaf();

    /**
     * @note DO NOT REMOVE OR MODIFY jniObjectPointer VARIABLE:
     * it is used for mapping java instance of this class and cpp related object.
     */
    @SuppressWarnings("unused")
    private long jniObjectPointer;

    static {
        System.loadLibrary("jni_brainboxsdk");
    }

    /**
     * for the model [sampling_rate = 250Hz, 2 channels], the sliding window sec is 8 seconds.
     * @param sampling_rate
     * @param sliding_windows_sec
     * @throws Exception
     */
    public Calibration(int sampling_rate, int sliding_windows_sec) throws Exception {
         new_calibration(sampling_rate, sliding_windows_sec);
    }

    /**
     *
     * @param signals the signal length of each channel must be bigger or equal the sliding window size * sampling rate.
     *                eg: sliding window size = 8, sampling rate = 250Hz -> signal length (each channel) must be >= 250*8=2000
     * @param qualities
     * @return error code, 0 means no error
     * @throws Exception
     */
    public int computeCalibration(@Nullable final float[][] signals ,
                                  @Nullable final float[][] qualities ) throws Exception {
        int error = compute_calibration(signals, qualities);
        return error;
    }

    @Override
    public void close() throws Exception {
        destroy_calibration();
    }

    public float[] GetRms() {
        return get_rms();
    }

    public float[] GetRelativeRMS() {
        return get_relative_rms();
    }

    public float[] GetHistFreq() {
        return get_hist_freq();
    }

    public float[] GetIAF() {
        return get_iaf();
    }
}
