package io.deckers.blob_courier

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.facebook.common.internal.ImmutableMap
import com.facebook.react.bridge.Promise
import org.json.JSONObject

class DownloadReceiver(val downloadId: Long, val promise: Promise) : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val action = intent.action

    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
      val query = DownloadManager.Query()
      query.setFilterById(downloadId)

      downloadManager.query(query).use { c ->
        if (!c.moveToFirst()) {
          return
        }

        val columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
        when (c.getInt(columnIndex)) {
          DownloadManager.STATUS_SUCCESSFUL -> {
            val uri = Uri.parse(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)))

            onDownloadDone(uri)
          }
          else ->
            promise.reject("TEH_CODE", convertJsonToMap(JSONObject(ImmutableMap.of<String, Any>("result", "FAILURE"))))
        }

        context.unregisterReceiver(this)
      }
    }
  }

  fun onDownloadDone(uri: Uri) {
    promise.resolve(convertJsonToMap(JSONObject(ImmutableMap.of("type", "Managed", "response", ImmutableMap.of<String, Any>("result", "SUCCESS", "fullFilePath", uri.toString())))))
  }
}
