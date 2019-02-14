package utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import java.util.ArrayList;

public class BroadcastUtils {

    private static boolean isReceiverRegistered = false;
    /**
     * Register receiver for a list of actions
     * @param context
     * @param actions
     */
    public static void registerReceiverIntents(Context context, ArrayList<String> actions, BroadcastReceiver receiver){
        for (String action : actions){
            IntentFilter intentFilter = new IntentFilter(action);
            context.registerReceiver(receiver, intentFilter);
            isReceiverRegistered = true;
        }
    }

    public static void unregisterReceiver(Context context, BroadcastReceiver broadcastReceiver){
        if(isReceiverRegistered){
            context.unregisterReceiver(broadcastReceiver);
            isReceiverRegistered = false;
        }
    }
}
