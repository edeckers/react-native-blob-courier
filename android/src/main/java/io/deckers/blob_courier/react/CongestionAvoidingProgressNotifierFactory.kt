package io.deckers.blob_courier.react

import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.progress.ProgressNotifierFactory

class CongestionAvoidingProgressNotifierFactory(
  private val context: ReactApplicationContext,
  private val progressInterval: Int
) : ProgressNotifierFactory {
  override fun create(taskId: String) =
    CongestionAvoidingProgressNotifier(context, taskId, progressInterval)
}
