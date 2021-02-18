/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.fetch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.deckers.blob_courier.common.ACTION_CANCEL_REQUEST
import io.deckers.blob_courier.common.DOWNLOAD_TYPE_UNMANAGED
import io.deckers.blob_courier.common.ERROR_CANCELED_EXCEPTION
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_ERROR
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_EXCEPTION
import io.deckers.blob_courier.common.Failure
import io.deckers.blob_courier.common.Logger
import io.deckers.blob_courier.common.Result
import io.deckers.blob_courier.common.Success
import io.deckers.blob_courier.common.createErrorFromThrowable
import io.deckers.blob_courier.common.mapHeadersToMap
import io.deckers.blob_courier.progress.BlobCourierProgressResponse
import io.deckers.blob_courier.progress.ProgressNotifier
import okhttp3.Call
import java.io.File
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Okio
import okio.Source
import java.io.IOException

private val TAG = UnmanagedDownloader::class.java.name

private val logger = Logger(TAG)
private fun li(m: String) = logger.i(m)
private fun lv(m: String, e: Throwable? = null) = logger.v(m, e)

class UnmanagedDownloader(
  private val context: Context,
  private val httpClient: OkHttpClient,
  private val progressNotifier: ProgressNotifier
) {
  fun fetch(
    downloaderParameters: DownloaderParameters,
    toAbsoluteFilePath: File,
  ): Result<Map<String, Any>> {
    li("Starting unmanaged fetch")

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
      createDownloadProgressInterceptor(progressNotifier)

    val httpClientWithInterceptor =
      httpClient.newBuilder()
        .addInterceptor(progressInterceptor)
        .build()

    val call = httpClientWithInterceptor.newCall(request)

    try {
      registerCancellationHandler(downloaderParameters.taskId, call)

      val response = call.execute()

      response.body()?.source().use { source ->
        Okio.buffer(Okio.sink(toAbsoluteFilePath)).use { sink ->

          sink.writeAll(source as Source)
        }
      }

      li("Finished unmanaged fetch")

      return Success(
        mapOf(
          "type" to DOWNLOAD_TYPE_UNMANAGED,
          "data" to mapOf(
            "absoluteFilePath" to Uri.fromFile(toAbsoluteFilePath),
            "response" to mapOf(
              "code" to response.code(),
              "headers" to mapHeadersToMap(response.headers())
            )
          )
        )
      )
    } catch (e: IOException) {
      val code = if (call.isCanceled) ERROR_CANCELED_EXCEPTION else ERROR_UNEXPECTED_ERROR

      return Failure(createErrorFromThrowable(code, e))
    } catch (e: Throwable) {
      return Failure(createErrorFromThrowable(ERROR_UNEXPECTED_ERROR, e))
    }
  }

  private fun registerCancellationHandler(taskId: String, call: Call) {
    lv("Registering $ACTION_CANCEL_REQUEST receiver")

    LocalBroadcastManager.getInstance(context)
      .registerReceiver(object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
          if (p1?.getStringExtra("taskId") != taskId) {
            return
          }

          call.cancel()
        }
      }, IntentFilter(ACTION_CANCEL_REQUEST))

    lv("Registered $ACTION_CANCEL_REQUEST receiver")
  }

  private fun createDownloadProgressInterceptor(
    progressNotifier: ProgressNotifier
  ): (
    Interceptor.Chain
  ) -> Response = fun(
    chain: Interceptor.Chain
  ): Response {
    val originalResponse = chain.proceed(chain.request())

    return originalResponse.body()?.let {
      originalResponse.newBuilder().body(
        BlobCourierProgressResponse(progressNotifier, it)
      ).build()
    } ?: originalResponse
  }
}
