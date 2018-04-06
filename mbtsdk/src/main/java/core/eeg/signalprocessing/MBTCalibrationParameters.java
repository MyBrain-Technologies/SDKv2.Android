package core.eeg.signalprocessing;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Vincent on 26/11/2015.
 */
public final class MBTCalibrationParameters {
    private final HashMap<String, float[]> params;
    ArrayList<float[]> valuesAsList;

    public MBTCalibrationParameters(final HashMap<String, float[]> params){
        if (params == null || params.size() == 0)
            throw new IllegalArgumentException("calib params MUST NOT be NULL or EMPTY");
        this.params = params;
        if(this.params != null && !this.params.isEmpty()){
            valuesAsList = new ArrayList<>();
            valuesAsList.addAll(this.params.values());
        }
    }


    public final HashMap<String, float[]> getParamsAsMap(){
        return params;
    }

    public final int getSize() { return this.params.size();}

    public final String getKey(final int index) {
        if (index < 0 || index > this.params.size())
            throw new IllegalArgumentException("invalid index");

        final String[] keys = this.params.keySet().toArray(new String[this.params.size()]);
        return keys[index];
    }

    public final float[] getValue(final int index) {
        if (index < 0 || index > this.params.size())
            throw new IllegalArgumentException("invalid index");

        return valuesAsList.get(index);
    }
}
