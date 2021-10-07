package com.mybraintech.android.jnibrainbox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class RelaxIndex implements AutoCloseable  {
    private native void new_relax_index(int sampling_rate, float[] smoothed_rms, float iaf_median_inf, float iaf_median_sup);
    private native void destroy_relax_index();
    private native float compute(float[][] signals, float[] qualities);
    private native void end_session();
    private native float get_mean_alpha_power();
    private native float get_mean_relative_alpha_power();
    private native float get_confidence();
    private native float[] get_volums();
    private native float[] get_alpha_powers();
    private native float[] get_relative_alpha_powers();
    private native float[] get_qualities();
    private native float[] get_past_relax_index();
    private native float[] get_smoothed_relax_index();

    /**
     * @note DO NOT REMOVE OR MODIFY jniObjectPointer VARIABLE:
     * it is used for mapping java instance of this class and cpp related object.
     */
    private long jniObjectPointer;

    static {
        System.loadLibrary("jni_brainboxsdk");
    }

    public RelaxIndex(int sampling_rate, float[] smoothed_rms, float iaf_median_inf, float iaf_median_sup) throws Exception {
         new_relax_index(sampling_rate, smoothed_rms, iaf_median_inf, iaf_median_sup);
    }

    public float computeVolume(@Nullable final float[][] signals,
                               @Nullable final float[] qualities) throws Exception {
        return compute(signals, qualities);
    }

    @Override
    public void close() throws Exception {
        destroy_relax_index();
    }

    public RelaxIndexSessionOutputData endSession() {
        end_session();
        RelaxIndexSessionOutputData data = new RelaxIndexSessionOutputData();
        data.mean_alpha_power = get_mean_alpha_power();
        data.mean_relative_alpha_power = get_mean_relative_alpha_power();
        data.confidence = get_confidence();
        data.volums = get_volums();
        data.alpha_powers = get_alpha_powers();
        data.relative_alpha_powers = get_relative_alpha_powers();
        data.qualities = get_qualities();
        data.past_relax_index = get_past_relax_index();
        data.smoothed_relax_index = get_smoothed_relax_index();
        return data;
    }
}
