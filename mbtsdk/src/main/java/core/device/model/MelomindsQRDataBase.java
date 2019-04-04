package core.device.model;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


import features.MbtFeatures;

import static android.content.Context.MODE_PRIVATE;


public class MelomindsQRDataBase extends ConcurrentHashMap<String, String> {
    private transient final static String TAG = MelomindsQRDataBase.class.getSimpleName();

    public transient static final String QR_PREFIX = "MM";
    public transient static final String QR_SUFFIX = ".";
    public transient static final int QR_LENGTH = MbtFeatures.DEVICE_QR_CODE_LENGTH;
    private static final String QR_CODE_FILE = "qrcodes_serial.csv";

    public MelomindsQRDataBase(Context context, boolean setQRAsKey){
        initFromFile(context, setQRAsKey);

    }

    public void storeNewCouple(Context context, Pair<String, String> couple){
        this.put(couple.first, couple.second);
        saveToPreferences(context);
    }

    private void saveToPreferences(Context context){
        Log.d(TAG, "store qrcode map to preferences");
        SharedPreferences mPrefs = context.getSharedPreferences("melomind_qr_coupling",MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(this);
        prefsEditor.clear();
        prefsEditor.putString("qr_couples", json);
        prefsEditor.apply();
    }

    private void initFromFile(Context context, boolean setQRAsKey){

        Log.i(TAG, "reading qrcode file");

        String line = null;
        InputStream filepath;
        BufferedReader reader;
        try {
            filepath = context.getAssets().open(QR_CODE_FILE);
            reader = new BufferedReader(new InputStreamReader(filepath));
            while ((line = reader.readLine()) != null) {
                String[] s = line.split(",");
                if(s.length >= 2){
                    if(setQRAsKey)
                        this.put(s[0], s[1]);
                    else
                        this.put(s[1] ,s[0]);
                }
            }
            filepath.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String get(Object key) {
        String value = null;
        if(key instanceof String){

            if(((String)key).contains(MbtFeatures.A2DP_DEVICE_NAME_PREFIX)) //audio_ prefix is removed in the key
                key = ((String) key).replace(MbtFeatures.A2DP_DEVICE_NAME_PREFIX,"");

            if(((String)key).contains(MbtFeatures.A2DP_DEVICE_NAME_PREFIX_LEGACY)) //melo_ prefix is removed in the key
                key = ((String) key).replace(MbtFeatures.A2DP_DEVICE_NAME_PREFIX_LEGACY,"");

            if(((String)key).contains(QR_PREFIX) && ((String)key).length() == (MbtFeatures.DEVICE_QR_CODE_LENGTH-1)) // a dot is added if 1 digit is missing in the QR code as the key
                key = ((String) key).concat(QR_SUFFIX);

            value = super.get(key);
            if(value != null && ((String) key).contains(QR_PREFIX) && !value.contains(QR_PREFIX)) //melo_ prefix is added in the value (for the BLE name)
                value = MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX + value;
        }
        Log.d(TAG, "Key/value pair is ["+key+" ; "+value+"]");
        return value;
    }

}