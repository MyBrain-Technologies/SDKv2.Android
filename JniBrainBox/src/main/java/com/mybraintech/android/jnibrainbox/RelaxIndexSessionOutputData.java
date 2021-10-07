package com.mybraintech.android.jnibrainbox;

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
}
