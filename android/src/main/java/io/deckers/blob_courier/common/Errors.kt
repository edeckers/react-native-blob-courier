/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

const val ERROR_INVALID_VALUE = "ERROR_INVALID_VALUE"
const val ERROR_UNEXPECTED_EXCEPTION = "ERROR_UNEXPECTED_EXCEPTION"
const val ERROR_UNEXPECTED_ERROR = "ERROR_UNEXPECTED_ERROR"
const val ERROR_UNEXPECTED_EMPTY_VALUE = "ERROR_UNEXPECTED_EMPTY_VALUE"
const val ERROR_UNKNOWN_HOST = "ERROR_UNKNOWN_HOST"

open class BlobCourierError(val code: String, val message: String)
open class BlobCourierThrowabe(val code: String, message: String) : Throwable(message)

class BlobCourierErrorInvalidValue(
  code: String,
  parameterName: String,
  value: String
) :
  BlobCourierError(code, "Invalid value `$value` for parameter `$parameterName`")

class BlobCourierErrorUnexpectedEmpty(
  code: String,
  parameterName: String
) :
  BlobCourierError(code, "Parameter `$parameterName` is empty")