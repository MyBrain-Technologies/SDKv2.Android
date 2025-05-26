package com.mybraintech.android.jnibrainbox;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class RelaxIndexSessionOutputData {
    public float mean_alpha_power;
    public float mean_relative_alpha_power;
    public float confidence;

    /**
     * volume
     */
    public float[] volums;

    public float[] alpha_powers;
    public float[] relative_alpha_powers;
    public float[] qualities;
    public float[] past_relax_index;
    public float[] smoothed_relax_index;

    @NonNull
    @Override
    public String toString() {
        return "RelaxIndexSessionOutputData{" +
                "mean_alpha_power=" + mean_alpha_power +
                ", mean_relative_alpha_power=" + mean_relative_alpha_power +
                ", confidence=" + confidence +
                ", volumes=" + Arrays.toString(volums) +
                ", alpha_powers=" + Arrays.toString(alpha_powers) +
                ", relative_alpha_powers=" + Arrays.toString(relative_alpha_powers) +
                ", qualities=" + Arrays.toString(qualities) +
                ", past_relax_index=" + Arrays.toString(past_relax_index) +
                ", smoothed_relax_index=" + Arrays.toString(smoothed_relax_index) +
                '}';
    }
}
