/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.send

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

private val TAG = BlobSender::class.java.name

private val logger = Logger(TAG)
private fun li(m: String) = logger.i(m)

class BlobSender(
  private val context: Context,
  private val httpClient: OkHttpClient,
  private val progressNotifierFactory: ProgressNotifierFactory
) {

  fun send(senderParameters: SenderParameters): Result<Map<String, Any>> {
    li("Starting unmanaged send")

    val requestBody = BlobCourierProgressRequest(
      senderParameters.toRequestBody(context.contentResolver),
      progressNotifierFactory.create(senderParameters.taskId)
    )

    val requestBuilder = Request.Builder()
      .url(senderParameters.uri)
      .method(senderParameters.method, requestBody)
      .apply {
        senderParameters.headers.forEach { e: Map.Entry<String, String> ->
          addHeader(e.key, e.value)
        }
      }
      .build()

    val sendRequestCall = httpClient.newCall(requestBuilder)

    try {
      registerCancellationHandler(context, senderParameters.taskId, sendRequestCall)

      val response = sendRequestCall.execute()

      val responseBody = response.body?.string().orEmpty()

      li("Finished unmanaged send")

      return Success(
        mapOf(
          "response" to mapOf(
            "code" to response.code,
            "data" to if (senderParameters.returnResponse) responseBody else "",
            "headers" to mapHeadersToMap(response.headers)
          )
        )
      )
    } catch (e: IOException) {
      if (sendRequestCall.isCanceled()) {
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
