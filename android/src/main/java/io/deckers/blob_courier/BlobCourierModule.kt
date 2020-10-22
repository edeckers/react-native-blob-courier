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
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Okio
import okio.Source
import org.json.JSONObject

private const val ERROR_MISSING_REQUIRED_PARAMETER = "ERROR_MISSING_REQUIRED_PARAMETER"

private const val PARAMETER_FILENAME = "filename"
private const val PARAMETER_FILE_PATH = "filePath"
private const val PARAMETER_METHOD = "method"
private const val PARAMETER_MIME_TYPE = "mimeType"
private const val PARAMETER_TASK_ID = "taskId"
private const val PARAMETER_URL = "url"
private const val PARAMETER_USE_DOWNLOAD_MANAGER = "useDownloadManager"

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

private fun assertRequiredParameter(input: ReadableMap, type: Type, parameterName: String) {
  val defaultFallback =
    "No processor defined for type `$type`, valid options: $AVAILABLE_PARAMETER_PROCESSORS"
  val unknownProcessor = { _: ReadableMap, _: String -> throw Exception(defaultFallback) }

  val maybeValue =
    REQUIRED_PARAMETER_PROCESSORS.getOrDefault(
      type.toString(), unknownProcessor
    )(input, parameterName)

  maybeValue ?: throw BlobCourierError(
    ERROR_MISSING_REQUIRED_PARAMETER,
    "`$parameterName` is a required parameter of type `$type`"
  )
}

private fun createDownloadProgressInterceptor(
  reactContext: ReactApplicationContext,
  taskId: String
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
      .build()
  )

  val request = Request.Builder()
    .url(uri)
    .method(method, requestBody)
    .build()

  thread {
    val response = DEFAULT_OK_HTTP_CLIENT.newCall(request).execute()

    promise.resolve(
      convertJsonToMap(
        JSONObject(
          mapOf<String, Any>(
            "response" to mapOf(
              "code" to response.code(),
              "data" to response.body()?.string().orEmpty(),
              "headers" to response.headers().toMultimap()
            )
          )
        )
      )
    )
  }
}

class BlobCourierModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private val defaultDownloadManager =
    reactContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

  override fun getName(): String = LIBRARY_NAME

  private fun createFullFilePath(filename: String) = File(reactContext.cacheDir, filename)

  private fun uploadBlobFromValidatedParameters(input: ReadableMap, promise: Promise) {
    val maybeTaskId = input.getString(PARAMETER_TASK_ID)
    val maybeFilePath = input.getString(PARAMETER_FILE_PATH)
    val maybeUrl = input.getString(PARAMETER_URL)
    val method = input.getString(PARAMETER_METHOD) ?: DEFAULT_UPLOAD_METHOD
    val mimeType = input.getString(PARAMETER_MIME_TYPE) ?: DEFAULT_MIME_TYPE

    if (maybeTaskId.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_TASK_ID)
      return
    }

    if (maybeFilePath.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_FILE_PATH)

      return
    }

    if (maybeUrl.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_URL)

      return
    }

    val file = File(maybeFilePath)

    val uri = URL(maybeUrl)

    startBlobUpload(reactContext, maybeTaskId, file, mimeType, uri, method, promise)
  }

  private fun fetchBlobUsingDownloadManager(
    taskId: String,
    uri: Uri,
    filename: String,
    promise: Promise
  ) {
    val fullFilePath = createFullFilePath(filename)

    val downloadId =
      DownloadManager.Request(uri)
        .setAllowedOverRoaming(true)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .let { request -> defaultDownloadManager.enqueue(request) }

    ManagedProgressUpdater.start(reactContext, downloadId, taskId)

    reactContext.registerReceiver(
      ManagedDownloadReceiver(downloadId, fullFilePath, promise),
      IntentFilter(
        DownloadManager.ACTION_DOWNLOAD_COMPLETE
      )
    )
  }

  private fun fetchBlobWithoutDownloadManager(
    taskId: String,
    uri: Uri,
    filename: String,
    method: String,
    promise: Promise
  ) {
    val fullFilePath = createFullFilePath(filename)

    val request = Request.Builder().method(method, null).url(uri.toString()).build()

    val httpCient =
      DEFAULT_OK_HTTP_CLIENT.newBuilder()
        .addInterceptor(createDownloadProgressInterceptor(reactContext, taskId))
        .build()

    try {

      httpCient.newCall(request).execute().use { response ->
        thread {
          response.body()?.source().use { source ->
            Okio.buffer(Okio.sink(fullFilePath)).use { sink ->

              sink.writeAll(source as Source)
            }
          }

          promise.resolve(
            convertJsonToMap(
              JSONObject(
                mapOf(
                  "type" to DOWNLOAD_TYPE_UNMANAGED,
                  "data" to mapOf(
                    "fullFilePath" to fullFilePath,
                    "response" to mapOf<String, Any>(
                      "code" to response.code(),
                      "headers" to response.headers().toMultimap()
                    )
                  )
                )
              )
            )
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
    val useDownloadManager =
      input.hasKey(PARAMETER_USE_DOWNLOAD_MANAGER) &&
        input.getBoolean(PARAMETER_USE_DOWNLOAD_MANAGER)
    val method = input.getString(PARAMETER_METHOD) ?: DEFAULT_FETCH_METHOD

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
      maybeFilename,
      promise,
      method
    )
  }

  private fun startBlobFetch(
    taskId: String,
    uri: Uri,
    useDownloadManager: Boolean,
    filename: String,
    promise: Promise,
    method: String
  ) =
    if (useDownloadManager)
      fetchBlobUsingDownloadManager(taskId, uri, filename, promise)
    else fetchBlobWithoutDownloadManager(taskId, uri, filename, method, promise)

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
      assertRequiredParameter(input, String::class.java, PARAMETER_FILE_PATH)
      assertRequiredParameter(input, String::class.java, PARAMETER_URL)

      uploadBlobFromValidatedParameters(input, promise)
    } catch (e: BlobCourierError) {
      promise.reject(e.code, e.message)
    } catch (e: Exception) {
      processUnexpectedException(promise, e)
    }
  }
}
