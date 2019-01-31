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

    private transient static HashMap<String, String> initialQRDataBase = initQRDataBase();

    public transient static final String QR_PREFIX = "MM";
    public transient static final int QR_LENGTH = 10;


    private static HashMap<String, String> initQRDataBase(){
        HashMap<String, String> hashMap = new HashMap<>(6);
        //Livraison Carnac début janvier
        hashMap.put("MM10001551", "melo_1010100012");
        hashMap.put("MM10001552", "melo_1010101487");
        hashMap.put("MM10001553", "melo_1010100036");
        hashMap.put("MM10001554", "melo_1010100067");
        hashMap.put("MM10001555", "melo_1010101486");
        hashMap.put("MM10001560", "melo_1010100004");
        hashMap.put("MM10001651", "melo_1010100016");
        hashMap.put("MM10001559", "melo_1010101427");
        //Livraison Carnac début février
        hashMap.put("MM10001284", "melo_1010101010");
        hashMap.put("MM10001281", "melo_1010101101");
        hashMap.put("MM10001289", "melo_1010100957");
        hashMap.put("MM10001288", "melo_1010100956");
        hashMap.put("MM10001282", "melo_1010100954");

        //Livraison Enedis début mars

        //colis 1
        hashMap.put("MM10001535", "melo_1010100178");
        hashMap.put("MM10001536", "melo_1010101017");
        hashMap.put("MM10001537", "melo_1010100919");
        hashMap.put("MM10001538", "melo_1010100207");
        hashMap.put("MM10001539", "melo_1010100142");
        hashMap.put("MM10001540", "melo_1010100242");

        //colis 2
        hashMap.put("MM10001531", "melo_1010101046");
        hashMap.put("MM10001532", "melo_1010101092");
        hashMap.put("MM10001533", "melo_1010101093");
        hashMap.put("MM10001534", "melo_1010101178");
        hashMap.put("MM10001571", "melo_1010100146");
        hashMap.put("MM10001574", "melo_1010101036");

        //colis 3
        hashMap.put("MM10001572", "melo_1010100974");
        hashMap.put("MM10001573", "melo_1010101070");
        hashMap.put("MM10001575", "melo_1010100169");
        hashMap.put("MM10001576", "melo_1010101052");
        hashMap.put("MM10001577", "melo_1010100232");
        hashMap.put("MM10001578", "melo_1010101080");

        //colis 4
        hashMap.put("MM10001569", "melo_1010100170");
        hashMap.put("MM10001570", "melo_1010101182");
        hashMap.put("MM10001579", "melo_1010100148");
        hashMap.put("MM10001580", "melo_1010100197");
        hashMap.put("MM10001286", "melo_1010101091");
        hashMap.put("MM10001290", "melo_1010100984");

        //Gefco
        hashMap.put("MM10001297", "melo_1010101021");
        hashMap.put("MM10001229", "melo_1010100976");
        hashMap.put("MM10001236", "melo_1010100153");
        hashMap.put("MM10001239", "melo_1010100932");
        hashMap.put("MM10001521", "melo_1010101028");
        hashMap.put("MM10000972", "melo_1010101042");
        hashMap.put("MM10000903", "melo_1010101020");
        hashMap.put("MM10001588", "melo_1010101023");
        hashMap.put("MM10000904", "melo_1010100162");
        hashMap.put("MM10001524", "melo_1010100175");
        hashMap.put("MM10000424", "melo_1010101076");
        hashMap.put("MM10001295", "melo_1010100201");
        hashMap.put("MM10001298", "melo_1010100966");
        hashMap.put("MM10001296", "melo_1010100185");
        hashMap.put("MM10000973", "melo_1010100168");
        hashMap.put("MM10000971", "melo_1010100199");
        hashMap.put("MM10000426", "melo_1010100134");

        return hashMap;
    }

    public MelomindsQRDataBase(Context context, boolean initFromFile, boolean setQRAsKey){
        if(!initFromFile)
            loadFromPreferences(context);
        else
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

    private void loadFromPreferences(Context context){
        Log.d(TAG, "retrieving qrcode map from preferences");
        SharedPreferences mPrefs = context.getSharedPreferences("melomind_qr_coupling",MODE_PRIVATE);
        Gson gson = new Gson();
        String json = mPrefs.getString("qr_couples", null);
        if(json != null){
            HashMap tempBase = gson.fromJson(json, HashMap.class);
            if(tempBase != null && tempBase.size() >= initialQRDataBase.size())
                this.putAll(tempBase);
            else{
                //Init database with static values for ces and carnac
                this.putAll(initialQRDataBase);
                saveToPreferences(context);
            }
        }
        else{
            //Init database with static values for ces and carnac
            this.putAll(initialQRDataBase);
            saveToPreferences(context);
        }
    }

    private void initFromFile(Context context, boolean setQRAsKey){

        Log.i(TAG, "reading qrcode file");

        String line = null;
        InputStream filepath;
        BufferedReader reader;
        try {
            filepath = context.getAssets().open("qrcodes_serial.csv");
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