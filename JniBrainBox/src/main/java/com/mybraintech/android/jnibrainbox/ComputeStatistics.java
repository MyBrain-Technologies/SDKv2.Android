package com.mybraintech.android.jnibrainbox;


import android.util.Log;

import java.util.HashMap;
import org.apache.commons.lang.ArrayUtils;

/**
 * Created by Etienne on 12/04/2017.
 */

public class ComputeStatistics {
  static {
    System.loadLibrary("mbt_statistic");
  }
  private static final String TAG = ComputeStatistics.class.getName();


  public  HashMap<String, Float> computeStatisticsSNR(final float threshold, final Float[] snrValues) {

    Log.d(TAG, "Dev_Debug SDKV3 start to call computeStatisticsSNR: ");
    HashMap<String, Float> result = new HashMap<>();
    long timeBefore = System.currentTimeMillis();
    result = nativeComputeStatisticsSNR(threshold, snrValues.length, ArrayUtils.toPrimitive(snrValues));
    long timeAfter = System.currentTimeMillis();
    Log.d(TAG, "Dev_Debug SDKV3 Statistics computation time : " + (timeAfter - timeBefore));
    return result;
  }


  private native static HashMap<String, Float> nativeComputeStatisticsSNR(final float threshold, final int size, final float[] snrValues);

}
