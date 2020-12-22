/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.fetch

import android.app.DownloadManager
import android.content.Context
import android.content.IntentFilter
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.progress.ManagedProgressUpdater
import java.io.File

private const val DOWNLOAD_MANAGER_PARAMETER_DESCRIPTION = "description"
private const val DOWNLOAD_MANAGER_PARAMETER_ENABLE_NOTIFICATIONS = "enableNotifications"
private const val DOWNLOAD_MANAGER_PARAMETER_TITLE = "title"

class ManagedDownloader(private val reactContext: ReactApplicationContext) {
  private val defaultDownloadManager =
    reactContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

  fun fetchBlobUsingDownloadManager(
    downloaderParameters: DownloaderParameters,
    toAbsoluteFilePath: File,
    downloadManagerSettings: Map<String, Any>,
    promise: Promise
  ) {
    val request =
      DownloadManager.Request(downloaderParameters.uri)
        .setAllowedOverRoaming(true)
        .setMimeType(downloaderParameters.mimeType)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

    if (downloadManagerSettings.containsKey(DOWNLOAD_MANAGER_PARAMETER_DESCRIPTION)) {
      request.setDescription(
        downloadManagerSettings[DOWNLOAD_MANAGER_PARAMETER_DESCRIPTION] as String
      )
    }

    if (downloadManagerSettings.containsKey(DOWNLOAD_MANAGER_PARAMETER_TITLE)) {
      request.setTitle(
        downloadManagerSettings[DOWNLOAD_MANAGER_PARAMETER_TITLE] as String
      )
    }

    val enableNotifications =
      downloadManagerSettings.containsKey(DOWNLOAD_MANAGER_PARAMETER_ENABLE_NOTIFICATIONS) &&
        downloadManagerSettings[DOWNLOAD_MANAGER_PARAMETER_ENABLE_NOTIFICATIONS] == true

    request.setNotificationVisibility(if (enableNotifications) 1 else 0)

    val downloadId = request
      .let { requestBuilder
        ->
        requestBuilder.apply {
          downloaderParameters.headers.forEach { e: Map.Entry<String, String> ->
            addRequestHeader(e.key, e.value)
          }
        }

        defaultDownloadManager.enqueue(
          requestBuilder
        )
      }

    val progressUpdater =
      ManagedProgressUpdater(
        reactContext,
        downloaderParameters.taskId,
        downloadId,
        downloaderParameters.progressInterval.toLong()
      )

    progressUpdater.start()

    reactContext.registerReceiver(
      ManagedDownloadReceiver(downloadId, toAbsoluteFilePath, progressUpdater, promise),
      IntentFilter(
        DownloadManager.ACTION_DOWNLOAD_COMPLETE,
      )
    )
  }
}
