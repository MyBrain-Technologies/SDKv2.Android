package mbtsdk.com.mybraintech.sdkv2;

import android.app.Application;

import engine.MbtClient;
import utils.LogUtils;


public class TestApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MbtClient.init(getApplicationContext());
    }
}
