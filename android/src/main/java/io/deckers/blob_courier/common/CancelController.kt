package io.deckers.blob_courier.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import okhttp3.Call

private const val TAG = "CancelController"

private val logger = Logger(TAG)
private fun lv(m: String, e: Throwable? = null) = logger.v(m, e)

fun registerCancellationHandler(context: Context, taskId: String, call: Call) {
  lv("Registering $ACTION_CANCEL_REQUEST receiver")

  LocalBroadcastManager.getInstance(context)
    .registerReceiver(object : BroadcastReceiver() {
      override fun onReceive(p0: Context?, intent: Intent?) {
        if (intent?.getStringExtra("taskId") != taskId) {
          return
        }

        call.cancel()
      }
    }, IntentFilter(ACTION_CANCEL_REQUEST))

  lv("Registered $ACTION_CANCEL_REQUEST receiver")
}
