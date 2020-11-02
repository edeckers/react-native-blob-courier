/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import android.app.DownloadManager
import com.facebook.react.bridge.ReactApplicationContext
import java.util.Timer
import kotlin.concurrent.timerTask

class ManagedProgressUpdater {
  companion object {
    fun start(
      context: ReactApplicationContext,
      downloadId: Long,
      taskId: String,
      progressInterval: Long
    ) {
      val progressUpdaterInterval = Timer()

      progressUpdaterInterval.scheduleAtFixedRate(
        timerTask {
          ProgressUpdateRunner(
            context,
            createDownloadManager(context),
            downloadId,
            taskId,
            progressUpdaterInterval
          )
        },
        0, progressInterval
      )
    }

    private class ProgressUpdateRunner(
      private val context: ReactApplicationContext,
      private val dm: DownloadManager,
      private val downloadId: Long,
      private val taskId: String,
      private val progressUpdaterInterval: Timer
    ) : Runnable {

      private fun updateStatus() =
        dm.query(
          DownloadManager.Query().apply {
            setFilterById(downloadId)
            setFilterByStatus(DownloadManager.STATUS_FAILED or DownloadManager.STATUS_SUCCESSFUL)
          }
        ).use { cursor ->

          if (!cursor.moveToFirst()) {
            return
          }

          progressUpdaterInterval.cancel()
        }

      private fun updateProgress() =
        dm.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
          if (!cursor.moveToFirst()) {
            return
          }

          val numberOfBytes =
            cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
          val totalSizeBytes =
            cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

          if (numberOfBytes == totalSizeBytes) {
            progressUpdaterInterval.cancel()
          }

          notifyBridgeOfProgress(context, taskId, numberOfBytes, totalSizeBytes)
        }

      override fun run() {
        try {
          updateProgress()
          updateStatus()
        } finally {
        }
      }
    }
  }
}
