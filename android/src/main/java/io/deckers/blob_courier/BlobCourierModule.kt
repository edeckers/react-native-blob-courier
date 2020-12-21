/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.modules.network.OkHttpClientProvider
import java.net.URL
import java.net.UnknownHostException
import kotlin.concurrent.thread

private fun createHttpClient() = OkHttpClientProvider.getOkHttpClient()

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

class BlobCourierModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String = LIBRARY_NAME

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

    try {
      val uploader = BlobUploader(reactContext, createHttpClient())

      uploader.upload(
        maybeTaskId,
        maybeParts,
        uri,
        method,
        headers,
        returnResponse,
        progressInterval,
        promise
      )
    } catch (e: UnknownHostException) {
      promise.reject(ERROR_UNKNOWN_HOST, e)
    }
  }

  private fun fetchBlobFromValidatedParameters(ps: DownloaderParameters, promise: Promise) {
    try {
      BlobDownloader(reactContext, createHttpClient())
        .startBlobFetch(ps, promise)
    } catch (e: UnknownHostException) {
      promise.reject(ERROR_UNKNOWN_HOST, e)
    }
  }

  @ReactMethod
  fun fetchBlob(input: ReadableMap, promise: Promise) {
    thread {
      try {
        val ps = DownloaderParameterFactory().fromInput(input, promise)

        ps?.run { fetchBlobFromValidatedParameters(ps, promise) }
      } catch (e: BlobCourierError) {
        promise.reject(e.code, e.message)
      } catch (e: Exception) {
        processUnexpectedException(promise, e)
      } catch (e: Error) {
        processUnexpectedError(promise, e)
      }
    }
  }

  @ReactMethod
  fun uploadBlob(input: ReadableMap, promise: Promise) {
    thread {
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
}
