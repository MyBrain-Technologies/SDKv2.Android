package core.eeg.signalprocessing;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * MBTCalibrationParameters contains all parameters linked to the calibration
 *
 * @author Vincent on 26/11/2015.
 */
@Keep
public final class MBTCalibrationParameters {
    private final HashMap<String, float[]> params;
    private ArrayList<float[]> valuesAsList;

    public MBTCalibrationParameters(final HashMap<String, float[]> params){
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


    public boolean isValidCalibration() {
        if (params == null){
            return false;
        }
        if (ContextSP.SP_VERSION.equals("2.1.0"))
            return params.get("BestChannel").length > 0 && params.get("BestChannel")[0] >= 0f;

        return params.get("errorMsg").length > 0 && params.get("errorMsg")[0] >= 0f;

    }
}
