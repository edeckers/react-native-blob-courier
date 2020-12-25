/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.fetch

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import io.deckers.blob_courier.common.DOWNLOAD_TYPE_MANAGED
import io.deckers.blob_courier.common.MANAGED_DOWNLOAD_FAILURE
import io.deckers.blob_courier.common.MANAGED_DOWNLOAD_SUCCESS
import io.deckers.blob_courier.common.createDownloadManager
import io.deckers.blob_courier.progress.ManagedProgressUpdater
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ManagedDownloadReceiver(
  private val downloadId: Long,
  private val destinationFile: File,
  private val managedProgressUpdater: ManagedProgressUpdater,
  private val processCompletedOrError: (Pair<Throwable?, Map<String, Any>?>) -> Unit
) :
  BroadcastReceiver(), Closeable {
  override fun onReceive(context: Context, intent: Intent) {
    try {
      val downloadManager = createDownloadManager(context)

      processDownloadCompleteAction(downloadManager, context)
    } catch (e: Exception) {
      processCompletedOrError(Pair(e, null))
    } finally {
      context.unregisterReceiver(this)
      close()
    }
  }

  private fun processDownloadCompleteAction(downloadManager: DownloadManager, context: Context) {
    val query = DownloadManager.Query().apply { setFilterById(downloadId) }

    downloadManager.query(query).use { cursor ->
      if (!cursor.moveToFirst()) {
        return
      }

      processCompletedDownloadStatus(context, cursor)
    }
  }

  private fun processCompletedDownloadStatus(context: Context, cursor: Cursor) {
    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

    val isStatusSuccessful = cursor.getInt(columnIndex) == DownloadManager.STATUS_SUCCESSFUL

    if (isStatusSuccessful) {
      val localFileUri =
        Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)))

      onDownloadDone(context, localFileUri)

      return
    }

    processCompletedOrError(
      Pair(null, mapOf<String, Any>("result" to MANAGED_DOWNLOAD_FAILURE))

      // promise.reject(
      //   ERROR_UNEXPECTED_EXCEPTION,
      //     mapOf<String, Any>("result" to MANAGED_DOWNLOAD_FAILURE))
    )
  }

  private fun getFileData(context: Context, uriToUpload: Uri): InputStream? {
    context.contentResolver.query(uriToUpload, null, null, null, null).use { cursor ->
      cursor?.moveToFirst()

      return context.contentResolver.openInputStream(uriToUpload)
    }
  }

  private fun writeFileOnInternalStorage(inputStream: InputStream) {
    if (destinationFile.parentFile?.exists() != true) {
      destinationFile.parentFile?.mkdir()
    }

    try {
      FileOutputStream(destinationFile).use { fos -> inputStream.copyTo(fos) }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun onDownloadDone(context: Context, localFileUri: Uri) {
    moveFileToInternalStorage(context, localFileUri)

    processCompletedOrError(
      Pair(
        null,
        mapOf(
          "type" to DOWNLOAD_TYPE_MANAGED,
          "data" to mapOf(
            "absoluteFilePath" to destinationFile,
            "result" to MANAGED_DOWNLOAD_SUCCESS
          )
        )
      )
    )
  }

  private fun moveFileToInternalStorage(context: Context, localFileUri: Uri) {
    getFileData(context, localFileUri)?.use { fis ->
      writeFileOnInternalStorage(fis)
    }
  }

  override fun close() = managedProgressUpdater.close()
}
