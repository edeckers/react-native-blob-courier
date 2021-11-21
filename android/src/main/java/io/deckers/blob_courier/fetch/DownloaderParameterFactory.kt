/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.fetch

import android.net.Uri
import com.facebook.react.bridge.ReadableMap
import io.deckers.blob_courier.common.DEFAULT_FETCH_METHOD
import io.deckers.blob_courier.common.DEFAULT_MIME_TYPE
import io.deckers.blob_courier.common.DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.common.PARAMETER_FILENAME
import io.deckers.blob_courier.common.PARAMETER_HEADERS
import io.deckers.blob_courier.common.PARAMETER_METHOD
import io.deckers.blob_courier.common.PARAMETER_MIME_TYPE
import io.deckers.blob_courier.common.PARAMETER_SETTINGS_PROGRESS_INTERVAL
import io.deckers.blob_courier.common.PARAMETER_TASK_ID
import io.deckers.blob_courier.common.PARAMETER_URL
import io.deckers.blob_courier.common.PROVIDED_PARAMETERS
import io.deckers.blob_courier.common.ValidationError
import io.deckers.blob_courier.common.ValidationFailure
import io.deckers.blob_courier.common.ValidationResult
import io.deckers.blob_courier.common.ValidationSuccess
import io.deckers.blob_courier.common.filterHeaders
import io.deckers.blob_courier.common.getMapInt
import io.deckers.blob_courier.common.hasRequiredStringField
import io.deckers.blob_courier.common.ifNone
import io.deckers.blob_courier.common.isNotNull
import io.deckers.blob_courier.common.maybe
import io.deckers.blob_courier.common.testKeep
import io.deckers.blob_courier.common.validationContext

private const val PARAMETER_ANDROID_SETTINGS = "android"
private const val PARAMETER_DOWNLOAD_MANAGER_SETTINGS = "downloadManager"
private const val PARAMETER_TARGET = "target"
private const val PARAMETER_USE_DOWNLOAD_MANAGER = "useDownloadManager"

data class RequiredDownloadParameters(
  val filename: String,
  val taskId: String,
  val url: String,
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

private fun validateRequiredParameters(input: ReadableMap):
  ValidationResult<RequiredDownloadParameters> =
    validationContext(input, isNotNull(PROVIDED_PARAMETERS))
      .fmap(testKeep(hasRequiredStringField(PARAMETER_FILENAME)))
      .fmap(testKeep(hasRequiredStringField(PARAMETER_TASK_ID)))
      .fmap(testKeep(hasRequiredStringField(PARAMETER_URL)))
      .fmap { (_, validatedParameters) ->
        val (url, rest) = validatedParameters
        val (taskId, rest2) = rest
        val (filename, _) = rest2

        ValidationSuccess(RequiredDownloadParameters(filename, taskId, url))
      }

private fun validateParameters(
  parameters: RequiredDownloadParameters,
  input: ReadableMap
): ValidationResult<DownloaderParameters> {
  val (filename, taskId, url) = parameters

  val method = maybe(input.getString(PARAMETER_METHOD)).ifNone(DEFAULT_FETCH_METHOD)
  val mimeType = maybe(input.getString(PARAMETER_MIME_TYPE)).ifNone(DEFAULT_MIME_TYPE)

  val maybeAndroidSettings = input.getMap(PARAMETER_ANDROID_SETTINGS)

  val targetDirectoryOrFallback = (
    maybeAndroidSettings?.getString(PARAMETER_TARGET)
      ?: BlobDownloader.TargetDirectoryEnum.Cache.toString()
    )

  val maybeTargetDirectory =
    BlobDownloader.TargetDirectoryEnum
      .values()
      .firstOrNull { t ->
        t.name.equals(targetDirectoryOrFallback, ignoreCase = true)
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

  return maybe(maybeTargetDirectory)
    .map { targetDirectory ->
      ValidationSuccess(
        DownloaderParameters(
          taskId,
          useDownloadManager,
          downloadManagerSettings,
          Uri.parse(url),
          targetDirectory,
          filename,
          headers,
          method,
          mimeType,
          progressInterval
        )
      )
    }.ifNone(
      ValidationFailure(
        ValidationError.InvalidValue(PARAMETER_TARGET, targetDirectoryOrFallback)
      )
    )
}

class DownloaderParameterFactory {
  fun fromInput(input: ReadableMap): ValidationResult<DownloaderParameters> =
    validateRequiredParameters(input).fmap { v -> validateParameters(v, input) }
}
