package core.device.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;

import java.util.concurrent.ConcurrentHashMap;


import features.MbtFeatures;

import static android.content.Context.MODE_PRIVATE;


public class MelomindsQRDataBase extends ConcurrentHashMap<String, String> {
    private transient final static String TAG = MelomindsQRDataBase.class.getSimpleName();

    public transient static final String QR_PREFIX = "MM";
    public transient static final String QR_SUFFIX = ".";
    public transient static final int QR_LENGTH = MbtFeatures.DEVICE_QR_CODE_LENGTH;

    public MelomindsQRDataBase(Context context, boolean setQRAsKey){

    }

    public void storeNewCouple(Context context, Pair<String, String> couple){
        this.put(couple.first, couple.second);
        saveToCache(context);
    }

    private void saveToCache(Context context){
        Log.d(TAG, "store qrcode map to preferences");
        SharedPreferences mPrefs = context.getSharedPreferences("melomind_qr_coupling",MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(this);
        prefsEditor.clear();
        prefsEditor.putString("qr_couples", json);
        prefsEditor.apply();
    }

    @Override
    public String get(Object key) {
        String value = null;
        if(key instanceof String){

            if(((String)key).contains(MbtFeatures.A2DP_DEVICE_NAME_PREFIX)) //audio_ prefix is removed in the key
                key = ((String) key).replace(MbtFeatures.A2DP_DEVICE_NAME_PREFIX,"");

            if(((String)key).contains(MbtFeatures.A2DP_DEVICE_NAME_PREFIX_LEGACY)) //melo_ prefix is removed in the key
                key = ((String) key).replace(MbtFeatures.A2DP_DEVICE_NAME_PREFIX_LEGACY,"");

            if(((String)key).contains(QR_PREFIX) && ((String)key).length() == (MbtFeatures.DEVICE_QR_CODE_LENGTH)) // a dot is added if 1 digit is missing in the QR code as the key
                key = ((String) key).concat(QR_SUFFIX);

            value = super.get(key);
            if(value != null && ((String) key).contains(QR_PREFIX) && !value.contains(QR_PREFIX)) //melo_ prefix is added in the value (for the BLE name)
                value = MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX + value;
        }
        Log.d(TAG, "Key/value pair is ["+key+" ; "+value+"]");
        return value;
    }

}