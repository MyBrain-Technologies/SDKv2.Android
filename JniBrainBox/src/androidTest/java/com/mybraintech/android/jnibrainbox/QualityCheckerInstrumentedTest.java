package com.mybraintech.android.jnibrainbox;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class QualityCheckerInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.mybraintech.android.jnibrainbox.test", appContext.getPackageName());

        ArrayList<Float> channel1 = new ArrayList<Float>();
        channel1.add(1f);
        channel1.add(2f);
        channel1.add(3f);
        channel1.add(4f);
        channel1.add(5f);
        ArrayList<ArrayList<Float>> channels = new ArrayList<ArrayList<Float>>();
        channels.add(channel1);
        int sample_rate = 250; // Hz : frequency signal
        QualityChecker qc;

        try {
            qc = new QualityChecker(sample_rate);
        } catch (Exception e) {
            assertEquals("Unexpected Error -> ", e.getMessage());
            qc = null;
        }

        try {
            //QualityChecker qc = new QualityChecker(sample_rate);
            if (qc != null) {
                float[] q = qc.computeQualityChecker(channels);
                assertEquals(1, q.length);
                assertEquals(0.0f, q[0], 0f);
            }
        } catch (Exception e) {
            assertEquals("Unexpected Error -> ", e.getMessage());
        }
    }

    @Test
    public void useCalibrationAndRelaxIndex() {
        try {
            int sampling_rate = 250;
            int sliding_windows_sec = 8;
            int duration = 20;
            float expected_mean_alpha_power = 625.0503540039062f;

            Calibration c = new Calibration(sampling_rate, sliding_windows_sec);

            float[][] signal = new float[2][sampling_rate*duration];
            float[][] qualities = new float[2][duration];

            for (int i = 0; i < sampling_rate*duration; ++i)
                signal[0][i] = (float)Math.sin((float)i/360.0);
            for (int i = 0; i < sampling_rate*duration; ++i)
                signal[1][i] = (float)Math.cos((float)i/360.0);

            for (int i = 0; i < duration; ++i)
            {
                qualities[0][i] = 1;
                qualities[1][i] = 1;
            }

            int errorCode = c.computeCalibration(signal, qualities);

            //the computing must be done without any error, error code must be 0.
            assertEquals(0, errorCode);

            //RelativeRMS size must be equal the input signal calibration duration
            assertEquals(duration, c.GetRelativeRMS().length);

            float[] iaf = c.GetIAF();

            //iaf contains the iaf median inf and iaf median sup
            assertEquals(2, iaf.length);

            float iaf_median_inf = iaf[0];
            float iaf_median_sup = iaf[1];
            RelaxIndex ri = new RelaxIndex(sampling_rate, c.GetRelativeRMS(), iaf_median_inf, iaf_median_sup);

            float[][] real_time_signal = new float[2][sampling_rate];
            for (int i = 0; i < sampling_rate; ++i) {
                real_time_signal[0][i] = (float) Math.sin((float) i / 360.0);
                real_time_signal[1][i] = (float) Math.cos((float) i / 360.0);
            }
            float[] real_time_qualities = new float[2];
            real_time_qualities[0] = 1;
            real_time_qualities[1] = 1;

            // Simulate a session live of 3 seconds
            int session_sec = 3;
            for (int live = 0; live < session_sec; ++live) {
                float volume = ri.computeVolume(real_time_signal, real_time_qualities);
                assertEquals(0, volume, 3);
            }

            // Simulate the end of session
            RelaxIndexSessionOutputData session = ri.endSession();
            assertEquals(session_sec, session.alpha_powers.length);
            assertEquals(expected_mean_alpha_power, session.mean_alpha_power, 3);
        } catch (Exception e) {
            assertEquals("Unexpected Error -> ", e.getMessage());
        }
    }
}