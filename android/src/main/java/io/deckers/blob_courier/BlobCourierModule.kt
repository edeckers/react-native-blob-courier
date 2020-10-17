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
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okio.Okio
import org.json.JSONObject

class BlobCourierModule(val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  val ERROR_MISSING_REQUIRED_PARAM = "ERROR_MISSING_REQUIRED_PARAM"
  val ERROR_UNEXPECTED_EXCEPTION = "ERROR_UNEXPECTED_EXCEPTION"

  val PARAM_FILENAME = "filename"
  val PARAM_FILE_PATH = "filePath"
  val PARAM_METHOD = "method"
  val PARAM_MIME_TYPE = "mimeType"
  val PARAM_URL = "url"
  val PARAM_USE_DOWNLOAD_MANAGER = "useDownloadManager"

  val DEFAULT_METHOD = "GET"
  val DEFAULT_MIME_TYPE = "text/plain"

  override fun getName(): String = "BlobCourier"

  class BlobCourierError(open val code: String, message: String) : Throwable(message)

  val REQUIRED_PARAMETER_PROCESSOR = ImmutableMap.of(
    Boolean::class.java.toString(),
    { input: ReadableMap, parameterName: String -> input.getBoolean(parameterName) },
    String::class.java.toString(),
    { input: ReadableMap, parameterName: String -> input.getString(parameterName) }
  )

  fun assertRequiredParameter(input: ReadableMap, type: Type, parameterName: String) {
    val availableOptions = REQUIRED_PARAMETER_PROCESSOR.keys.joinToString(", ")
    val defaultFallback = "No processor defined for type `$type`, valid options: $availableOptions"
    val unknownProcessor = { _: ReadableMap, _: String -> throw Exception(defaultFallback) }

    val maybeValue =
      REQUIRED_PARAMETER_PROCESSOR.getOrDefault(
        type.toString(), unknownProcessor
      )(input, parameterName)

    if (maybeValue == null) {
      throw BlobCourierError(
        ERROR_MISSING_REQUIRED_PARAM,
        "`$parameterName` is a required parameter of type `$type`"
      )
    }
  }

  fun fetchBlobUsingDownloadManager(uri: Uri, filename: String?, promise: Promise) {
    val downloadManager = reactContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val request = DownloadManager.Request(uri).apply {
      setAllowedOverRoaming(true)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    }

    val downloadId = downloadManager.enqueue(request)

    reactContext.registerReceiver(
      DownloadReceiver(downloadId, promise),
      IntentFilter(
        DownloadManager.ACTION_DOWNLOAD_COMPLETE
      )
    )
  }

  fun client() = OkHttpClientProvider.getOkHttpClient()

  fun fetchBlobWithoutDownloadManager(
    uri: Uri,
    filename: String,
    method: String,
    promise: Promise
  ) {
    val fullFilePath = File(reactContext.cacheDir, filename)

    val okHttpClient = client()

    val request = Request.Builder().method(method, null).url(uri.toString()).build()

    okHttpClient.newCall(request).execute().use { response ->
      response.body()?.source().use { source ->
        Okio.buffer(Okio.sink(fullFilePath)).use { sink ->
          sink.writeAll(source)
        }
      }
    }

    promise.resolve(
      convertJsonToMap(
        JSONObject(
          ImmutableMap.of<String, Any>(
            "type", "Http",
            "response",
            ImmutableMap.of<String, Any>(
              "filePath", fullFilePath,
              "response",
              ImmutableMap.of<String, Any>(
                "code", 200
              )
            )
          )
        )
      )
    )
  }

  fun fetchBlobFromValidatedParameters(input: ReadableMap, promise: Promise) {
    val filename = input.getString(PARAM_FILENAME) ?: ""
    val uri = Uri.parse(input.getString(PARAM_URL))

    val useDownloadManager =
      input.hasKey(PARAM_USE_DOWNLOAD_MANAGER) &&
        input.getBoolean(PARAM_USE_DOWNLOAD_MANAGER)

    if (useDownloadManager) {
      fetchBlobUsingDownloadManager(uri, filename, promise)
      return
    }

    val method = input.getString(PARAM_METHOD) ?: DEFAULT_METHOD

    fetchBlobWithoutDownloadManager(uri, filename, method, promise)
  }

  @ReactMethod
  fun fetchBlob(input: ReadableMap, promise: Promise) {
    try {
      assertRequiredParameter(input, String::class.java, PARAM_FILENAME)
      assertRequiredParameter(input, String::class.java, PARAM_URL)

      fetchBlobFromValidatedParameters(input, promise)
    } catch (e: BlobCourierError) {
      promise.reject(e.code, e.message)
      return
    } catch (e: Exception) {
      promise.reject(ERROR_UNEXPECTED_EXCEPTION, "An unexpected exception occurred: ${e.message}")
      return
    }
  }

  fun uploadBlobFromValidatedParameters(input: ReadableMap, promise: Promise) {
    val filePath = input.getString(PARAM_FILE_PATH)

    val file = File(filePath)

    val uri = URL(input.getString(PARAM_URL))

    val method = input.getString(PARAM_METHOD) ?: DEFAULT_METHOD

    val mimeType = input.getString(PARAM_MIME_TYPE) ?: DEFAULT_MIME_TYPE

    val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
      .addFormDataPart(
        "file", file.getName(),
        RequestBody.create(MediaType.parse(mimeType), file)
      )
      .build()

    val request = Request.Builder()
      .url(uri)
      .method(method, requestBody)
      .build()

    val response = client().newCall(request).execute()

    val data = response.body()?.string() ?: "OH_HELLO"

    promise.resolve(
      convertJsonToMap(
        JSONObject(
          ImmutableMap.of<String, Any>(
            "response",
            ImmutableMap.of<String, Any>("data", data)
          )
        )
      )
    )
  }

  @ReactMethod
  fun uploadBlob(input: ReadableMap, promise: Promise) {
    try {
      assertRequiredParameter(input, String::class.java, PARAM_FILE_PATH)
      assertRequiredParameter(input, String::class.java, PARAM_URL)

      uploadBlobFromValidatedParameters(input, promise)
    } catch (e: BlobCourierError) {
      promise.reject(e.code, e.message)
      return
    } catch (e: Exception) {
      promise.reject(ERROR_UNEXPECTED_EXCEPTION, "An unexpected exception occurred: ${e.message}")
      return
    }
  }
}
