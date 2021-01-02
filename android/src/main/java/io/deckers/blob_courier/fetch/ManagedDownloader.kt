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
import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.common.BlobCourierError
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_ERROR
import io.deckers.blob_courier.common.Failure
import io.deckers.blob_courier.common.Logger
import io.deckers.blob_courier.common.MANAGED_DOWNLOAD_FAILURE
import io.deckers.blob_courier.common.Result
import io.deckers.blob_courier.common.createErrorFromThrowabe
import io.deckers.blob_courier.common.fold
import io.deckers.blob_courier.common.tag
import io.deckers.blob_courier.progress.ManagedProgressUpdater
import io.deckers.blob_courier.progress.ProgressNotifier
import java.io.File

private const val DOWNLOAD_MANAGER_PARAMETER_DESCRIPTION = "description"
private const val DOWNLOAD_MANAGER_PARAMETER_ENABLE_NOTIFICATIONS = "enableNotifications"
private const val DOWNLOAD_MANAGER_PARAMETER_TITLE = "title"

private val TAG = tag(ManagedDownloader::class.java.name)
private val logger = Logger(TAG)
private fun li(m: String) = logger.i(m)
private fun lv(m: String, e: Throwable? = null) = logger.v(m, e)

class ManagedDownloader(
  private val reactContext: ReactApplicationContext,
  private val progressNotifier: ProgressNotifier
) {
  private val defaultDownloadManager =
    reactContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

  fun fetch(
    downloaderParameters: DownloaderParameters,
    toAbsoluteFilePath: File,
  ): Result<Map<String, Any>> {
    try {
      li("Starting managed fetch")
      val request =
        DownloadManager.Request(downloaderParameters.uri)
          .setAllowedOverRoaming(true)
          .setMimeType(downloaderParameters.mimeType)
          .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
      lv("Created request")

      val downloadManagerSettings = downloaderParameters.downloadManagerSettings
      if (downloadManagerSettings.containsKey(DOWNLOAD_MANAGER_PARAMETER_DESCRIPTION)) {
        request.setDescription(
          downloadManagerSettings[DOWNLOAD_MANAGER_PARAMETER_DESCRIPTION] as String
        )
      }
      lv("Set download description")

      if (downloadManagerSettings.containsKey(DOWNLOAD_MANAGER_PARAMETER_TITLE)) {
        request.setTitle(
          downloadManagerSettings[DOWNLOAD_MANAGER_PARAMETER_TITLE] as String
        )
      }
      lv("Set download title")

      val enableNotifications =
        downloadManagerSettings.containsKey(DOWNLOAD_MANAGER_PARAMETER_ENABLE_NOTIFICATIONS) &&
          downloadManagerSettings[DOWNLOAD_MANAGER_PARAMETER_ENABLE_NOTIFICATIONS] == true

      request.setNotificationVisibility(if (enableNotifications) 1 else 0)
      lv("Toggled notification visibility (visibility=$enableNotifications)")

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
      lv("Queued request")

      val progressUpdater =
        ManagedProgressUpdater(
          reactContext,
          downloadId,
          downloaderParameters.progressInterval.toLong(),
          progressNotifier
        )

      progressUpdater.start()
      lv("Started progress updater")

      val waitForDownloadCompletion = Object()

      var result: Result<Map<String, Any>>? = null

      synchronized(waitForDownloadCompletion) {
        val downloadReceiver =
          ManagedDownloadReceiver(
            downloadId,
            toAbsoluteFilePath,
            progressUpdater
          ) { errorOrResult ->
            result = errorOrResult
            lv("Finished managed download")

            synchronized(waitForDownloadCompletion) {
              waitForDownloadCompletion.notify()
              lv("Notify lock")
            }
          }

        reactContext.registerReceiver(
          downloadReceiver,
          IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )

        lv("Registered ${DownloadManager.ACTION_DOWNLOAD_COMPLETE} receiver")

        lv("Waiting for completion")

        waitForDownloadCompletion.wait()

        li("Finished managed fetch and returning result")

        lv("Result: ${result?.fold({ it }, { it.toString() }) ?: "NO_RESULT_SET"}")

        return result
          ?: Failure(BlobCourierError(MANAGED_DOWNLOAD_FAILURE, "Result was never set"))
      }
    } catch (e: Throwable) {
      return Failure(createErrorFromThrowabe(ERROR_UNEXPECTED_ERROR, e))
    }
  }
}
