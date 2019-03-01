package utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import java.util.ArrayList;

public class BroadcastUtils {

    private static boolean isReceiverRegistered = false;
    /**
     * Register receiver for a list of actions
     */
    public static void registerReceiverIntents(Context context, BroadcastReceiver receiver, String... actions){
        if(!isReceiverRegistered){
            for (String action : actions){
                IntentFilter intentFilter = new IntentFilter(action);
                context.registerReceiver(receiver, intentFilter);
                isReceiverRegistered = true;
            }
        }
    }

    public static void unregisterReceiver(Context context, BroadcastReceiver broadcastReceiver) {
        try {
            if (isReceiverRegistered && context != null) {
                context.unregisterReceiver(broadcastReceiver);
                isReceiverRegistered = false;
            }
        }catch (IllegalArgumentException e){
            LogUtils.w("BroadcastUtils", "Unregistering failed : the receiver was not registered.");
        }
    }
}
