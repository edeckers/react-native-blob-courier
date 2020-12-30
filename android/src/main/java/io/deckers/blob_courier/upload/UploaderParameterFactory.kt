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
import io.deckers.blob_courier.common.ValidationError
import io.deckers.blob_courier.common.ValidationFailure
import io.deckers.blob_courier.common.ValidationResult
import io.deckers.blob_courier.common.ValidationSuccess
import io.deckers.blob_courier.common.filterHeaders
import io.deckers.blob_courier.common.fold
import io.deckers.blob_courier.common.getMapInt
import io.deckers.blob_courier.common.hasArrayReqParam
import io.deckers.blob_courier.common.hasKey
import io.deckers.blob_courier.common.hasMapReqParam
import io.deckers.blob_courier.common.hasStringReqParam
import io.deckers.blob_courier.common.ifLeft
import io.deckers.blob_courier.common.ifNone
import io.deckers.blob_courier.common.isNotNull
import io.deckers.blob_courier.common.maybe
import io.deckers.blob_courier.common.right
import io.deckers.blob_courier.common.testDrop
import io.deckers.blob_courier.common.testTake
import io.deckers.blob_courier.common.validate
import io.deckers.blob_courier.common.write
import java.net.URL
import okhttp3.MediaType
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
        if (part.payload is FilePart) {
          val payload = part.payload

          payload.run {
            it.addFormDataPart(
              part.name,
              payload.filename,
              InputStreamRequestBody(
                payload.mimeType.let(MediaType::parse)
                  ?: MediaType.get(DEFAULT_MIME_TYPE),
                contentResolver,
                payload.absoluteFilePath
              )
            )
          }
        }

        if (part.payload is DataPart) {
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
        p.plus(parts.getMap(i)!!)
      else p
    }
  )

private fun validatedPartListToEither(validatedParts: List<ValidationResult<ReadableMap>>):
  ValidationResult<List<ReadableMap>> =
    validatedParts.fold(
      right(emptyList()),
      { listOfValidMaps, validMap ->
        validMap.fold(
          { listOfValidMaps },
          { m -> listOfValidMaps.map { p0 -> p0.plus(m) } }
        )
      }
    )

private fun invalidatePartToEither(invalidatedPart: ValidationResult<ReadableMap>):
  ValidationResult<List<ReadableMap>> =
    invalidatedPart.map { emptyList<ReadableMap>() }
      .fold(
        ::ValidationFailure,
        ::ValidationSuccess
      )

private fun verifyParts(parts: ReadableArray): ValidationResult<List<ReadableMap>> {
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

  val validatedParts = mapParts.map(::validatePartsMap)
  val invalidatedParts = validatedParts.firstOrNull { verification -> verification is Either.Left }

  return maybe(invalidatedParts)
    .fold(
      { validatedPartListToEither(validatedParts) },
      ::invalidatePartToEither
    )
}

private fun createFilePayload(payload: ReadableMap): FilePart {
  val fileUrl = Uri.parse(payload.getString(PARAMETER_ABSOLUTE_FILE_PATH)!!)
  val fileUrlWithScheme =
    maybe(fileUrl.scheme)
      .map { fileUrl }
      .ifNone(Uri.parse("file://$fileUrl"))

  val errorOrFilename = ValidationSuccess(payload)
    .pipe(::write)
    .fmap(testTake(hasStringReqParam(PARAMETER_FILENAME)))
    .map { (_, w) -> w }
    .map { (f, _) -> f }

  val mimeType = ValidationSuccess(payload)
    .pipe(::write)
    .fmap(testTake(hasStringReqParam(PARAMETER_MIME_TYPE)))
    .map { (_, w) -> w }
    .map { (mime, _) -> mime }
    .ifLeft(DEFAULT_MIME_TYPE)

  return errorOrFilename
    .map { v -> FilePart(fileUrlWithScheme, v, mimeType) }
    .ifLeft(FilePart(fileUrl, "TESTTESTTEST", mimeType))
}

private fun createPart(part: ReadableMap): Part {
  val name = part.getString(PARAMETER_PART_NAME)!!

  val payload = when (part.getString(PARAMETER_PART_TYPE)) {
    "file" -> part.getMap(PARAMETER_PART_PAYLOAD)!!.let(::createFilePayload)
    else -> DataPart(part.getString(PARAMETER_PART_PAYLOAD) ?: "")
  }

  return Part(name, payload)
}

private fun createParts(parts: ReadableArray): List<Part> =
  filterReadableMapsFromReadableArray(parts).map(::createPart)

private fun isValidFilePayload(map: ReadableMap?):
  ValidationResult<ReadableMap> =
    ValidationSuccess(map)
      .pipe(::write)
      .fmap(testTake(isNotNull(PARAMETER_PARTS)))
      .fmap(testTake(hasMapReqParam((PARAMETER_PART_PAYLOAD))))
      .map { (_, w) ->
        Pair(w.v.first, w)
      }
      .fmap(testDrop(hasStringReqParam(PARAMETER_ABSOLUTE_FILE_PATH)))
      .fmap(testDrop(hasStringReqParam(PARAMETER_MIME_TYPE)))
      .map { (_, w) ->
        val (_, w2) = w

        val (nonNullPart) = w2

        nonNullPart
      }

private fun isValidStringPayload(part: ReadableMap?):
  ValidationResult<ReadableMap> = validate(part, hasKey(PARAMETER_PART_PAYLOAD))
    .pipe(::write)
    .fmap(testDrop(hasStringReqParam(PARAMETER_PART_PAYLOAD)))
    .map { it.first }

private fun isValidPayload(map: ReadableMap?): ValidationResult<ReadableMap> =
  if (map?.getString(PARAMETER_PART_TYPE) == "file")
    isValidFilePayload(map)
  else isValidStringPayload(map)

private fun validatePartsMap(value: ReadableMap?): ValidationResult<ReadableMap> =
  validate(value, isNotNull(PARAMETER_PARTS))
    .pipe(::write)
    .fmap(testDrop(hasStringReqParam(PARAMETER_PART_NAME)))
    .fmap(testDrop(isNotNull(PARAMETER_PART_PAYLOAD)))
    .fmap(testDrop(hasStringReqParam(PARAMETER_PART_TYPE)))
    .fmap(testDrop(::isValidPayload))
    .map { it.first }

private fun validateRequiredParameters(input: ReadableMap): ValidationResult<RequiredParameters> =
  ValidationSuccess(input)
    .pipe(::write)
    .fmap(testTake(hasArrayReqParam(PARAMETER_PARTS)))
    .fmap(testTake(hasStringReqParam(PARAMETER_TASK_ID)))
    .fmap(testTake(hasStringReqParam(PARAMETER_URL)))
    .fmap { (_, validatedParameters) ->
      val (url, rest) = validatedParameters
      val (taskId, rest2) = rest
      val (parts, _) = rest2

      ValidationSuccess(RequiredParameters(parts, taskId, url))
    }

class UploaderParameterFactory {
  fun fromInput(input: ReadableMap): ValidationResult<UploaderParameters> =
    validateRequiredParameters(input)
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

        verifyParts(rawParts).map {
          val parts = createParts(rawParts)

          val uri = URL(url)

          UploaderParameters(
            taskId,
            parts,
            uri,
            method,
            headers,
            returnResponse,
            progressInterval
          )
        }
      }
}
