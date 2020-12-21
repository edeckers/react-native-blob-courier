/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import com.facebook.common.internal.ImmutableMap
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import java.lang.reflect.Type

const val ERROR_MISSING_REQUIRED_PARAMETER = "ERROR_MISSING_REQUIRED_PARAMETER"

const val PARAMETER_ABSOLUTE_FILE_PATH = "absoluteFilePath"
const val PARAMETER_ANDROID_SETTINGS = "android"
const val PARAMETER_DOWNLOAD_MANAGER_SETTINGS = "downloadManager"
const val PARAMETER_FILENAME = "filename"
const val PARAMETER_HEADERS = "headers"
const val PARAMETER_METHOD = "method"
const val PARAMETER_MIME_TYPE = "mimeType"
const val PARAMETER_PART_PAYLOAD = "payload"
const val PARAMETER_PARTS = "parts"
const val PARAMETER_RETURN_RESPONSE = "returnResponse"
const val PARAMETER_SETTINGS_PROGRESS_INTERVAL = "progressIntervalMilliseconds"
const val PARAMETER_TARGET = "target"
const val PARAMETER_TASK_ID = "taskId"
const val PARAMETER_URL = "url"
const val PARAMETER_USE_DOWNLOAD_MANAGER = "useDownloadManager"

private val REQUIRED_PARAMETER_PROCESSORS = ImmutableMap.of(
  Boolean::class.java.toString(),
  { input: ReadableMap, parameterName: String -> input.getBoolean(parameterName) },
  ReadableMap::class.java.toString(),
  { input: ReadableMap, parameterName: String -> input.getMap(parameterName) },
  String::class.java.toString(),
  { input: ReadableMap, parameterName: String -> input.getString(parameterName) }
)

private val AVAILABLE_PARAMETER_PROCESSORS = REQUIRED_PARAMETER_PROCESSORS.keys.joinToString(", ")

fun assertRequiredParameter(input: ReadableMap, type: Type, parameterName: String) {
  val defaultFallback =
    "No processor defined for type `$type`, valid options: $AVAILABLE_PARAMETER_PROCESSORS"
  val unknownProcessor = { _: ReadableMap, _: String -> throw Exception(defaultFallback) }

  val maybeValue =
    REQUIRED_PARAMETER_PROCESSORS.getOrElse(
      type.toString(), { unknownProcessor }
    )(input, parameterName)

  maybeValue ?: throw BlobCourierError(
    ERROR_MISSING_REQUIRED_PARAMETER,
    "`$parameterName` is a required parameter of type `$type`"
  )
}

@Suppress("SameParameterValue")
fun getMapInt(input: ReadableMap, field: String, fallback: Int): Int =
  if (input.hasKey(field)) input.getInt(field) else fallback

fun processUnexpectedError(promise: Promise, e: Error) = promise.reject(
  ERROR_UNEXPECTED_ERROR,
  "An unexpected error occurred: ${e.message}"
)

fun processUnexpectedException(promise: Promise, e: Exception) = promise.reject(
  ERROR_UNEXPECTED_EXCEPTION,
  "An unexpected exception occurred: ${e.message}"
)

fun processUnexpectedEmptyValue(promise: Promise, parameterName: String) = promise.reject(
  ERROR_UNEXPECTED_EMPTY_VALUE,
  "Parameter `$parameterName` cannot be empty."
)
