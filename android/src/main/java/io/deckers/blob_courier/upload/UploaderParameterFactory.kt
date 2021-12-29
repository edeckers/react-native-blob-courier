/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.upload

import android.content.ContentResolver
import android.net.Uri
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import io.deckers.blob_courier.common.DEFAULT_MIME_TYPE
import io.deckers.blob_courier.common.DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.common.DEFAULT_UPLOAD_METHOD
import io.deckers.blob_courier.common.Either
import io.deckers.blob_courier.common.PARAMETER_ABSOLUTE_FILE_PATH
import io.deckers.blob_courier.common.PARAMETER_FILENAME
import io.deckers.blob_courier.common.PARAMETER_HEADERS
import io.deckers.blob_courier.common.PARAMETER_METHOD
import io.deckers.blob_courier.common.PARAMETER_MIME_TYPE
import io.deckers.blob_courier.common.PARAMETER_PART_NAME
import io.deckers.blob_courier.common.PARAMETER_PART_PAYLOAD
import io.deckers.blob_courier.common.PARAMETER_PART_TYPE
import io.deckers.blob_courier.common.PARAMETER_SETTINGS_PROGRESS_INTERVAL
import io.deckers.blob_courier.common.PARAMETER_TASK_ID
import io.deckers.blob_courier.common.PARAMETER_URL
import io.deckers.blob_courier.common.PROVIDED_PARAMETERS
import io.deckers.blob_courier.common.ValidationError
import io.deckers.blob_courier.common.ValidationFailure
import io.deckers.blob_courier.common.ValidationResult
import io.deckers.blob_courier.common.ValidationSuccess
import io.deckers.blob_courier.common.filterHeaders
import io.deckers.blob_courier.common.fold
import io.deckers.blob_courier.common.getMapInt
import io.deckers.blob_courier.common.hasRequiredArrayField
import io.deckers.blob_courier.common.hasRequiredMapField
import io.deckers.blob_courier.common.hasRequiredStringField
import io.deckers.blob_courier.common.ifLeft
import io.deckers.blob_courier.common.ifNone
import io.deckers.blob_courier.common.isNotNull
import io.deckers.blob_courier.common.maybe
import io.deckers.blob_courier.common.popToContext
import io.deckers.blob_courier.common.readContext
import io.deckers.blob_courier.common.testDiscard
import io.deckers.blob_courier.common.testKeep
import io.deckers.blob_courier.common.toEither
import io.deckers.blob_courier.common.validationContext
import java.net.URL
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody

private const val PARAMETER_PARTS = "parts"
private const val PARAMETER_RETURN_RESPONSE = "returnResponse"

data class RequiredParameters(
  val parts: ReadableArray,
  val taskId: String,
  val url: String
)

data class UploaderParameters(
  val taskId: String,
  val parts: List<Part>,
  val uri: URL,
  val method: String,
  val headers: Map<String, String>,
  val returnResponse: Boolean,
  val progressInterval: Int
)

fun UploaderParameters.toMultipartBody(contentResolver: ContentResolver): MultipartBody =
  MultipartBody.Builder()
    .setType(MultipartBody.FORM).let {

      parts.forEach { part ->
        if (part.payload is FilePayload) {
          val payload = part.payload

          payload.run {
            it.addFormDataPart(
              part.name,
              payload.filename,
              InputStreamRequestBody(
                payload.mimeType.toMediaTypeOrNull()
                  ?: DEFAULT_MIME_TYPE.toMediaType(),
                contentResolver,
                payload.absoluteFilePath
              )
            )
          }
        }

        if (part.payload is StringPayload) {
          val payload = part.payload

          payload.run {
            it.addFormDataPart(part.name, payload.value)
          }
        }
      }

      it
    }.build()

private fun filterReadableMapsFromReadableArray(parts: ReadableArray): Array<ReadableMap> =
  (0 until parts.size()).fold(
    emptyArray(),
    { p, i ->
      if (parts.getType(i) == ReadableType.Map)
        // Added linter suppression because from RN0.63 -> RN0.64 the interface changes from
        // @Nullable to @NonNullable, see:
        // https://github.com/edeckers/react-native-blob-courier/issues/180
        //
        // A better solution would probably be to pin the RN version, but that isn't as
        // straight-forward as just pinning a particular Gradle dependency:
        // https://github.com/facebook/react-native/issues/13094#issuecomment-288616901
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        p.plus(parts.getMap(i)!!)
      else p
    }
  )

private fun validatedPartListToEither(validatedParts: List<ValidationResult<Part>>):
  ValidationResult<List<Part>> = validatedParts.toEither()

private fun invalidatePartToEither(invalidatedPart: ValidationResult<Part>):
  ValidationResult<List<Part>> =
    invalidatedPart.map { emptyList<Part>() }
      .fold(
        ::ValidationFailure,
        ::ValidationSuccess
      )

private fun createFilePayload(payload: ReadableMap): ValidationResult<FilePayload> {
  val context = validationContext(payload, isNotNull(PARAMETER_PART_PAYLOAD))
    .fmap(testKeep(hasRequiredStringField(PARAMETER_ABSOLUTE_FILE_PATH)))
    .fmap(testKeep(hasRequiredStringField(PARAMETER_MIME_TYPE)))

  val fileUrl = Uri.parse(payload.getString(PARAMETER_ABSOLUTE_FILE_PATH)!!)
  val fileUrlWithScheme =
    maybe(fileUrl.scheme)
      .map { fileUrl }
      .ifNone(Uri.parse("file://$fileUrl"))

  val errorOrFilename =
    context
      .fmap(testKeep(hasRequiredStringField(PARAMETER_FILENAME)))
      .map(::popToContext)
      .map(::readContext)
      .fold(
        {
          maybe(fileUrl.lastPathSegment)
            .map(::ValidationSuccess)
            .ifNone(
              ValidationFailure(
                ValidationError.MissingParameter(PARAMETER_FILENAME, String::class.java.name)
              )
            )
        },
        ::ValidationSuccess
      )

  val mimeType =
    context
      .fmap(testKeep(hasRequiredStringField(PARAMETER_MIME_TYPE)))
      .map(::popToContext)
      .map(::readContext)
      .ifLeft(DEFAULT_MIME_TYPE)

  return errorOrFilename
    .map { v -> FilePayload(fileUrlWithScheme, v, mimeType) }
}

private fun validateStringPart(part: ReadableMap, name: String): ValidationResult<Part> =
  validationContext(part, isNotNull(PARAMETER_PARTS))
    .fmap(testKeep(hasRequiredStringField(PARAMETER_PART_PAYLOAD)))
    .map(::popToContext)
    .fmap(testDiscard(isNotNull(PARAMETER_PART_PAYLOAD)))
    .map(::readContext)
    .map { stringPayload -> Part(name, StringPayload(stringPayload)) }

private fun validateFilePart(part: ReadableMap, name: String): ValidationResult<Part> =
  validationContext(part, isNotNull(PARAMETER_PARTS))
    .fmap(testKeep(hasRequiredMapField(PARAMETER_PART_PAYLOAD)))
    .map(::popToContext)
    .fmap { (payload, _) -> createFilePayload(payload) }
    .map { filePayload -> Part(name, filePayload) }

private fun validatePart(partMap: ReadableMap?): ValidationResult<Part> =
  validationContext(partMap, isNotNull(PARAMETER_PARTS))
    .fmap(testKeep(hasRequiredStringField(PARAMETER_PART_TYPE)))
    .fmap(testKeep(hasRequiredStringField(PARAMETER_PART_NAME)))
    .fmap { (p, w) ->
      val (name, w2) = w
      val (type, _) = w2

      when (type) {
        "file" -> validateFilePart(p, name)
        else -> validateStringPart(p, name)
      }
    }

private fun validateParts(parts: ReadableArray): ValidationResult<List<Part>> {
  val mapParts = filterReadableMapsFromReadableArray(parts)

  val diff = parts.size() - mapParts.size
  if (diff > 0) {
    return Either.Left(
      ValidationError.InvalidType(
        "parts",
        "ReadableMap[]",
        parts.toArrayList().map { it.javaClass.name }.joinToString { ";" }
      )
    )
  }

  val validatedParts = mapParts.map { validatePart(it) }
  val invalidatedParts = validatedParts.firstOrNull { verification -> verification is Either.Left }

  return maybe(invalidatedParts)
    .fold(
      { validatedPartListToEither(validatedParts) },
      ::invalidatePartToEither
    )
}

private fun verifyRequiredParametersProvided(input: ReadableMap):
  ValidationResult<RequiredParameters> =
    validationContext(input, isNotNull(PROVIDED_PARAMETERS))
      .fmap(testKeep(hasRequiredArrayField(PARAMETER_PARTS)))
      .fmap(testKeep(hasRequiredStringField(PARAMETER_TASK_ID)))
      .fmap(testKeep(hasRequiredStringField(PARAMETER_URL)))
      .fmap { (_, validatedParameters) ->
        val (url, rest) = validatedParameters
        val (taskId, rest2) = rest
        val (parts, _) = rest2

        ValidationSuccess(RequiredParameters(parts, taskId, url))
      }

class UploaderParameterFactory {
  fun fromInput(input: ReadableMap): ValidationResult<UploaderParameters> =
    verifyRequiredParametersProvided(input)
      .fmap {
        val (rawParts, taskId, url) = it

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

        validateParts(rawParts).map { parts ->
          UploaderParameters(
            taskId,
            parts,
            URL(url),
            method,
            headers,
            returnResponse,
            progressInterval
          )
        }
      }
}
