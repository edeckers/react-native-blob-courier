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
import android.net.Uri
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.common.DOWNLOAD_TYPE_UNMANAGED
import io.deckers.blob_courier.common.mapHeadersToMap
import io.deckers.blob_courier.common.toReactMap
import io.deckers.blob_courier.progress.BlobCourierProgressResponse
import io.deckers.blob_courier.progress.ManagedProgressUpdater
import java.io.File
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Okio
import okio.Source

private const val DOWNLOAD_MANAGER_PARAMETER_DESCRIPTION = "description"
private const val DOWNLOAD_MANAGER_PARAMETER_ENABLE_NOTIFICATIONS = "enableNotifications"
private const val DOWNLOAD_MANAGER_PARAMETER_TITLE = "title"

private fun createDownloadProgressInterceptor(
  reactContext: ReactApplicationContext,
  taskId: String,
  progressInterval: Int
): (
  Interceptor.Chain
) -> Response = fun(
  chain: Interceptor.Chain
): Response {
  val originalResponse = chain.proceed(chain.request())

  return originalResponse.body()?.let {
    originalResponse.newBuilder().body(
      BlobCourierProgressResponse(
        reactContext,
        taskId,
        progressInterval,
        it
      )
    ).build()
  } ?: originalResponse
}

class BlobDownloader(
  private val reactContext: ReactApplicationContext,
  private val httpClient: OkHttpClient
) {

  enum class TargetDirectoryEnum {
    Cache,
    Data
  }

  private val defaultDownloadManager =
    reactContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

  private fun createAbsoluteFilePath(
    filename: String,
    targetDirectory: TargetDirectoryEnum
  ) =
    File(
      reactContext.let {
        if (targetDirectory == TargetDirectoryEnum.Data) it.filesDir else it.cacheDir
      },
      filename
    )

  fun download(
    downloaderParameters: DownloaderParameters,
    promise: Promise
  ) =
    if (downloaderParameters.useDownloadManager)
      fetchBlobUsingDownloadManager(
        downloaderParameters,
        downloaderParameters.downloadManagerSettings,
        promise
      )
    else fetchBlobWithoutDownloadManager(downloaderParameters, promise)

  private fun fetchBlobUsingDownloadManager(
    downloaderParameters: DownloaderParameters,
    downloadManagerSettings: Map<String, Any>,
    promise: Promise
  ) {
    val absoluteFilePath =
      createAbsoluteFilePath(downloaderParameters.filename, downloaderParameters.targetDirectory)

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
      ManagedDownloadReceiver(downloadId, absoluteFilePath, progressUpdater, promise),
      IntentFilter(
        DownloadManager.ACTION_DOWNLOAD_COMPLETE,
      )
    )
  }

  private fun fetchBlobWithoutDownloadManager(
    downloaderParameters: DownloaderParameters,
    promise: Promise
  ) {
    val absoluteFilePath =
      createAbsoluteFilePath(downloaderParameters.filename, downloaderParameters.targetDirectory)

    val request = Request.Builder()
      .method(downloaderParameters.method, null)
      .url(downloaderParameters.uri.toString())
      .apply {
        downloaderParameters.headers.forEach { e: Map.Entry<String, String> ->
          addHeader(e.key, e.value)
        }
      }
      .build()

    val progressInterceptor =
      createDownloadProgressInterceptor(
        reactContext, downloaderParameters.taskId, downloaderParameters.progressInterval
      )

    val httpClient =
      httpClient.newBuilder()
        .addInterceptor(progressInterceptor)
        .build()

    httpClient.newCall(request).execute().use { response ->
      response.body()?.source().use { source ->
        Okio.buffer(Okio.sink(absoluteFilePath)).use { sink ->

          sink.writeAll(source as Source)
        }
      }

      promise.resolve(
        mapOf(
          "type" to DOWNLOAD_TYPE_UNMANAGED,
          "data" to mapOf(
            "absoluteFilePath" to Uri.fromFile(absoluteFilePath),
            "response" to mapOf(
              "code" to response.code(),
              "headers" to mapHeadersToMap(response.headers())
            )
          )
        ).toReactMap()
      )
    }
  }
}
