/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import android.net.Uri
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import java.util.Locale

@Suppress("SameParameterValue")
private fun processInvalidValue(
  promise: Promise,
  parameterName: String,
  invalidValue: String
) =
  promise.reject(
    ERROR_INVALID_VALUE,
    "Parameter `$parameterName` has an invalid value (value=$invalidValue)."
  )

data class DownloaderParameters(
  val taskId: String,
  val useDownloadManager: Boolean,
  val downloadManagerSettings: Map<String, Any>,
  val uri: Uri,
  val targetDirectory: BlobDownloader.TargetDirectoryEnum,
  val filename: String,
  val headers: Map<String, String>,
  val method: String,
  val mimeType: String,
  val progressInterval: Int
)

class DownloaderParameterFactory {
  fun fromInput(input: ReadableMap, promise: Promise): DownloaderParameters? {
    assertRequiredParameter(input, String::class.java, PARAMETER_TASK_ID)
    assertRequiredParameter(input, String::class.java, PARAMETER_FILENAME)
    assertRequiredParameter(input, String::class.java, PARAMETER_URL)
    val maybeTaskId = input.getString(PARAMETER_TASK_ID)
    val maybeFilename = input.getString(PARAMETER_FILENAME)
    val maybeUrl = input.getString(PARAMETER_URL)

    val method = input.getString(PARAMETER_METHOD) ?: DEFAULT_FETCH_METHOD
    val mimeType = input.getString(PARAMETER_MIME_TYPE) ?: DEFAULT_MIME_TYPE

    val maybeAndroidSettings = input.getMap(PARAMETER_ANDROID_SETTINGS)

    val targetDirectoryOrFallback = (
      maybeAndroidSettings?.getString(PARAMETER_TARGET)
        ?: BlobDownloader.TargetDirectoryEnum.Cache.toString()
      )

    val maybeTargetDirectory =
      BlobDownloader.TargetDirectoryEnum
        .values()
        .firstOrNull {
          it.name.toLowerCase(Locale.getDefault()) ==
            targetDirectoryOrFallback.toLowerCase(Locale.getDefault())
        }

    val downloadManagerSettings =
      maybeAndroidSettings?.getMap(PARAMETER_DOWNLOAD_MANAGER_SETTINGS)?.toHashMap().orEmpty()

    val useDownloadManager =
      maybeAndroidSettings?.hasKey(PARAMETER_USE_DOWNLOAD_MANAGER) ?: false

    val unfilteredHeaders =
      input.getMap(PARAMETER_HEADERS)?.toHashMap() ?: emptyMap<String, Any>()

    val headers = filterHeaders(unfilteredHeaders)

    val progressInterval =
      getMapInt(input, PARAMETER_SETTINGS_PROGRESS_INTERVAL, DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS)

    if (maybeTaskId.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_TASK_ID)

      return null
    }

    if (maybeFilename.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_FILENAME)

      return null
    }

    if (maybeUrl.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_URL)

      return null
    }

    if (maybeTargetDirectory == null) {
      processInvalidValue(promise, PARAMETER_TARGET, targetDirectoryOrFallback)

      return null
    }

    return DownloaderParameters(
      maybeTaskId,
      useDownloadManager,
      downloadManagerSettings,
      Uri.parse(maybeUrl),
      maybeTargetDirectory,
      maybeFilename,
      headers,
      method,
      mimeType,
      progressInterval
    )
  }
}
