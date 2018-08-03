package core.eeg.signalprocessing;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class MBTCalibrationParametersTest {

    @Test (expected = IllegalArgumentException.class)
    public void ParamsNullTest() {
        HashMap<String, float[]> params = null; //check that IllegalArgumentException is raised if params is null
        MBTCalibrationParameters calibrationParameters = new MBTCalibrationParameters(params);
    }

    @Test (expected = IllegalArgumentException.class)
    public void ParamsEmptyTest() {
        HashMap<String, float[]> params = new HashMap<>(); //check that IllegalArgumentException is raised if params is empty / size = 0
        MBTCalibrationParameters calibrationParameters = new MBTCalibrationParameters(params);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getKeyNegativeIndexTest() {
        int index = -1;
        HashMap<String, float[]> params = new HashMap<>(); //check that IllegalArgumentException is raised if index is negative
        params.put("a",new float[]{1f});
        MBTCalibrationParameters calibrationParameters = new MBTCalibrationParameters(params);
        calibrationParameters.getKey(index);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getKeyBigIndexTest() {
        int index = 2; //check that IllegalArgumentException is raised if index is bigger than params.size()
        HashMap<String, float[]> params = new HashMap<>();
        params.put("a",new float[]{1f}); //params size = 1 => index > params.size => should raise exception
        MBTCalibrationParameters calibrationParameters = new MBTCalibrationParameters(params);
        calibrationParameters.getKey(index);
    }

    @Test
    public void getKeyGoodIndexTest() {
        int index = 1;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("a",new float[]{1f});
        params.put("b",new float[]{2f});
        params.put("c",new float[]{3f}); //index < params.size => we can access to params[index] without any exception
        MBTCalibrationParameters calibrationParameters = new MBTCalibrationParameters(params);
        String key = calibrationParameters.getKey(index);
        assertNotNull(key);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getValueNegativeIndexTest() {
        int index = -1; //check that IllegalArgumentException is raised if index is negative
        HashMap<String, float[]> params = new HashMap<>();
        params.put("a",new float[]{1f});
        MBTCalibrationParameters calibrationParameters = new MBTCalibrationParameters(params);
        calibrationParameters.getValue(index);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getValueBigIndexTest() {
        int index = 2; //check that IllegalArgumentException is raised if index is bigger than params.size
        HashMap<String, float[]> params = new HashMap<>();
        params.put("a",new float[]{1f}); //params size = 1 => index > params.size =>  should raise exception
        MBTCalibrationParameters calibrationParameters = new MBTCalibrationParameters(params);
        calibrationParameters.getValue(index);
    }

    @Test
    public void getValueGoodIndexTest() {
        int index = 2;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("a",new float[]{1f});
        params.put("b",new float[]{2f});
        params.put("c",new float[]{3f}); //index < params.size => we can access to params[index] without any exception
        MBTCalibrationParameters calibrationParameters = new MBTCalibrationParameters(params);
        float[] value = calibrationParameters.getValue(index);
        assertNotNull(value);
    }
}