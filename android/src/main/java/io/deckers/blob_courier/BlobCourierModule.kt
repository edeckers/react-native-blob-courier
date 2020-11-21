/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import android.app.DownloadManager
import android.content.Context
import android.content.IntentFilter
import android.net.Uri
import com.facebook.common.internal.ImmutableMap
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.modules.network.OkHttpClientProvider
import java.io.File
import java.lang.reflect.Type
import java.net.URL
import kotlin.concurrent.thread
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Okio
import okio.Source

private const val ERROR_MISSING_REQUIRED_PARAMETER = "ERROR_MISSING_REQUIRED_PARAMETER"

private const val PARAMETER_ABSOLUTE_FILE_PATH = "absoluteFilePath"
private const val PARAMETER_ANDROID_SETTINGS = "android"
private const val PARAMETER_DOWNLOAD_MANAGER_SETTINGS = "downloadManager"
private const val PARAMETER_FILENAME = "filename"
private const val PARAMETER_HEADERS = "headers"
private const val PARAMETER_METHOD = "method"
private const val PARAMETER_MIME_TYPE = "mimeType"
private const val PARAMETER_RETURN_RESPONSE = "returnResponse"
private const val PARAMETER_SETTINGS_PROGRESS_INTERVAL = "progressIntervalMilliseconds"
private const val PARAMETER_TASK_ID = "taskId"
private const val PARAMETER_URL = "url"
private const val PARAMETER_USE_DOWNLOAD_MANAGER = "useDownloadManager"

private const val DOWNLOAD_MANAGER_PARAMETER_DESCRIPTION = "description"
private const val DOWNLOAD_MANAGER_PARAMETER_ENABLE_NOTIFICATIONS = "enableNotifications"
private const val DOWNLOAD_MANAGER_PARAMETER_TITLE = "title"

private val REQUIRED_PARAMETER_PROCESSORS = ImmutableMap.of(
  Boolean::class.java.toString(),
  { input: ReadableMap, parameterName: String -> input.getBoolean(parameterName) },
  String::class.java.toString(),
  { input: ReadableMap, parameterName: String -> input.getString(parameterName) }
)

private val AVAILABLE_PARAMETER_PROCESSORS = REQUIRED_PARAMETER_PROCESSORS.keys.joinToString(", ")

private val DEFAULT_OK_HTTP_CLIENT = OkHttpClientProvider.getOkHttpClient()

private fun processUnexpectedException(promise: Promise, e: Exception) = promise.reject(
  ERROR_UNEXPECTED_EXCEPTION,
  "An unexpected exception occurred: ${e.message}"
)

private fun processUnexpectedEmptyValue(promise: Promise, parameterName: String) = promise.reject(
  ERROR_UNEXPECTED_EMPTY_VALUE,
  "Parameter `$parameterName` cannot be empty."
)

private fun mapHeadersToMap(headers: Headers): Map<String, String> =
  headers
    .toMultimap()
    .map { entry -> Pair(entry.key, entry.value.joinToString()) }
    .toMap()

private fun assertRequiredParameter(input: ReadableMap, type: Type, parameterName: String) {
  val defaultFallback =
    "No processor defined for type `$type`, valid options: $AVAILABLE_PARAMETER_PROCESSORS"
  val unknownProcessor = { _: ReadableMap, _: String -> throw Exception(defaultFallback) }

  val maybeValue =
    REQUIRED_PARAMETER_PROCESSORS.getOrElse(
      type.toString(), { unknownProcessor }
    )(input, parameterName)

  maybeValue ?: throw BlobCourierError(
    ERROR_MISSING_REQUIRED_PARAMETER,
    "`$parameterName` is a required parameter of type `$type`"
  )
}

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

private fun startBlobUpload(
  reactContext: ReactApplicationContext,
  taskId: String,
  file: File,
  mimeType: String,
  uri: URL,
  method: String,
  headers: Map<String, String>,
  returnResponse: Boolean,
  progressInterval: Int,
  promise: Promise
) {
  val requestBody = BlobCourierProgressRequest(
    reactContext,
    taskId,
    MultipartBody.Builder().setType(MultipartBody.FORM)
      .addFormDataPart(
        "file", file.name,
        RequestBody.create(MediaType.parse(mimeType), file)
      )
      .build(),
    progressInterval
  )

  val requestBuilder = Request.Builder()
    .url(uri)
    .method(method, requestBody)
    .apply {
      headers.forEach { e: Map.Entry<String, String> ->
        addHeader(e.key, e.value)
      }
    }
    .build()

  thread {
    val response = DEFAULT_OK_HTTP_CLIENT.newCall(
      requestBuilder
    ).execute()

    promise.resolve(
      mapOf(
        "response" to mapOf(
          "code" to response.code(),
          "data" to if (returnResponse) response.body()?.string().orEmpty() else "",
          "headers" to mapHeadersToMap(response.headers())
        )
      ).toReactMap()
    )
  }
}

private fun filterHeaders(unfilteredHeaders: Map<String, Any>): Map<String, String> =
  unfilteredHeaders
    .mapValues { (_, v) -> v as? String }
    .filter { true }
    .mapNotNull { (k, v) -> v?.let { k to it } }
    .toMap()

class BlobCourierModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private val defaultDownloadManager =
    reactContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

  override fun getName(): String = LIBRARY_NAME

  private fun createAbsoluteFilePath(filename: String) = File(reactContext.cacheDir, filename)

  private fun uploadBlobFromValidatedParameters(input: ReadableMap, promise: Promise) {
    val maybeTaskId = input.getString(PARAMETER_TASK_ID)
    val maybeFilePath = input.getString(PARAMETER_ABSOLUTE_FILE_PATH)
    val maybeUrl = input.getString(PARAMETER_URL)
    val method = input.getString(PARAMETER_METHOD) ?: DEFAULT_UPLOAD_METHOD
    val mimeType = input.getString(PARAMETER_MIME_TYPE) ?: DEFAULT_MIME_TYPE

    val unfilteredHeaders =
      if (input.hasKey(PARAMETER_HEADERS))
        input.getMap(PARAMETER_HEADERS)?.toHashMap() ?: emptyMap<String, Any>()
      else emptyMap<String, Any>()

    val headers = filterHeaders(unfilteredHeaders)

    val returnResponse =
      input.hasKey(PARAMETER_RETURN_RESPONSE) && input.getBoolean(PARAMETER_RETURN_RESPONSE)

    val progressInterval =
      if (input.hasKey(PARAMETER_SETTINGS_PROGRESS_INTERVAL))
        input.getInt(PARAMETER_SETTINGS_PROGRESS_INTERVAL)
      else DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS

    if (maybeTaskId.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_TASK_ID)
      return
    }

    if (maybeFilePath.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_ABSOLUTE_FILE_PATH)

      return
    }

    if (maybeUrl.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_URL)

      return
    }

    val file = File(maybeFilePath)

    val uri = URL(maybeUrl)

    startBlobUpload(
      reactContext,
      maybeTaskId,
      file,
      mimeType,
      uri,
      method,
      headers,
      returnResponse,
      progressInterval,
      promise
    )
  }

  private fun fetchBlobUsingDownloadManager(
    taskId: String,
    downloadManagerSettings: Map<String, Any>,
    uri: Uri,
    filename: String,
    headers: Map<String, String>,
    mimeType: String,
    progressInterval: Int,
    promise: Promise
  ) {
    val absoluteFilePath = createAbsoluteFilePath(filename)

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
    filename: String,
    headers: Map<String, String>,
    method: String,
    progressInterval: Int,
    promise: Promise
  ) {
    val absoluteFilePath = createAbsoluteFilePath(filename)

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
      DEFAULT_OK_HTTP_CLIENT.newBuilder()
        .addInterceptor(createDownloadProgressInterceptor(reactContext, taskId, progressInterval))
        .build()

    try {

      httpClient.newCall(request).execute().use { response ->
        thread {
          response.body()?.source().use { source ->
            Okio.buffer(Okio.sink(absoluteFilePath)).use { sink ->

              sink.writeAll(source as Source)
            }
          }

          promise.resolve(
            mapOf(
              "type" to DOWNLOAD_TYPE_UNMANAGED,
              "data" to mapOf(
                "absoluteFilePath" to absoluteFilePath,
                "response" to mapOf(
                  "code" to response.code(),
                  "headers" to mapHeadersToMap(response.headers())
                )
              )
            ).toReactMap()
          )
        }
      }
    } catch (e: Exception) {
      promise.reject(ERROR_UNEXPECTED_EXCEPTION, e.message)
    }
  }

  private fun fetchBlobFromValidatedParameters(input: ReadableMap, promise: Promise) {
    val maybeTaskId = input.getString(PARAMETER_TASK_ID)
    val maybeFilename = input.getString(PARAMETER_FILENAME)
    val maybeUrl = input.getString(PARAMETER_URL)

    val method = input.getString(PARAMETER_METHOD) ?: DEFAULT_FETCH_METHOD
    val mimeType = input.getString(PARAMETER_MIME_TYPE) ?: DEFAULT_MIME_TYPE

    val maybeAndroidSettings =
      if (input.hasKey(PARAMETER_ANDROID_SETTINGS))
        input.getMap(PARAMETER_ANDROID_SETTINGS)
      else null

    val downloadManagerSettings = maybeAndroidSettings?.let {
      it.getMap(PARAMETER_DOWNLOAD_MANAGER_SETTINGS)?.toHashMap()
    } ?: emptyMap<String, Any>()

    val useDownloadManager =
      maybeAndroidSettings?.getBoolean(PARAMETER_USE_DOWNLOAD_MANAGER) ?: false

    val unfilteredHeaders =
      input.getMap(PARAMETER_HEADERS)?.toHashMap() ?: emptyMap<String, Any>()

    val headers = filterHeaders(unfilteredHeaders)

    val progressInterval =
      if (input.hasKey(PARAMETER_SETTINGS_PROGRESS_INTERVAL))
        input.getInt(PARAMETER_SETTINGS_PROGRESS_INTERVAL)
      else DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS

    if (maybeTaskId.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_TASK_ID)
      return
    }

    if (maybeFilename.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_FILENAME)
      return
    }

    if (maybeUrl.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_URL)

      return
    }

    startBlobFetch(
      maybeTaskId,
      Uri.parse(maybeUrl),
      useDownloadManager,
      downloadManagerSettings,
      maybeFilename,
      mimeType,
      promise,
      method,
      headers,
      progressInterval
    )
  }

  private fun startBlobFetch(
    taskId: String,
    uri: Uri,
    useDownloadManager: Boolean,
    downloadManagerSettings: Map<String, Any>,
    filename: String,
    mimeType: String,
    promise: Promise,
    method: String,
    headers: Map<String, String>,
    progressInterval: Int
  ) =
    if (useDownloadManager)
      fetchBlobUsingDownloadManager(
        taskId,
        downloadManagerSettings,
        uri,
        filename,
        headers,
        mimeType,
        progressInterval,
        promise
      )
    else fetchBlobWithoutDownloadManager(
      taskId,
      uri,
      filename,
      headers,
      method,
      progressInterval,
      promise
    )

  @ReactMethod
  fun fetchBlob(input: ReadableMap, promise: Promise) {
    try {
      assertRequiredParameter(input, String::class.java, PARAMETER_TASK_ID)
      assertRequiredParameter(input, String::class.java, PARAMETER_FILENAME)
      assertRequiredParameter(input, String::class.java, PARAMETER_URL)

      fetchBlobFromValidatedParameters(input, promise)
    } catch (e: BlobCourierError) {
      promise.reject(e.code, e.message)
    } catch (e: Exception) {
      processUnexpectedException(promise, e)
    }
  }

  @ReactMethod
  fun uploadBlob(input: ReadableMap, promise: Promise) {
    try {
      assertRequiredParameter(input, String::class.java, PARAMETER_TASK_ID)
      assertRequiredParameter(input, String::class.java, PARAMETER_ABSOLUTE_FILE_PATH)
      assertRequiredParameter(input, String::class.java, PARAMETER_URL)

      uploadBlobFromValidatedParameters(input, promise)
    } catch (e: BlobCourierError) {
      promise.reject(e.code, e.message)
    } catch (e: Exception) {
      processUnexpectedException(promise, e)
    }
  }
}
