/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.upload

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_ERROR
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_EXCEPTION
import io.deckers.blob_courier.common.mapHeadersToMap
import io.deckers.blob_courier.common.toReactMap
import io.deckers.blob_courier.progress.BlobCourierProgressRequest
import okhttp3.OkHttpClient
import okhttp3.Request

class BlobUploader(
  private val reactContext: ReactApplicationContext,
  private val httpClient: OkHttpClient
) {

  fun upload(
    uploaderParameters: UploaderParameters,
    promise: Promise,
  ) {

    val requestBody = BlobCourierProgressRequest(
      reactContext,
      uploaderParameters.taskId,
      uploaderParameters.toMultipartBody(reactContext.contentResolver),
      uploaderParameters.progressInterval
    )

    val requestBuilder = Request.Builder()
      .url(uploaderParameters.uri)
      .method(uploaderParameters.method, requestBody)
      .apply {
        uploaderParameters.headers.forEach { e: Map.Entry<String, String> ->
          addHeader(e.key, e.value)
        }
      }
      .build()

    try {
      val response = httpClient.newCall(
        requestBuilder
      ).execute()

      val b = response.body()?.string().orEmpty()

      promise.resolve(
        mapOf(
          "response" to mapOf(
            "code" to response.code(),
            "data" to if (uploaderParameters.returnResponse) b else "",
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
