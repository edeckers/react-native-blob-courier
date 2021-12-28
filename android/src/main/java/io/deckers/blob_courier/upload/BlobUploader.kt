/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.upload

import android.content.Context
import io.deckers.blob_courier.cancel.registerCancellationHandler
import io.deckers.blob_courier.common.ERROR_CANCELED_EXCEPTION
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_ERROR
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_EXCEPTION
import io.deckers.blob_courier.common.Failure
import io.deckers.blob_courier.common.Logger
import io.deckers.blob_courier.common.Result
import io.deckers.blob_courier.common.Success
import io.deckers.blob_courier.common.createErrorFromThrowable
import io.deckers.blob_courier.common.mapHeadersToMap
import io.deckers.blob_courier.progress.BlobCourierProgressRequest
import io.deckers.blob_courier.progress.ProgressNotifierFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

private val TAG = BlobUploader::class.java.name

private val logger = Logger(TAG)
private fun li(m: String) = logger.i(m)

class BlobUploader(
  private val context: Context,
  private val httpClient: OkHttpClient,
  private val progressNotifierFactory: ProgressNotifierFactory
) {

  fun upload(uploaderParameters: UploaderParameters): Result<Map<String, Any>> {
    li("Starting unmanaged upload")

    val requestBody = BlobCourierProgressRequest(
      uploaderParameters.toMultipartBody(context.contentResolver),
      progressNotifierFactory.create(uploaderParameters.taskId)
    )

    val requestBuilder = Request.Builder()
      .url(uploaderParameters.uri)
      .method(uploaderParameters.method, requestBody)
      .apply {
        uploaderParameters.headers.forEach { (name, value) ->
          addHeader(name, value)
        }
      }
      .build()

    val uploadRequestCall = httpClient.newCall(requestBuilder)

    try {
      registerCancellationHandler(context, uploaderParameters.taskId, uploadRequestCall)

      val response = uploadRequestCall.execute()

      val responseBody = response.body?.string().orEmpty()

      li("Finished unmanaged upload")

      return Success(
        mapOf(
          "response" to mapOf(
            "code" to response.code,
            "data" to if (uploaderParameters.returnResponse) responseBody else "",
            "headers" to mapHeadersToMap(response.headers)
          )
        )
      )
    } catch (e: IOException) {
      if (uploadRequestCall.isCanceled()) {
        return Failure(createErrorFromThrowable(ERROR_CANCELED_EXCEPTION, e))
      }

      return Failure(createErrorFromThrowable(ERROR_UNEXPECTED_EXCEPTION, e))
    } catch (e: Exception) {
      return Failure(createErrorFromThrowable(ERROR_UNEXPECTED_EXCEPTION, e))
    } catch (e: Error) {
      return Failure(createErrorFromThrowable(ERROR_UNEXPECTED_ERROR, e))
    }
  }
}
