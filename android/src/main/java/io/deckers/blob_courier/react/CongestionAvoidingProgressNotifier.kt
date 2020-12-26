/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.react

import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.progress.ProgressNotifier
import java.util.Date

class CongestionAvoidingProgressNotifier(
  private val context: ReactApplicationContext,
  private val taskId: String,
  private val progressInterval: Int
) : ProgressNotifier {
  var lastProgressUpdate = Date()

  override fun notify(numberOfBytesUntilNow: Long, totalNumberOfBytes: Long) {
    val isDownloadFinished = numberOfBytesUntilNow == totalNumberOfBytes
    val timeoutHasPassed = (Date().time - lastProgressUpdate.time) > progressInterval
    val shouldUpdate = isDownloadFinished || timeoutHasPassed

    if (!shouldUpdate) {
      return
    }

    notifyBridgeOfProgress(context, taskId, numberOfBytesUntilNow, totalNumberOfBytes)
    lastProgressUpdate = Date()
  }
}
