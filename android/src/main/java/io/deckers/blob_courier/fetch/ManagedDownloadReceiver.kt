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
import io.deckers.blob_courier.common.BlobCourierError
import io.deckers.blob_courier.common.DOWNLOAD_TYPE_MANAGED
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_EXCEPTION
import io.deckers.blob_courier.common.Failure
import io.deckers.blob_courier.common.Logger
import io.deckers.blob_courier.common.MANAGED_DOWNLOAD_FAILURE
import io.deckers.blob_courier.common.MANAGED_DOWNLOAD_SUCCESS
import io.deckers.blob_courier.common.Result
import io.deckers.blob_courier.common.Success
import io.deckers.blob_courier.common.createDownloadManager
import io.deckers.blob_courier.common.createErrorFromThrowable
import io.deckers.blob_courier.progress.ManagedProgressUpdater
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

private val TAG = ManagedDownloadReceiver::class.java.name
private val logger = Logger(TAG)
private fun lv(m: String, e: Throwable? = null) = logger.v(m, e)
private fun lw(m: String, e: Throwable? = null) = logger.w(m, e)

class ManagedDownloadReceiver(
  private val downloadId: Long,
  private val destinationFile: File,
  private val managedProgressUpdater: ManagedProgressUpdater,
  private val processCompletedOrError: (Result<Map<String, Any>>) -> Unit
) :
  BroadcastReceiver(), Closeable {
  override fun onReceive(context: Context, intent: Intent) {
    try {
      val intentDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

      if (intentDownloadId != downloadId) {
        lv("Ignoring ${DownloadManager.ACTION_DOWNLOAD_COMPLETE} message for another download (downloadId=${downloadId}, receivedId=${intentDownloadId})")
        return
      }

      lv("Processing ${DownloadManager.ACTION_DOWNLOAD_COMPLETE} message (downloadId=${intentDownloadId}")
      val downloadManager = createDownloadManager(context)

      processDownloadCompleteAction(downloadManager, context)
    } catch (e: Exception) {
      lv("Processing completed error: ${e.message}")
      processCompletedOrError(Failure(createErrorFromThrowable(ERROR_UNEXPECTED_EXCEPTION, e)))
    } finally {
      context.unregisterReceiver(this)
      close()
      lv("Unregistered and closed ${DownloadManager.ACTION_DOWNLOAD_COMPLETE} receiver (downloadId=${downloadId})")
    }
  }

  private fun processDownloadCompleteAction(downloadManager: DownloadManager, context: Context) {
    val query = DownloadManager.Query().setFilterById(downloadId)
    lv("Queried download manager for download (id=$downloadId)")

    downloadManager.query(query).use { cursor ->
      if (!cursor.moveToFirst()) {
        lw("Did not find download (id=$downloadId)")
        return
      }

      processCompletedDownloadStatus(context, cursor)
    }
  }

  private fun processCompletedDownloadStatus(context: Context, cursor: Cursor) {
    lv("Processing completed download status")
    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
    val status = cursor.getInt(columnIndex)
    val isStatusSuccessful = status == DownloadManager.STATUS_SUCCESSFUL

    lv("Received status (status=$status, isStatusSuccessful=$isStatusSuccessful)")

    if (isStatusSuccessful) {
      val columnLocalUri = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
      val isExistingColumn = columnLocalUri > 0
      if (!isExistingColumn) {
        return
      }

      val localFileUri = Uri.parse(cursor.getString(columnLocalUri))

      onDownloadDone(context, localFileUri)

      return
    }

    lw("Something went wrong processing the managed download (status=$status)")

    processCompletedOrError(
      Failure(
        BlobCourierError(
          MANAGED_DOWNLOAD_FAILURE,
          "Something went wrong retrieving download status (status=$status)"
        )
      )
    )
  }

  private fun getFileData(context: Context, uriToUpload: Uri): InputStream? {
    context.contentResolver.query(uriToUpload, null, null, null, null).use { cursor ->
      cursor?.moveToFirst()

      return context.contentResolver.openInputStream(uriToUpload)
    }
  }

  private fun writeFileOnInternalStorage(inputStream: InputStream) {
    lv("Writing stream to $destinationFile")
    if (destinationFile.parentFile?.exists() != true) {
      lv("${destinationFile.parentFile} does not exist, so create it")
      destinationFile.parentFile?.mkdir()
    }

    try {
      FileOutputStream(destinationFile).use { fos -> inputStream.copyTo(fos) }
      lv("Finished writing stream to $destinationFile")
    } catch (e: Exception) {
      lv("Something went wrong writing the stream to $destinationFile", e)

      throw e
    }
  }

  private fun onDownloadDone(context: Context, localFileUri: Uri) {
    lv("Processing succesful managed download")
    moveFileToInternalStorage(context, localFileUri)

    lv("Returning success message")
    processCompletedOrError(
      Success(
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
    lv("Writing downloaded file $localFileUri to internal storage")
    getFileData(context, localFileUri)?.use { fis ->
      writeFileOnInternalStorage(fis)
    }
  }

  override fun close() = managedProgressUpdater.close()
}
