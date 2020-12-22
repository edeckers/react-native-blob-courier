/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.fetch

import android.net.Uri
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.common.DOWNLOAD_TYPE_UNMANAGED
import io.deckers.blob_courier.common.mapHeadersToMap
import io.deckers.blob_courier.common.toReactMap
import io.deckers.blob_courier.progress.BlobCourierProgressResponse
import java.io.File
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Okio
import okio.Source

class UnmanagedDownloader(
  private val reactContext: ReactApplicationContext,
  private val httpClient: OkHttpClient
) {

  private fun createDownloadProgressInterceptor(
    taskId: String,
    progressInterval: Int
  ): (
    Interceptor.Chain
  ) -> Response = fun(
    chain: Interceptor.Chain
  ): Response {
    val originalResponse = chain.proceed(chain.request())

    return originalResponse.body()?.let {
      originalResponse.newBuilder().body(
        BlobCourierProgressResponse(
          reactContext,
          taskId,
          progressInterval,
          it
        )
      ).build()
    } ?: originalResponse
  }

  fun fetch(
    downloaderParameters: DownloaderParameters,
    toAbsoluteFilePath: File,
    promise: Promise
  ) {
    val request = Request.Builder()
      .method(downloaderParameters.method, null)
      .url(downloaderParameters.uri.toString())
      .apply {
        downloaderParameters.headers.forEach { e: Map.Entry<String, String> ->
          addHeader(e.key, e.value)
        }
      }
      .build()

    val progressInterceptor =
      createDownloadProgressInterceptor(
        downloaderParameters.taskId,
        downloaderParameters.progressInterval
      )

    val httpClientWithInterceptor =
      httpClient.newBuilder()
        .addInterceptor(progressInterceptor)
        .build()

    httpClientWithInterceptor.newCall(request).execute().use { response ->
      response.body()?.source().use { source ->
        Okio.buffer(Okio.sink(toAbsoluteFilePath)).use { sink ->

          sink.writeAll(source as Source)
        }
      }

      promise.resolve(
        mapOf(
          "type" to DOWNLOAD_TYPE_UNMANAGED,
          "data" to mapOf(
            "absoluteFilePath" to Uri.fromFile(toAbsoluteFilePath),
            "response" to mapOf(
              "code" to response.code(),
              "headers" to mapHeadersToMap(response.headers())
            )
          )
        ).toReactMap()
      )
    }
  }
}
