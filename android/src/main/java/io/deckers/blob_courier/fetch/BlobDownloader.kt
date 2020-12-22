/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.fetch

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.progress.BlobCourierProgressResponse
import java.io.File
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

private fun createDownloadProgressInterceptor(
  reactContext: ReactApplicationContext,
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

class BlobDownloader(
  private val reactContext: ReactApplicationContext,
  private val httpClient: OkHttpClient
) {
  enum class TargetDirectoryEnum {
    Cache,
    Data
  }

  private fun createAbsoluteFilePath(
    filename: String,
    targetDirectory: TargetDirectoryEnum
  ) =
    File(
      reactContext.let {
        if (targetDirectory == TargetDirectoryEnum.Data) it.filesDir else it.cacheDir
      },
      filename
    )

  fun download(
    downloaderParameters: DownloaderParameters,
    promise: Promise
  ) {
    val absoluteFilePath = createAbsoluteFilePath(
      downloaderParameters.filename,
      downloaderParameters.targetDirectory
    )

    val progressInterceptor =
      createDownloadProgressInterceptor(
        reactContext,
        downloaderParameters.taskId,
        downloaderParameters.progressInterval
      )

    if (downloaderParameters.useDownloadManager)
      ManagedDownloader(reactContext).fetchBlobUsingDownloadManager(
        downloaderParameters,
        absoluteFilePath,
        downloaderParameters.downloadManagerSettings,
        promise
      )
    else UnmanagedDownloader(httpClient)
      .fetchBlobWithoutDownloadManager(
        downloaderParameters,
        absoluteFilePath,
        progressInterceptor,
        promise
      )
  }
}
