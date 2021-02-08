/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.fetch

import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.common.Result
import io.deckers.blob_courier.progress.ProgressNotifierFactory
import java.io.File
import okhttp3.OkHttpClient

class BlobDownloader(
  private val reactContext: ReactApplicationContext,
  private val httpClient: OkHttpClient,
  private val progressNotifierFactory: ProgressNotifierFactory
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

  fun download(downloaderParameters: DownloaderParameters): Result<Map<String, Any>> {
    val absoluteFilePath = createAbsoluteFilePath(
      downloaderParameters.filename,
      downloaderParameters.targetDirectory
    )

    val progressNotifier = progressNotifierFactory.create(downloaderParameters.taskId)

    return if (downloaderParameters.useDownloadManager)
      ManagedDownloader(reactContext, progressNotifier)
        .fetch(downloaderParameters, absoluteFilePath)
    else
      UnmanagedDownloader(reactContext, httpClient, progressNotifier)
        .fetch(downloaderParameters, absoluteFilePath)
  }
}
