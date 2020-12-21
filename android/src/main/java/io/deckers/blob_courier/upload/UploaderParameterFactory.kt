/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.upload

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import io.deckers.blob_courier.common.DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.common.DEFAULT_UPLOAD_METHOD
import io.deckers.blob_courier.common.ERROR_MISSING_REQUIRED_PARAMETER
import io.deckers.blob_courier.common.PARAMETER_ABSOLUTE_FILE_PATH
import io.deckers.blob_courier.common.PARAMETER_HEADERS
import io.deckers.blob_courier.common.PARAMETER_METHOD
import io.deckers.blob_courier.common.PARAMETER_MIME_TYPE
import io.deckers.blob_courier.common.PARAMETER_PARTS
import io.deckers.blob_courier.common.PARAMETER_PART_PAYLOAD
import io.deckers.blob_courier.common.PARAMETER_RETURN_RESPONSE
import io.deckers.blob_courier.common.PARAMETER_SETTINGS_PROGRESS_INTERVAL
import io.deckers.blob_courier.common.PARAMETER_TASK_ID
import io.deckers.blob_courier.common.PARAMETER_URL
import io.deckers.blob_courier.common.filterHeaders
import io.deckers.blob_courier.common.getMapInt
import io.deckers.blob_courier.common.processUnexpectedEmptyValue
import java.net.URL

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

private fun verifyParts(parts: ReadableMap, promise: Promise): Boolean =
  parts.toHashMap().keys.fold(
    true,
    { p, c -> verifyPart(parts.getMap(c), promise) && p }
  )

data class UploaderParameters(
  val taskId: String,
  val verifiedParts: ReadableMap,
  val uri: URL,
  val method: String,
  val headers: Map<String, String>,
  val returnResponse: Boolean,
  val progressInterval: Int
)

class UploaderParameterFactory {
  fun fromInput(input: ReadableMap, promise: Promise): UploaderParameters? {
    val maybeTaskId = input.getString(PARAMETER_TASK_ID)
    val maybeUrl = input.getString(PARAMETER_URL)
    val method = input.getString(PARAMETER_METHOD) ?: DEFAULT_UPLOAD_METHOD
    val maybeParts = input.getMap(PARAMETER_PARTS)

    val unfilteredHeaders =
      input.getMap(PARAMETER_HEADERS)?.toHashMap() ?: emptyMap<String, Any>()

    val headers = filterHeaders(unfilteredHeaders)

    val returnResponse =
      input.hasKey(PARAMETER_RETURN_RESPONSE) && input.getBoolean(PARAMETER_RETURN_RESPONSE)

    val progressInterval =
      getMapInt(input, PARAMETER_SETTINGS_PROGRESS_INTERVAL, DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS)

    if (maybeTaskId.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_TASK_ID)

      return null
    }

    if (maybeParts == null) {
      processUnexpectedEmptyValue(promise, PARAMETER_PARTS)

      return null
    }

    if (maybeUrl.isNullOrEmpty()) {
      processUnexpectedEmptyValue(promise, PARAMETER_URL)

      return null
    }

    if (!verifyParts(maybeParts, promise)) {
      return null
    }

    val uri = URL(maybeUrl)

    return UploaderParameters(
      maybeTaskId,
      maybeParts,
      uri,
      method,
      headers,
      returnResponse,
      progressInterval
    )
  }
}
