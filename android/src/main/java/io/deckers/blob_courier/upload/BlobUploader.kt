/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.upload

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.common.DEFAULT_MIME_TYPE
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_ERROR
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_EXCEPTION
import io.deckers.blob_courier.common.mapHeadersToMap
import io.deckers.blob_courier.common.toReactMap
import io.deckers.blob_courier.progress.BlobCourierProgressRequest
import okhttp3.MediaType
import okhttp3.MultipartBody
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
    val mpb = MultipartBody.Builder()
      .setType(MultipartBody.FORM)

    uploaderParameters.parts.forEach { part ->
      if (part.payload is FilePart) {
        val payload = part.payload

        payload.run {
          mpb.addFormDataPart(
            part.name,
            payload.filename,
            InputStreamRequestBody(
              payload.mimeType.let { MediaType.parse(it) }
                ?: MediaType.get(DEFAULT_MIME_TYPE),
              reactContext.contentResolver,
              payload.absoluteFilePath
            )
          )
        }
      }

      if (part.payload is DataPart) {
        val payload = part.payload

        payload.run {
          mpb.addFormDataPart(part.name, payload.value)
        }
      }
    }

    val multipartBody = mpb.build()

    val requestBody = BlobCourierProgressRequest(
      reactContext,
      uploaderParameters.taskId,
      multipartBody,
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
