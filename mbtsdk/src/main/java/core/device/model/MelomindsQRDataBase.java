package core.device.model;

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


import static android.content.Context.MODE_PRIVATE;


public class MelomindsQRDataBase extends ConcurrentHashMap<String, String> {
    private transient final static String TAG = MelomindsQRDataBase.class.getSimpleName();

    public transient static final String QR_PREFIX = "MM";
    public transient static final int QR_LENGTH = 10;
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
        return super.get(key);
    }
}