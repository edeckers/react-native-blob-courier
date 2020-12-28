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
import io.deckers.blob_courier.common.BlobCourierError
import io.deckers.blob_courier.common.DEFAULT_MIME_TYPE
import io.deckers.blob_courier.common.DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.common.DEFAULT_UPLOAD_METHOD
import io.deckers.blob_courier.common.ERROR_INVALID_VALUE
import io.deckers.blob_courier.common.Either
import io.deckers.blob_courier.common.Failure
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
import io.deckers.blob_courier.common.Result
import io.deckers.blob_courier.common.Success
import io.deckers.blob_courier.common.cons
import io.deckers.blob_courier.common.filterHeaders
import io.deckers.blob_courier.common.fold
import io.deckers.blob_courier.common.getMapInt
import io.deckers.blob_courier.common.isNotNull
import io.deckers.blob_courier.common.isNotNullOrEmpty
import io.deckers.blob_courier.common.left
import io.deckers.blob_courier.common.right
import io.deckers.blob_courier.common.tryRetrieveArray
import io.deckers.blob_courier.common.tryRetrieveString
import io.deckers.blob_courier.common.validateParameter
import io.deckers.blob_courier.react.processUnexpectedEmptyValue
import java.net.URL
import okhttp3.MediaType
import okhttp3.MultipartBody

private const val PARAMETER_PARTS = "parts"
private const val PARAMETER_RETURN_RESPONSE = "returnResponse"

// private fun verifyFilePart(part: ReadableMap): Triple<Boolean, String, String> {
//   if (!part.hasKey(PARAMETER_PART_PAYLOAD)) {
//     return Triple(false, ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_PART_PAYLOAD")
//   }
//
//   val payload = part.getMap(PARAMETER_PART_PAYLOAD)!!
//   if (!payload.hasKey(PARAMETER_ABSOLUTE_FILE_PATH)) {
//     return Triple(false, ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_ABSOLUTE_FILE_PATH")
//   }
//
//   if (!payload.hasKey(PARAMETER_MIME_TYPE)) {
//     return Triple(false, ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_MIME_TYPE")
//   }
//
//   if (payload.getString(PARAMETER_ABSOLUTE_FILE_PATH).isNullOrEmpty()) {
//     return Triple(false, ERROR_UNEXPECTED_EMPTY_VALUE, "part.$PARAMETER_ABSOLUTE_FILE_PATH")
//   }
//
//   if (payload.getString(PARAMETER_MIME_TYPE).isNullOrEmpty()) {
//     return Triple(false, ERROR_UNEXPECTED_EMPTY_VALUE, "part.$PARAMETER_MIME_TYPE")
//   }
//
//   return Triple(true, "", "")
// }

// private fun verifyStringPart(part: ReadableMap): Triple<Boolean, String, String> {
//   if (part.hasKey(PARAMETER_PART_PAYLOAD)) {
//     return Triple(true, "", "")
//   }
//
//   return Triple(false, ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_PART_PAYLOAD")
// }

// private fun verifyPart(part: ReadableMap?): Triple<Boolean, String, String> {
//   if (part == null) {
//     return Triple(false, ERROR_UNEXPECTED_EMPTY_VALUE, "part")
//   }
//
//   if (!part.hasKey(PARAMETER_PART_NAME)) {
//     return Triple(false, ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_PART_NAME")
//   }
//
//   if (!part.hasKey(PARAMETER_PART_PAYLOAD)) {
//     return Triple(false, ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_PART_PAYLOAD")
//   }
//
//   if (!part.hasKey(PARAMETER_PART_TYPE)) {
//     return Triple(false, ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_PART_TYPE")
//   }
//
//   if (part.getString(PARAMETER_PART_TYPE) == "file") {
//     return verifyFilePart(part)
//   }
//
//   return verifyStringPart(part)
// }

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

private fun verifyParts(parts: ReadableArray): Either<BlobCourierError, Unit> {
  val mapParts = filterReadableMapsFromReadableArray(parts)

  val diff = parts.size() - mapParts.size
  if (diff > 0) {
    return Either.Left(
      BlobCourierError(
        ERROR_INVALID_VALUE,
        "$diff provided part(s) are not ReadableMap objects"
      )
    )
  }

  val failedPartVerifications =
    mapParts.map(::validatePartsMap).filter { verification -> verification is Either.Left }
  if (failedPartVerifications.isNotEmpty()) {
    val firstFailure = failedPartVerifications.first() as Either.Left

    return Either.Left(BlobCourierError("CODE", firstFailure.v.message ?: ""))
  }

  return Either.Right(Unit)
}

private fun createFilePayload(payload: ReadableMap): FilePart? {
  val fileUrl = Uri.parse(payload.getString(PARAMETER_ABSOLUTE_FILE_PATH)!!)
  val fileUrlWithScheme =
    if (fileUrl.scheme == null) Uri.parse("file://$fileUrl") else fileUrl

  val filename =
    if (payload.hasKey(PARAMETER_FILENAME)) (
      payload.getString(PARAMETER_FILENAME)
        ?: fileUrl.lastPathSegment
      ) else fileUrl.lastPathSegment
  val mimeType = payload.getString(PARAMETER_MIME_TYPE)!!

  if (fileUrlWithScheme == null) {
    return null
  }

  if (filename == null) {
    return null
  }

  return FilePart(fileUrlWithScheme, filename, mimeType)
}

private fun createPart(part: ReadableMap): Part {
  val name = part.getString(PARAMETER_PART_NAME)!!

  val payload = when (part.getString(PARAMETER_PART_TYPE)) {
    "file" -> part.getMap(PARAMETER_PART_PAYLOAD)?.let(::createFilePayload)
    else -> DataPart(part.getString(PARAMETER_PART_PAYLOAD) ?: "")
  }!!

  return Part(name, payload)
}

private fun createParts(parts: ReadableArray): List<Part> =
  filterReadableMapsFromReadableArray(parts).map(::createPart)

private fun retrieveRequiredParameters(input: ReadableMap):
  Result<RequiredParameters> {
    val rawParts = tryRetrieveArray(input, PARAMETER_PARTS)
    val taskId = tryRetrieveString(input, PARAMETER_TASK_ID)
    val url = tryRetrieveString(input, PARAMETER_URL)

    return rawParts.map { RequiredParameters(it, taskId, url) }
  }

data class RequiredParameters(val parts: ReadableArray?, val taskId: String?, val url: String?)
data class ValidatedRequiredParameters(
  val parts: ReadableArray,
  val taskId: String,
  val url: String
)

private fun isValidFilePayload(key: String, map: ReadableMap?): Either<String, ReadableMap> =
  validateParameter(PARAMETER_PART_PAYLOAD, map, ::hasKey)
    .map { (part, _) -> cons(part.getMap(PARAMETER_PART_PAYLOAD), part) }
    .fmap { (payload, part) ->
      validateParameter(
        PARAMETER_ABSOLUTE_FILE_PATH,
        payload,
        ::hasKey,
        part
      )
    }
    .fmap { (payload, part) -> validateParameter(PARAMETER_MIME_TYPE, payload, ::hasKey, part) }
    .fmap { (payload, part) ->
      validateParameter(
        PARAMETER_ABSOLUTE_FILE_PATH,
        payload.getString(PARAMETER_ABSOLUTE_FILE_PATH),
        ::isNotNullOrEmpty,
        cons(payload, part)
      )
    }
    .fmap { (_, t) ->
      val (payload, part) = t

      validateParameter(
        PARAMETER_MIME_TYPE,
        payload.getString(PARAMETER_MIME_TYPE),
        ::isNotNullOrEmpty,
        part
      )
    }
    .fold(
      { v -> left("$key.$v") },
      { (_, validatedMap) -> right(validatedMap) }
    )

private fun isValidStringPayload(key: String, map: ReadableMap?): Either<String, ReadableMap> =
  validateParameter(PARAMETER_PART_PAYLOAD, map, ::hasKey)
    .fold(
      { v -> left("$key.$v") },
      { (validatedMap) -> right(validatedMap) }
    )

private fun isValidPayload(key: String, map: ReadableMap?): Either<String, ReadableMap> =
  if (map?.getString(PARAMETER_PART_TYPE) == "file")
    isValidFilePayload(key, map)
  else isValidStringPayload(key, map)

private fun hasKey(key: String, map: ReadableMap?): Either<String, ReadableMap> =
  if (map?.hasKey(key) == true) right(map) else left(key)

private fun validatePartsMap(value: ReadableMap?): Result<ReadableMap> =
  validateParameter(PARAMETER_PARTS, value, ::isNotNull)
    .fmap { prev -> validateParameter(PARAMETER_PART_NAME, prev.first, ::hasKey) }
    .fmap { prev -> validateParameter(PARAMETER_PART_PAYLOAD, prev.first, ::hasKey) }
    .fmap { prev -> validateParameter(PARAMETER_PART_TYPE, prev.first, ::hasKey) }
    .fmap { prev -> validateParameter(PARAMETER_PART_TYPE, prev.first, ::isValidPayload) }
    .fold(
      { v -> Failure(Error(v)) },
      { v -> Success(v.first) }
    )

private fun validateRequiredParameters(
  parameters: RequiredParameters,
): Result<ValidatedRequiredParameters> =
  validateParameter(PARAMETER_PARTS, parameters.parts, ::isNotNull)
    .fmap { prev -> validateParameter(PARAMETER_TASK_ID, parameters.taskId, ::isNotNull, prev) }
    .fmap { prev -> validateParameter(PARAMETER_URL, parameters.url, ::isNotNull, prev) }
    .fold(
      { v -> Failure(processUnexpectedEmptyValue(v)) },
      { validatedParameters ->
        val (url, rest) = validatedParameters
        val (taskId, rest2) = rest
        val (parts, _) = rest2

        Success(ValidatedRequiredParameters(parts, taskId, url))
      }
    )

class UploaderParameterFactory {
  fun fromInput(input: ReadableMap): Result<UploaderParameters> =
    retrieveRequiredParameters(input)
      .fmap(::validateRequiredParameters)
      .fmap {
        val (rawParts, taskId, url) = it

        val method = input.getString(PARAMETER_METHOD) ?: DEFAULT_UPLOAD_METHOD

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

        val errorOrVerifiedUnit = verifyParts(rawParts)
        if (errorOrVerifiedUnit is BlobCourierError) {
          return@fmap Failure(errorOrVerifiedUnit)
        }

        val parts = createParts(rawParts)

        val uri = URL(url)

        Success(
          UploaderParameters(
            taskId,
            parts,
            uri,
            method,
            headers,
            returnResponse,
            progressInterval
          )
        )
      }
}
