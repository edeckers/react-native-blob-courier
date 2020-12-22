/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.fetch

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import java.io.File
import okhttp3.OkHttpClient

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

    if (downloaderParameters.useDownloadManager)
      ManagedDownloader(reactContext)
        .fetch(downloaderParameters, absoluteFilePath, promise)
    else UnmanagedDownloader(reactContext, httpClient)
      .fetch(downloaderParameters, absoluteFilePath, promise)
  }
}
