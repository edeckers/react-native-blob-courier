/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import com.facebook.react.bridge.ReactApplicationContext
import java.util.Date

class CongestionAvoidingProgressNotifier(
  private val context: ReactApplicationContext,
  private val taskId: String,
  private val totalNumberOfBytes: Long,
  private val progressInterval: Int
) {
  var lastProgressUpdate: Date = Date()

  fun notify(numberOfBytesUntilNow: Long) {
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
