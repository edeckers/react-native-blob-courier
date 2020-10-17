package io.deckers.blob_courier

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import com.facebook.react.bridge.Promise
import org.json.JSONObject

class DownloadReceiver(private val downloadId: Long, private val promise: Promise) :
  BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
      return
    }

    processDownloadCompleteAction(downloadManager, context)
  }

  private fun processDownloadCompleteAction(downloadManager: DownloadManager, context: Context) {
    val query = DownloadManager.Query().apply { setFilterById(downloadId) }

    downloadManager.query(query).use { cursor ->
      if (!cursor.moveToFirst()) {
        return
      }

      processCompletedDownloadStatus(cursor)

      context.unregisterReceiver(this)
    }
  }

  private fun processCompletedDownloadStatus(cursor: Cursor) {
    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

    val isStatusSuccessful = cursor.getInt(columnIndex) == DownloadManager.STATUS_SUCCESSFUL

    if (isStatusSuccessful) {
      onDownloadDone(
        Uri.parse(
          cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
        )
      )

      return
    }

    promise.reject(
      ERROR_UNEXPECTED_EXCEPTION,
      convertJsonToMap(
        JSONObject(
          mapOf<String, Any>("result" to MANAGED_DOWNLOAD_FAILURE)
        )
      )
    )
  }

  private fun onDownloadDone(uri: Uri) = promise.resolve(
    convertJsonToMap(
      JSONObject(
        mapOf(
          "type" to DOWNLOAD_TYPE_MANAGED,
          "response" to mapOf<String, Any>(
            "result" to MANAGED_DOWNLOAD_SUCCESS,
            "fullFilePath" to uri.toString()
          )
        )
      )
    )
  )
}
