/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.facebook.common.internal.ImmutableMap
import com.facebook.react.bridge.*
import com.facebook.react.modules.network.OkHttpClientProvider
import java.io.File
import java.lang.reflect.Type
import okhttp3.Request
import okio.Okio

class BlobCourierModule(val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  val ERROR_MISSING_REQUIRED_PARAM = "ERROR_MISSING_REQUIRED_PARAM"
  val ERROR_UNEXPECTED_EXCEPTION = "ERROR_UNEXPECTED_EXCEPTION"

  val PARAM_FILENAME = "filename"
  val PARAM_METHOD = "method"
  val PARAM_URL = "url"
  val PARAM_USE_DOWNLOAD_MANAGER = "useDownloadManager"

  val DEFAULT_METHOD = "GET"

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

  fun fetchBlobUsingDownloadManager(uri: Uri, filename: String?) {
    val downloadManager = reactContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val request = DownloadManager.Request(uri).apply {
      setAllowedOverRoaming(true)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    }

    downloadManager.enqueue(request)
  }

  fun fetchBlobWithoutDownloadManager(
    uri: Uri,
    filename: String,
    method: String
  ) {
    val fullFilePath = File(reactContext.cacheDir, filename)

    val okHttpClient = OkHttpClientProvider.getOkHttpClient()

    val request = Request.Builder().method(method, null).url(uri.toString()).build()

    okHttpClient.newCall(request).execute().use { response ->
      response.body()?.source().use { source ->
        Okio.buffer(Okio.sink(fullFilePath)).use { sink ->
          sink.writeAll(source)
        }
      }
    }
  }

  fun fetchBlobFromValidatedParameters(input: ReadableMap, promise: Promise) {
    val filename = input.getString(PARAM_FILENAME) ?: ""
    val uri = Uri.parse(input.getString(PARAM_URL))

    val useDownloadManager =
      input.hasKey(PARAM_USE_DOWNLOAD_MANAGER) &&
        input.getBoolean(PARAM_USE_DOWNLOAD_MANAGER)

    if (useDownloadManager) {
      fetchBlobUsingDownloadManager(uri, filename)
      promise.resolve(true)
      return
    }

    val method = input.getString(PARAM_METHOD) ?: DEFAULT_METHOD

    fetchBlobWithoutDownloadManager(uri, filename, method)
    promise.resolve(true)
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
}
