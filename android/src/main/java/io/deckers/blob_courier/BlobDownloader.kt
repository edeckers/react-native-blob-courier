package io.deckers.blob_courier

import android.app.DownloadManager
import android.content.Context
import android.content.IntentFilter
import android.net.Uri
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
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

  fun startBlobFetch(
    ps: DownloaderParameters,
    promise: Promise
  ) =
    if (ps.useDownloadManager)
      fetchBlobUsingDownloadManager(
        ps.taskId,
        ps.downloadManagerSettings,
        ps.uri,
        ps.targetDirectory,
        ps.filename,
        ps.headers,
        ps.mimeType,
        ps.progressInterval,
        promise
      )
    else fetchBlobWithoutDownloadManager(
      ps.taskId,
      ps.uri,
      ps.targetDirectory,
      ps.filename,
      ps.headers,
      ps.method,
      ps.progressInterval,
      promise
    )

  private fun fetchBlobUsingDownloadManager(
    taskId: String,
    downloadManagerSettings: Map<String, Any>,
    uri: Uri,
    targetDirectory: TargetDirectoryEnum,
    filename: String,
    headers: Map<String, String>,
    mimeType: String,
    progressInterval: Int,
    promise: Promise
  ) {
    val absoluteFilePath = createAbsoluteFilePath(filename, targetDirectory)

    val request =
      DownloadManager.Request(uri)
        .setAllowedOverRoaming(true)
        .setMimeType(mimeType)
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
          headers.forEach { e: Map.Entry<String, String> ->
            addRequestHeader(e.key, e.value)
          }
        }

        defaultDownloadManager.enqueue(
          requestBuilder
        )
      }

    val progressUpdater =
      ManagedProgressUpdater(reactContext, taskId, downloadId, progressInterval.toLong())

    progressUpdater.start()

    reactContext.registerReceiver(
      ManagedDownloadReceiver(downloadId, absoluteFilePath, progressUpdater, promise),
      IntentFilter(
        DownloadManager.ACTION_DOWNLOAD_COMPLETE,
      )
    )
  }

  private fun fetchBlobWithoutDownloadManager(
    taskId: String,
    uri: Uri,
    targetDirectory: TargetDirectoryEnum,
    filename: String,
    headers: Map<String, String>,
    method: String,
    progressInterval: Int,
    promise: Promise
  ) {
    val absoluteFilePath = createAbsoluteFilePath(filename, targetDirectory)

    val request = Request.Builder()
      .method(method, null)
      .url(uri.toString())
      .apply {
        headers.forEach { e: Map.Entry<String, String> ->
          addHeader(e.key, e.value)
        }
      }
      .build()

    val httpClient =
      httpClient.newBuilder()
        .addInterceptor(createDownloadProgressInterceptor(reactContext, taskId, progressInterval))
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
