/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

import com.facebook.common.internal.ImmutableMap
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import java.lang.reflect.Type

const val ERROR_MISSING_REQUIRED_PARAMETER = "ERROR_MISSING_REQUIRED_PARAMETER"

const val PARAMETER_ABSOLUTE_FILE_PATH = "absoluteFilePath"
const val PARAMETER_FILENAME = "filename"
const val PARAMETER_HEADERS = "headers"
const val PARAMETER_METHOD = "method"
const val PARAMETER_MIME_TYPE = "mimeType"
const val PARAMETER_PART_NAME = "name"
const val PARAMETER_PART_PAYLOAD = "payload"
const val PARAMETER_PART_TYPE = "type"
const val PARAMETER_SETTINGS_PROGRESS_INTERVAL = "progressIntervalMilliseconds"
const val PARAMETER_TASK_ID = "taskId"
const val PARAMETER_URL = "url"

private val REQUIRED_PARAMETER_PROCESSORS = ImmutableMap.of(
  Array::class.java.toString(),
  { input: ReadableMap, parameterName: String -> input.getArray(parameterName) },
  String::class.java.toString(),
  { input: ReadableMap, parameterName: String -> input.getString(parameterName) }
)

private val AVAILABLE_PARAMETER_PROCESSORS = REQUIRED_PARAMETER_PROCESSORS.keys.joinToString(", ")

fun assertRequiredParameter(
  input: ReadableMap,
  type: Type,
  parameterName: String
): ValidationResult<Any> {
  val defaultFallback =
    "No processor defined for type `$type`, valid options: $AVAILABLE_PARAMETER_PROCESSORS"
  val unknownProcessor = { _: ReadableMap, _: String -> throw Exception(defaultFallback) }

  val maybeValue =
    REQUIRED_PARAMETER_PROCESSORS.getOrElse(
      type.toString(), { unknownProcessor }
    )(input, parameterName)

  return validate(maybeValue, isNotNull(parameterName))
    .fold(
      { ValidationFailure(ValidationError.MissingParameter(parameterName, type.toString())) },
      ::ValidationSuccess
    )
}

fun tryRetrieveArray(input: ReadableMap, parameterName: String):
  ValidationResult<ReadableArray?> =
    assertRequiredParameter(input, Array::class.java, parameterName).map {
      input.getArray(parameterName)
    }

fun tryRetrieveString(input: ReadableMap, parameterName: String): ValidationResult<String?> =
  assertRequiredParameter(input, String::class.java, parameterName)
    .map { input.getString(parameterName) }

fun getMapInt(input: ReadableMap, field: String, fallback: Int): Int =
  if (input.hasKey(field)) input.getInt(field) else fallback
