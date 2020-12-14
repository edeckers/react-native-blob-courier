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
private const val PARAMETER_PART_PAYLOAD = "payload"
private const val PARAMETER_PARTS = "parts"
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
  ReadableMap::class.java.toString(),
  { input: ReadableMap, parameterName: String -> input.getMap(parameterName) },
  String::class.java.toString(),
  { input: ReadableMap, parameterName: String -> input.getString(parameterName) }
)

private val AVAILABLE_PARAMETER_PROCESSORS = REQUIRED_PARAMETER_PROCESSORS.keys.joinToString(", ")

private fun createHttpClient() = OkHttpClientProvider.getOkHttpClient()

private fun processUnexpectedError(promise: Promise, e: Error) = promise.reject(
  ERROR_UNEXPECTED_ERROR,
  "An unexpected error occurred: ${e.message}"
)

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

private fun verifyFilePart(part: ReadableMap, promise: Promise): Boolean {
  if (!part.hasKey(PARAMETER_PART_PAYLOAD)) {
    promise.reject(ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_PART_PAYLOAD")
    return false
  }

  val payload = part.getMap(PARAMETER_PART_PAYLOAD)!!
  if (!payload.hasKey(PARAMETER_ABSOLUTE_FILE_PATH)) {
    promise.reject(ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_ABSOLUTE_FILE_PATH")
    return false
  }

  if (!payload.hasKey(PARAMETER_MIME_TYPE)) {
    promise.reject(ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_MIME_TYPE")
    return false
  }

  if (payload.getString(PARAMETER_ABSOLUTE_FILE_PATH).isNullOrEmpty()) {
    processUnexpectedEmptyValue(promise, PARAMETER_ABSOLUTE_FILE_PATH)
    return false
  }

  if (payload.getString(PARAMETER_MIME_TYPE).isNullOrEmpty()) {
    processUnexpectedEmptyValue(promise, PARAMETER_MIME_TYPE)
    return false
  }

  return true
}

private fun verifyStringPart(part: ReadableMap, promise: Promise): Boolean {
  if (part.hasKey(PARAMETER_PART_PAYLOAD)) {
    return true
  }

  promise.reject(ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_PART_PAYLOAD")
  return false
}

private fun verifyPart(part: ReadableMap?, promise: Promise): Boolean {
  if (part == null) {
    processUnexpectedEmptyValue(promise, "part")
    return false
  }

  if (!part.hasKey("type")) {
    promise.reject(ERROR_MISSING_REQUIRED_PARAMETER, "part.type")
    return false
  }

  if (part.getString("type") == "file") {
    return verifyFilePart(part, promise)
  }

  return verifyStringPart(part, promise)
}

private fun verifyParts(parts: ReadableMap, promise: Promise): Boolean =
  parts.toHashMap().keys.fold(
    true,
    { p, c -> verifyPart(parts.getMap(c), promise) && p }
  )

private fun startBlobUpload(
  reactContext: ReactApplicationContext,
  taskId: String,
  verifiedParts: ReadableMap,
  uri: URL,
  method: String,
  headers: Map<String, String>,
  returnResponse: Boolean,
  progressInterval: Int,
  promise: Promise,
) {
  val mpb = MultipartBody.Builder()
    .setType(MultipartBody.FORM)

  verifiedParts.toHashMap().keys.forEach { multipartName ->
    val maybePart = verifiedParts.getMap(multipartName)

    maybePart?.run {
      when (this.getString("type")) {
        "file" -> {
          val payload = this.getMap(PARAMETER_PART_PAYLOAD)!!

          val fileUrl = Uri.parse(payload.getString(PARAMETER_ABSOLUTE_FILE_PATH)!!)
          val fileUrlWithScheme =
            if (fileUrl.scheme == null) Uri.parse("file://$fileUrl") else fileUrl

          val filename =
            if (payload.hasKey(PARAMETER_FILENAME)) (
              payload.getString(PARAMETER_FILENAME)
                ?: fileUrl.lastPathSegment
              ) else fileUrl.lastPathSegment

          mpb.addFormDataPart(
            multipartName,
            filename,
            InputStreamRequestBody(
              payload.getString(PARAMETER_MIME_TYPE)?.let { MediaType.parse(it) }
                ?: MediaType.get(DEFAULT_MIME_TYPE),
              reactContext.contentResolver,
              fileUrlWithScheme
            )
          )
        }
        else -> mpb.addFormDataPart(multipartName, this.getString(PARAMETER_PART_PAYLOAD)!!)
      }
    }
  }

  val multipartBody = mpb.build()

  val requestBody = BlobCourierProgressRequest(
    reactContext,
    taskId,
    multipartBody,
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
    try {
      val response = createHttpClient().newCall(
        requestBuilder
      ).execute()

      val b = response.body()?.string().orEmpty()

      promise.resolve(
        mapOf(
          "response" to mapOf(
            "code" to response.code(),
            "data" to if (returnResponse) b else "",
            "headers" to mapHeadersToMap(response.headers())
          )
        ).toReactMap()
      )
    } catch (e: Exception) {
      promise.reject(ERROR_UNEXPECTED_EXCEPTION, e.message)
    } catch (e: Error) {
      promise.reject(ERROR_UNEXPECTED_ERROR, e.message)
    }
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
    val maybeUrl = input.getString(PARAMETER_URL)
    val method = input.getString(PARAMETER_METHOD) ?: DEFAULT_UPLOAD_METHOD
    val maybeParts = input.getMap(PARAMETER_PARTS)

    val unfilteredHeaders =
      input.getMap(PARAMETER_HEADERS)?.toHashMap() ?: emptyMap<String, Any>()

    val headers = filterHeaders(unfilteredHeaders)

    val returnResponse =
      input.hasKey(PARAMETER_RETURN_RESPONSE) && input.getBoolean(PARAMETER_RETURN_RESPONSE)

    val progressInterval =
      getMapInt(input, PARAMETER_SETTINGS_PROGRESS_INTERVAL, DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS)

    if (maybeTaskId.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_TASK_ID)
      return
    }

    if (maybeParts == null) {
      processUnexpectedEmptyValue(promise, PARAMETER_PARTS)
      return
    }

    if (maybeUrl.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_URL)

      return
    }

    if (!verifyParts(maybeParts, promise)) {
      return
    }

    val uri = URL(maybeUrl)

    startBlobUpload(
      reactContext,
      maybeTaskId,
      maybeParts,
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
      createHttpClient().newBuilder()
        .addInterceptor(createDownloadProgressInterceptor(reactContext, taskId, progressInterval))
        .build()

    thread {
      try {
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
      } catch (e0: Exception) {
        promise.reject(ERROR_UNEXPECTED_EXCEPTION, e0.message)
      } catch (e0: Error) {
        promise.reject(ERROR_UNEXPECTED_ERROR, e0.message)
      }
    }
  }

  @Suppress("SameParameterValue")
  private fun getMapInt(input: ReadableMap, field: String, fallback: Int): Int =
    if (input.hasKey(field)) input.getInt(field) else fallback

  private fun fetchBlobFromValidatedParameters(input: ReadableMap, promise: Promise) {
    val maybeTaskId = input.getString(PARAMETER_TASK_ID)
    val maybeFilename = input.getString(PARAMETER_FILENAME)
    val maybeUrl = input.getString(PARAMETER_URL)

    val method = input.getString(PARAMETER_METHOD) ?: DEFAULT_FETCH_METHOD
    val mimeType = input.getString(PARAMETER_MIME_TYPE) ?: DEFAULT_MIME_TYPE

    val maybeAndroidSettings = input.getMap(PARAMETER_ANDROID_SETTINGS)

    val downloadManagerSettings = maybeAndroidSettings?.let {
      it.getMap(PARAMETER_DOWNLOAD_MANAGER_SETTINGS)?.toHashMap()
    } ?: emptyMap<String, Any>()

    val useDownloadManager =
      maybeAndroidSettings?.getBoolean(PARAMETER_USE_DOWNLOAD_MANAGER) ?: false

    val unfilteredHeaders =
      input.getMap(PARAMETER_HEADERS)?.toHashMap() ?: emptyMap<String, Any>()

    val headers = filterHeaders(unfilteredHeaders)

    val progressInterval =
      getMapInt(input, PARAMETER_SETTINGS_PROGRESS_INTERVAL, DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS)

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
    } catch (e: Error) {
      processUnexpectedError(promise, e)
    }
  }

  @ReactMethod
  fun uploadBlob(input: ReadableMap, promise: Promise) {
    try {
      assertRequiredParameter(input, String::class.java, PARAMETER_TASK_ID)
      assertRequiredParameter(input, ReadableMap::class.java, PARAMETER_PARTS)
      assertRequiredParameter(input, String::class.java, PARAMETER_URL)

      uploadBlobFromValidatedParameters(input, promise)
    } catch (e: BlobCourierError) {
      promise.reject(e.code, e.message)
    } catch (e: Exception) {
      processUnexpectedException(promise, e)
    } catch (e: Error) {
      processUnexpectedError(promise, e)
    }
  }
}
