/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.upload

import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.common.mapHeadersToMap
import io.deckers.blob_courier.progress.BlobCourierProgressRequest
import io.deckers.blob_courier.progress.ProgressNotifier
import okhttp3.OkHttpClient
import okhttp3.Request

class BlobUploader(
  private val reactContext: ReactApplicationContext,
  private val httpClient: OkHttpClient,
  private val progressNotifier: ProgressNotifier
) {

  fun upload(uploaderParameters: UploaderParameters): Pair<Throwable?, Map<String, Any>?> {

    val requestBody = BlobCourierProgressRequest(
      uploaderParameters.toMultipartBody(reactContext.contentResolver),
      progressNotifier
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

      return Pair(
        null,
        mapOf(
          "response" to mapOf(
            "code" to response.code(),
            "data" to if (uploaderParameters.returnResponse) b else "",
            "headers" to mapHeadersToMap(response.headers())
          )
        )
      )
    } catch (e: Exception) {
      return Pair(e, null)
    } catch (e: Error) {
      return Pair(e, null)
    }
  }
}
