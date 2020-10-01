package utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import java.util.*

object BroadcastUtils {
  private val broadcastReceivers: MutableList<BroadcastReceiver> = ArrayList()

  /**
   * Register receiver for a list of actions
   */
  fun registerReceiverIntents(context: Context, receiver: BroadcastReceiver, vararg actions: String) {
    if (!broadcastReceivers.contains(receiver)) {
      broadcastReceivers.add(receiver)
      for (action in actions) {
        val intentFilter = IntentFilter(action)
        context.registerReceiver(receiver, intentFilter)
      }
    }
  }

  fun unregisterReceiver(context: Context, receiver: BroadcastReceiver) {
    try {
      if (broadcastReceivers.contains(receiver)) {
        broadcastReceivers.remove(receiver)
        context.unregisterReceiver(receiver)
      }
    } catch (ignored: IllegalArgumentException) {
    }
  }
}