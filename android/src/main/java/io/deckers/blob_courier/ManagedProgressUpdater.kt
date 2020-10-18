/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import android.app.DownloadManager
import android.os.Handler
import com.facebook.react.bridge.ReactApplicationContext

class ManagedProgressUpdater {
  companion object {
    fun start(context: ReactApplicationContext, downloadId: Long, taskId: String) {
      val query = DownloadManager.Query().setFilterById(downloadId)

      ProgressUpdateRunner(context, createDownloadManager(context), downloadId, taskId).run()
    }

    private class ProgressUpdateRunner(
      private val context: ReactApplicationContext,
      private val dm: DownloadManager,
      private val downloadId: Long,
      private val taskId: String
    ) : Runnable {
      private val progressDelay = 100L
      private val handler = Handler()

      private var isRunning = true

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

          isRunning = false
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

          isRunning = numberOfBytes != totalSizeBytes

          notifyBridgeOfProgress(context, taskId, numberOfBytes, totalSizeBytes)
        }

      override fun run() {
        try {
          updateProgress()
          updateStatus()
        } finally {
          if (isRunning) {
            handler.postDelayed(this, progressDelay)
          }
        }
      }
    }
  }
}
