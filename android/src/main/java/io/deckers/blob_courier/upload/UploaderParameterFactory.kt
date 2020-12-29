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
import io.deckers.blob_courier.common.VFailure
import io.deckers.blob_courier.common.VResult
import io.deckers.blob_courier.common.VSuccess
import io.deckers.blob_courier.common.ValidationError
import io.deckers.blob_courier.common.filterHeaders
import io.deckers.blob_courier.common.fold
import io.deckers.blob_courier.common.getMapInt
import io.deckers.blob_courier.common.isNotNull
import io.deckers.blob_courier.common.isNotNullOrEmpty
import io.deckers.blob_courier.common.left
import io.deckers.blob_courier.common.right
import io.deckers.blob_courier.common.test
import io.deckers.blob_courier.common.tryRetrieveArray
import io.deckers.blob_courier.common.tryRetrieveString
import java.net.URL
import okhttp3.MediaType
import okhttp3.MultipartBody

private const val PARAMETER_PARTS = "parts"
private const val PARAMETER_RETURN_RESPONSE = "returnResponse"

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

private fun verifyParts(parts: ReadableArray): VResult<Unit> {
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

  val failedPartVerifications =
    mapParts.map(::validatePartsMap).filter { verification -> verification is Either.Left }
  if (failedPartVerifications.isNotEmpty()) {
    val firstFailure = failedPartVerifications.first().map { Unit } as Either.Left

    return firstFailure.map { Unit }
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

private fun retrieveRequiredParameters(input: ReadableMap): Result<RequiredParameters> {
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

class Writer<A, B>(o: A, acc: B) {
  val v = Pair(o, acc)

  operator fun component1() = v.first
  operator fun component2() = v.second

  infix fun <TResult> discard(v: VResult<TResult>) = v.map { Pair(it, this) }
  infix fun <TResult> update(v: VResult<TResult>) = v.map { Pair(it, Writer(it, this)) }
}

fun <A> write(v: VResult<A>) = v.map { Pair(it, Writer(it, Unit)) }

fun <TValue, TContextLast, TContextRest> pass(
  test: ((v: TValue?) -> VResult<TValue>)
): (Pair<TValue, Writer<TContextLast, TContextRest>>)
-> VResult<Pair<TValue, Writer<TContextLast, TContextRest>>> =
  { context: Pair<TValue, Writer<TContextLast, TContextRest>> ->
    context.second discard test(context.first, test)
  }

private fun isValidFilePayload(map: ReadableMap?):
  VResult<ReadableMap> =
    test(map, hasKey(PARAMETER_PART_PAYLOAD))
      .pipe(::write)
      .fmap { (part, w) ->
        w update test(
          part.getMap(PARAMETER_PART_PAYLOAD),
          isNotNull(PARAMETER_PART_PAYLOAD)
        )
      }
      .fmap(pass(hasKey(PARAMETER_ABSOLUTE_FILE_PATH)))
      .fmap(pass(hasKey(PARAMETER_MIME_TYPE)))
      .fmap { (payload, w) ->
        w discard test(
          payload.getString(PARAMETER_ABSOLUTE_FILE_PATH),
          isNotNullOrEmpty(PARAMETER_ABSOLUTE_FILE_PATH)
        )
      }
      .fmap { (_, w) ->
        val (payload, _) = w.v

        w discard test(
          payload.getString(PARAMETER_MIME_TYPE),
          isNotNullOrEmpty(PARAMETER_MIME_TYPE)
        )
      }
      .map { (_, w) ->
        val (_, w2) = w.v
        val (part, _) = w2.v

        part
      }

@Suppress("SameParameterValue")
private fun isValidStringPayload(part: ReadableMap?):
  VResult<ReadableMap> = test(part, hasKey(PARAMETER_PART_PAYLOAD))
    .pipe(::write)
    .fmap { (map, w) ->
      w discard test(
        map.getString(PARAMETER_PART_PAYLOAD),
        isNotNullOrEmpty(PARAMETER_PART_PAYLOAD)
      )
    }
    .map { (_, w) -> w.v.first }

private fun isValidPayload(map: ReadableMap?): VResult<ReadableMap> =
  if (map?.getString(PARAMETER_PART_TYPE) == "file")
    isValidFilePayload(map)
  else isValidStringPayload(map)

private fun hasKey(key: String): (map: ReadableMap?) -> VResult<ReadableMap> =
  { map: ReadableMap? ->
    if (map?.hasKey(key) == true) right(map) else left(ValidationError.KeyDoesNotExist(key))
  }

private fun validatePartsMap(value: ReadableMap?): VResult<ReadableMap> =
  test(value, isNotNull(PARAMETER_PARTS))
    .pipe(::write)
    .fmap(pass(hasKey(PARAMETER_PART_NAME)))
    .fmap(pass(hasKey(PARAMETER_PART_PAYLOAD)))
    .fmap(pass(hasKey(PARAMETER_PART_TYPE)))
    .fmap(pass(::isValidPayload))
    .fold(
      ::VFailure
    ) { (validatedPart, _) -> VSuccess(validatedPart) }

private fun validateRequiredParameters(
  parameters: RequiredParameters,
): Result<ValidatedRequiredParameters> =
  test(parameters.parts, isNotNull(PARAMETER_PARTS))
    .pipe(::write)
    .fmap { (_, w) -> w update test(parameters.taskId, isNotNull(PARAMETER_TASK_ID)) }
    .fmap { (_, w) -> w update test(parameters.url, isNotNull(PARAMETER_URL)) }
    .fold(
      ::Failure
    ) { (_, validatedParameters) ->
      val (url, rest) = validatedParameters
      val (taskId, rest2) = rest
      val (parts, _) = rest2

      Success(ValidatedRequiredParameters(parts, taskId, url))
    }

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

        errorOrVerifiedUnit.map {
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
        }.fold(
          ::Failure,
          ::Success
        )
      }
}
