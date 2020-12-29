/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.fetch

import android.net.Uri
import com.facebook.react.bridge.ReadableMap
import io.deckers.blob_courier.common.BlobCourierError
import io.deckers.blob_courier.common.DEFAULT_FETCH_METHOD
import io.deckers.blob_courier.common.DEFAULT_MIME_TYPE
import io.deckers.blob_courier.common.DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.common.ERROR_INVALID_VALUE
import io.deckers.blob_courier.common.Failure
import io.deckers.blob_courier.common.PARAMETER_FILENAME
import io.deckers.blob_courier.common.PARAMETER_HEADERS
import io.deckers.blob_courier.common.PARAMETER_METHOD
import io.deckers.blob_courier.common.PARAMETER_MIME_TYPE
import io.deckers.blob_courier.common.PARAMETER_SETTINGS_PROGRESS_INTERVAL
import io.deckers.blob_courier.common.PARAMETER_TASK_ID
import io.deckers.blob_courier.common.PARAMETER_URL
import io.deckers.blob_courier.common.Result
import io.deckers.blob_courier.common.Success
import io.deckers.blob_courier.common.`do`
import io.deckers.blob_courier.common.filterHeaders
import io.deckers.blob_courier.common.getMapInt
import io.deckers.blob_courier.common.ifLeft
import io.deckers.blob_courier.common.tryRetrieveString
import io.deckers.blob_courier.common.write
import io.deckers.blob_courier.react.processUnexpectedEmptyValue
import java.util.Locale

private const val PARAMETER_ANDROID_SETTINGS = "android"
private const val PARAMETER_DOWNLOAD_MANAGER_SETTINGS = "downloadManager"
private const val PARAMETER_TARGET = "target"
private const val PARAMETER_USE_DOWNLOAD_MANAGER = "useDownloadManager"

@Suppress("SameParameterValue")
private fun processInvalidValue(
  parameterName: String,
  invalidValue: String
) =
  BlobCourierError(
    ERROR_INVALID_VALUE,
    "Parameter `$parameterName` has an invalid value (value=$invalidValue)."
  )

private fun retrieveRequiredParametersOrThrow(input: ReadableMap):
  Triple<String?, String?, String?> =
    tryRetrieveString(input, PARAMETER_FILENAME)
      .pipe(::write)
      .fmap { (_, w) -> w take tryRetrieveString(input, PARAMETER_TASK_ID) }
      .fmap { (_, w) -> w take tryRetrieveString(input, PARAMETER_URL) }
      .map { (_, w) ->
        val (url, rest) = w
        val (taskId, rest2) = rest
        val (filename, _) = rest2

        Triple(filename, taskId, url)
      }
      .ifLeft(Triple(null, null, null))

private fun validateRequiredParameters(
  parameters: Triple<String?, String?, String?>,
): Result<Triple<String, String, String>> {
  val (filename, taskId, url) = parameters

  if (filename == null) {
    return Failure(processUnexpectedEmptyValue(PARAMETER_FILENAME))
  }

  if (taskId == null) {
    return Failure(processUnexpectedEmptyValue(PARAMETER_TASK_ID))
  }

  if (url == null) {
    return Failure(processUnexpectedEmptyValue(PARAMETER_URL))
  }

  return Success(Triple(filename, taskId, url))
}

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

private fun validateParameters(
  parameters: Triple<String, String, String>,
  input: ReadableMap
): Result<DownloaderParameters> {
  val (filename, taskId, url) = parameters

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
      .firstOrNull { t ->
        t.name.toLowerCase(Locale.getDefault()) ==
          targetDirectoryOrFallback.toLowerCase(Locale.getDefault())
      }

  val downloadManagerSettings =
    maybeAndroidSettings?.getMap(PARAMETER_DOWNLOAD_MANAGER_SETTINGS)?.toHashMap().orEmpty()

  val useDownloadManager =
    maybeAndroidSettings?.let { b ->
      b.hasKey(PARAMETER_USE_DOWNLOAD_MANAGER) &&
        b.getBoolean(PARAMETER_USE_DOWNLOAD_MANAGER)
    } ?: false

  val unfilteredHeaders =
    input.getMap(PARAMETER_HEADERS)?.toHashMap() ?: emptyMap<String, Any>()

  val headers = filterHeaders(unfilteredHeaders)

  val progressInterval =
    getMapInt(
      input,
      PARAMETER_SETTINGS_PROGRESS_INTERVAL,
      DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS
    )

  if (maybeTargetDirectory == null) {
    val e = processInvalidValue(PARAMETER_TARGET, targetDirectoryOrFallback)

    return Failure(e)
  }

  return Success(
    DownloaderParameters(
      taskId,
      useDownloadManager,
      downloadManagerSettings,
      Uri.parse(url),
      maybeTargetDirectory,
      filename,
      headers,
      method,
      mimeType,
      progressInterval
    )
  )
}

class DownloaderParameterFactory {
  fun fromInput(input: ReadableMap): Result<DownloaderParameters> {
    val requiredParameters = retrieveRequiredParametersOrThrow(input)

    return validateRequiredParameters(requiredParameters).`do`(
      ::Failure
    ) { v -> validateParameters(v, input) }
  }
}
