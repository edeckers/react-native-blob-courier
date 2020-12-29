/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.upload

import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_ERROR
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_EXCEPTION
import io.deckers.blob_courier.common.Failure
import io.deckers.blob_courier.common.Result
import io.deckers.blob_courier.common.Success
import io.deckers.blob_courier.common.createErrorFromThrowabe
import io.deckers.blob_courier.common.mapHeadersToMap
import io.deckers.blob_courier.progress.BlobCourierProgressRequest
import io.deckers.blob_courier.progress.ProgressNotifierFactory
import okhttp3.OkHttpClient
import okhttp3.Request

class BlobUploader(
  private val reactContext: ReactApplicationContext,
  private val httpClient: OkHttpClient,
  private val progressNotifierFactory: ProgressNotifierFactory
) {

  fun upload(uploaderParameters: UploaderParameters): Result<Map<String, Any>> {

    val requestBody = BlobCourierProgressRequest(
      uploaderParameters.toMultipartBody(reactContext.contentResolver),
      progressNotifierFactory.create(uploaderParameters.taskId)
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

      return Success(
        mapOf(
          "response" to mapOf(
            "code" to response.code(),
            "data" to if (uploaderParameters.returnResponse) b else "",
            "headers" to mapHeadersToMap(response.headers())
          )
        )
      )
    } catch (e: Exception) {
      return Failure(createErrorFromThrowabe(ERROR_UNEXPECTED_EXCEPTION, e))
    } catch (e: Error) {
      return Failure(createErrorFromThrowabe(ERROR_UNEXPECTED_ERROR, e))
    }
  }
}
