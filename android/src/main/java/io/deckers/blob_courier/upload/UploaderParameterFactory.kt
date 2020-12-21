/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.upload

import android.net.Uri
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import io.deckers.blob_courier.common.DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.common.DEFAULT_UPLOAD_METHOD
import io.deckers.blob_courier.common.ERROR_MISSING_REQUIRED_PARAMETER
import io.deckers.blob_courier.common.PARAMETER_ABSOLUTE_FILE_PATH
import io.deckers.blob_courier.common.PARAMETER_FILENAME
import io.deckers.blob_courier.common.PARAMETER_HEADERS
import io.deckers.blob_courier.common.PARAMETER_METHOD
import io.deckers.blob_courier.common.PARAMETER_MIME_TYPE
import io.deckers.blob_courier.common.PARAMETER_PART_PAYLOAD
import io.deckers.blob_courier.common.PARAMETER_SETTINGS_PROGRESS_INTERVAL
import io.deckers.blob_courier.common.PARAMETER_TASK_ID
import io.deckers.blob_courier.common.PARAMETER_URL
import io.deckers.blob_courier.common.filterHeaders
import io.deckers.blob_courier.common.getMapInt
import io.deckers.blob_courier.common.processUnexpectedEmptyValue
import io.deckers.blob_courier.common.tryRetrieveMap
import io.deckers.blob_courier.common.tryRetrieveString
import java.net.URL

private const val PARAMETER_PARTS = "parts"
private const val PARAMETER_RETURN_RESPONSE = "returnResponse"

private fun verifyFilePart(part: ReadableMap, promise: Promise): Boolean {
  if (!part.hasKey(PARAMETER_PART_PAYLOAD)) {
    promise.reject(ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_PART_PAYLOAD")
    return false
  }

  val payload = part.getMap(PARAMETER_PART_PAYLOAD)!!
  if (!payload.hasKey(PARAMETER_ABSOLUTE_FILE_PATH)) {
    promise.reject(ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_ABSOLUTE_FILE_PATH")
    return false
  }

  if (!payload.hasKey(PARAMETER_MIME_TYPE)) {
    promise.reject(ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_MIME_TYPE")
    return false
  }

  if (payload.getString(PARAMETER_ABSOLUTE_FILE_PATH).isNullOrEmpty()) {
    processUnexpectedEmptyValue(promise, PARAMETER_ABSOLUTE_FILE_PATH)
    return false
  }

  if (payload.getString(PARAMETER_MIME_TYPE).isNullOrEmpty()) {
    processUnexpectedEmptyValue(promise, PARAMETER_MIME_TYPE)
    return false
  }

  return true
}

private fun verifyStringPart(part: ReadableMap, promise: Promise): Boolean {
  if (part.hasKey(PARAMETER_PART_PAYLOAD)) {
    return true
  }

  promise.reject(ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_PART_PAYLOAD")
  return false
}

private fun verifyPart(part: ReadableMap?, promise: Promise): Boolean {
  if (part == null) {
    processUnexpectedEmptyValue(promise, "part")
    return false
  }

  if (!part.hasKey("type")) {
    promise.reject(ERROR_MISSING_REQUIRED_PARAMETER, "part.type")
    return false
  }

  if (part.getString("type") == "file") {
    return verifyFilePart(part, promise)
  }

  return verifyStringPart(part, promise)
}

data class UploaderParameters(
  val taskId: String,
  val parts: Map<String, Part>,
  val uri: URL,
  val method: String,
  val headers: Map<String, String>,
  val returnResponse: Boolean,
  val progressInterval: Int
)

private fun createFilePayload(payload: ReadableMap, promise: Promise): FilePart? {
  if (!payload.hasKey(PARAMETER_ABSOLUTE_FILE_PATH)) {
    promise.reject(ERROR_MISSING_REQUIRED_PARAMETER, "payload.$PARAMETER_ABSOLUTE_FILE_PATH")
    return null
  }

  if (!payload.hasKey(PARAMETER_MIME_TYPE)) {
    promise.reject(ERROR_MISSING_REQUIRED_PARAMETER, "payload.$PARAMETER_MIME_TYPE")
    return null
  }

  val absoluteFilePath = payload.getString(PARAMETER_ABSOLUTE_FILE_PATH)
  if (absoluteFilePath.isNullOrEmpty()) {
    processUnexpectedEmptyValue(promise, PARAMETER_ABSOLUTE_FILE_PATH)
    return null
  }

  val mimeType = payload.getString(PARAMETER_MIME_TYPE)
  if (mimeType.isNullOrEmpty()) {
    processUnexpectedEmptyValue(promise, PARAMETER_MIME_TYPE)
    return null
  }

  val fileUrl = Uri.parse(payload.getString(PARAMETER_ABSOLUTE_FILE_PATH)!!)
  val fileUrlWithScheme =
    if (fileUrl.scheme == null) Uri.parse("file://$fileUrl") else fileUrl

  val filename =
    if (payload.hasKey(PARAMETER_FILENAME)) (
      payload.getString(PARAMETER_FILENAME)
        ?: fileUrl.lastPathSegment
      ) else fileUrl.lastPathSegment

  if (fileUrlWithScheme == null) {
    return null
  }

  if (filename == null) {
    return null
  }

  return FilePart(fileUrlWithScheme, filename, mimeType)
}

private fun createPart(part: ReadableMap, promise: Promise): Part? {
  if (!verifyPart(part, promise)) {
    return null
  }

  if (!part.hasKey(PARAMETER_PART_PAYLOAD)) {
    promise.reject(ERROR_MISSING_REQUIRED_PARAMETER, "part.$PARAMETER_PART_PAYLOAD")
    return null
  }

  val payloadData = part.getMap(PARAMETER_PART_PAYLOAD) ?: return null

  val maybePayload = when (part.getString("type")) {
    "file" -> createFilePayload(payloadData, promise)
    else -> DataPart(part.getString(PARAMETER_PART_PAYLOAD) ?: "")
  }

  return maybePayload?.let { it -> Part(it) }
}

private fun createParts(maybeParts: ReadableMap, promise: Promise): Map<String, Part> =
  maybeParts.toHashMap().keys.fold(
    emptyMap(),
    { p, multipartName ->
      val maybePart = maybeParts.getMap(multipartName)

      val px = maybePart?.let { createPart(maybePart, promise) }

      px?.let { p.plus(multipartName to it) } ?: p
    }
  )

private fun retrieveRequiredParametersOrThrow(input: ReadableMap):
  Triple<ReadableMap?, String?, String?> {
    val rawParts = tryRetrieveMap(input, PARAMETER_PARTS)
    val taskId = tryRetrieveString(input, PARAMETER_TASK_ID)
    val url = tryRetrieveString(input, PARAMETER_URL)

    return Triple(rawParts, taskId, url)
  }

private fun validateRequiredParameters(
  parameters: Triple<ReadableMap?, String?, String?>,
  promise: Promise
): Triple<ReadableMap, String, String>? {
  val (rawParts, taskId, url) = parameters

  if (taskId == null) {
    processUnexpectedEmptyValue(promise, PARAMETER_TASK_ID)

    return null
  }

  if (rawParts == null) {
    processUnexpectedEmptyValue(promise, PARAMETER_PARTS)

    return null
  }

  if (url == null) {
    processUnexpectedEmptyValue(promise, PARAMETER_URL)

    return null
  }

  return Triple(rawParts, taskId, url)
}

class UploaderParameterFactory {
  fun fromInput(input: ReadableMap, promise: Promise): UploaderParameters? {
    val requiredParameters = retrieveRequiredParametersOrThrow(input)

    return validateRequiredParameters(requiredParameters, promise)?.let {
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

      val parts = createParts(rawParts, promise)

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
