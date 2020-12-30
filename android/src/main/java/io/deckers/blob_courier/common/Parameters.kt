/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

import com.facebook.react.bridge.ReadableMap

const val ERROR_MISSING_REQUIRED_PARAMETER = "ERROR_MISSING_REQUIRED_PARAMETER"

const val PROVIDED_PARAMETERS = "PROVIDED_PARAMETERS"

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

fun getMapInt(input: ReadableMap, field: String, fallback: Int): Int =
  if (input.hasKey(field)) input.getInt(field) else fallback
