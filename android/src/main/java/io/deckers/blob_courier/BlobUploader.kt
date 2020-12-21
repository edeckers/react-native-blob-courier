/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import android.net.Uri
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request

class BlobUploader(
  private val reactContext: ReactApplicationContext,
  private val httpClient: OkHttpClient
) {

  fun upload(
    ps: UploaderParameters,
    promise: Promise,
  ) {
    val mpb = MultipartBody.Builder()
      .setType(MultipartBody.FORM)

    ps.verifiedParts.toHashMap().keys.forEach { multipartName ->
      val maybePart = ps.verifiedParts.getMap(multipartName)

      maybePart?.run {
        when (this.getString("type")) {
          "file" -> {
            val payload = this.getMap(PARAMETER_PART_PAYLOAD)!!

            val fileUrl = Uri.parse(payload.getString(PARAMETER_ABSOLUTE_FILE_PATH)!!)
            val fileUrlWithScheme =
              if (fileUrl.scheme == null) Uri.parse("file://$fileUrl") else fileUrl

            val filename =
              if (payload.hasKey(PARAMETER_FILENAME)) (
                payload.getString(PARAMETER_FILENAME)
                  ?: fileUrl.lastPathSegment
                ) else fileUrl.lastPathSegment

            mpb.addFormDataPart(
              multipartName,
              filename,
              InputStreamRequestBody(
                payload.getString(PARAMETER_MIME_TYPE)?.let { MediaType.parse(it) }
                  ?: MediaType.get(DEFAULT_MIME_TYPE),
                reactContext.contentResolver,
                fileUrlWithScheme
              )
            )
          }
          else -> mpb.addFormDataPart(multipartName, this.getString(PARAMETER_PART_PAYLOAD)!!)
        }
      }
    }

    val multipartBody = mpb.build()

    val requestBody = BlobCourierProgressRequest(
      reactContext,
      ps.taskId,
      multipartBody,
      ps.progressInterval
    )

    val requestBuilder = Request.Builder()
      .url(ps.uri)
      .method(ps.method, requestBody)
      .apply {
        ps.headers.forEach { e: Map.Entry<String, String> ->
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
            "data" to if (ps.returnResponse) b else "",
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
