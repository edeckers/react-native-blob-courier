/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.send

import android.content.ContentResolver
import android.net.Uri
import com.facebook.react.bridge.ReadableMap
import io.deckers.blob_courier.common.DEFAULT_MIME_TYPE
import io.deckers.blob_courier.common.DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.common.DEFAULT_UPLOAD_METHOD
import io.deckers.blob_courier.common.InputStreamRequestBody
import io.deckers.blob_courier.common.PARAMETER_ABSOLUTE_FILE_PATH
import io.deckers.blob_courier.common.PARAMETER_HEADERS
import io.deckers.blob_courier.common.PARAMETER_METHOD
import io.deckers.blob_courier.common.PARAMETER_MIME_TYPE
import io.deckers.blob_courier.common.PARAMETER_SETTINGS_PROGRESS_INTERVAL
import io.deckers.blob_courier.common.PARAMETER_TASK_ID
import io.deckers.blob_courier.common.PARAMETER_URL
import io.deckers.blob_courier.common.PROVIDED_PARAMETERS
import io.deckers.blob_courier.common.ValidationResult
import io.deckers.blob_courier.common.ValidationSuccess
import io.deckers.blob_courier.common.filterHeaders
import io.deckers.blob_courier.common.getMapInt
import io.deckers.blob_courier.common.hasRequiredStringField
import io.deckers.blob_courier.common.ifNone
import io.deckers.blob_courier.common.isNotNull
import io.deckers.blob_courier.common.maybe
import io.deckers.blob_courier.common.right
import io.deckers.blob_courier.common.testKeep
import io.deckers.blob_courier.common.validationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import java.net.URL

private const val PARAMETER_RETURN_RESPONSE = "returnResponse"

data class RequiredParameters(
  val taskId: String,
  val url: String,
  val absoluteFilePath: String,
)

data class SenderParameters(
  val absoluteFilePath: Uri,
  val mediaType: String,
  val method: String,
  val headers: Map<String, String>,
  val progressInterval: Int,
  val returnResponse: Boolean,
  val taskId: String,
  val uri: URL,
)

fun SenderParameters.toRequestBody(contentResolver: ContentResolver): RequestBody =
  InputStreamRequestBody(
    mediaType.toMediaTypeOrNull()
      ?: DEFAULT_MIME_TYPE.toMediaType(),
    contentResolver,
    absoluteFilePath
  )

private fun verifyRequiredParametersProvided(input: ReadableMap):
  ValidationResult<RequiredParameters> =
  validationContext(input, isNotNull(PROVIDED_PARAMETERS))
    .fmap(testKeep(hasRequiredStringField(PARAMETER_ABSOLUTE_FILE_PATH)))
    .fmap(testKeep(hasRequiredStringField(PARAMETER_TASK_ID)))
    .fmap(testKeep(hasRequiredStringField(PARAMETER_URL)))
    .fmap { (_, validatedParameters) ->
      val (url, rest) = validatedParameters
      val (taskId, rest2) = rest
      val (absoluteFilePath, _) = rest2

      ValidationSuccess(RequiredParameters(taskId, url, absoluteFilePath))
    }

class SenderParameterFactory {
  fun fromInput(input: ReadableMap): ValidationResult<SenderParameters> =
    verifyRequiredParametersProvided(input)
      .fmap {
        val (taskId, url, absoluteFilePath) = it

        val mediaType = maybe(input.getString(PARAMETER_MIME_TYPE)).ifNone(DEFAULT_MIME_TYPE)
        val method = maybe(input.getString(PARAMETER_METHOD)).ifNone(DEFAULT_UPLOAD_METHOD)

        val unfilteredHeaders =
          input.getMap(PARAMETER_HEADERS)?.toHashMap() ?: emptyMap<String, Any>()

        val headers = filterHeaders(unfilteredHeaders)

        val returnResponse =
          input.hasKey(PARAMETER_RETURN_RESPONSE) && input.getBoolean(PARAMETER_RETURN_RESPONSE)

        val progressInterval =
          getMapInt(
            input,
            PARAMETER_SETTINGS_PROGRESS_INTERVAL,
            DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS
          )

        right(SenderParameters(
          Uri.parse(absoluteFilePath),
          mediaType,
          method,
          headers,
          progressInterval,
          returnResponse,
          taskId,
          URL(url),
        ))
      }
}
