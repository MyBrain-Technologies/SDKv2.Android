package utils;

import androidx.annotation.Keep;
import android.util.Log;

import mbtsdk.com.mybraintech.mbtsdk.BuildConfig;

@Keep
public final class LogUtils{
    private static boolean isLoggingEnabled = BuildConfig.DEBUG;

    public static int d(String tag, String msg) {
        return isLoggingEnabled ? Log.d(tag, msg) : 0;
    }

    public static int i(String tag, String msg) {
        return isLoggingEnabled ? Log.i(tag, msg) : 0;
    }

    public static int w(String tag, String msg) {
        return isLoggingEnabled ? Log.w(tag, msg) : 0;
    }

    public static int e(String tag, String msg) {
        return isLoggingEnabled ? Log.e(tag, msg) : 0;
    }

    public static int e(String tag, Throwable throwable) {
        return isLoggingEnabled ? Log.e(tag, throwable.getMessage(), throwable) : 0;
    }

    public static void enableLogging(){
        isLoggingEnabled = true;
    }

    public static void disableLogging(){
        isLoggingEnabled = false;
    }

}
