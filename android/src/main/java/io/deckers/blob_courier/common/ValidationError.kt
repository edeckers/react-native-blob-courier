/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

sealed class ValidationError(code: String, message: String) : BlobCourierError(code, message) {
  class IsNull(parameterName: String) : ValidationError(
    ERROR_PARAMETER_IS_NULL, "Unexpected `null` value for `$parameterName`"
  )

  class IsEmpty(parameterName: String) : ValidationError(
    ERROR_PARAMETER_IS_NULL, "Unexpected empty value for `$parameterName`"
  )

  class KeyDoesNotExist(parameterName: String) : ValidationError(
    ERROR_MISSING_REQUIRED_PARAMETER, "Key `$parameterName` does not exist"
  )

  class InvalidType(
    parameterName: String,
    expectedType: String,
    receivedType: String
  ) : ValidationError(
    ERROR_INVALID_VALUE,
    "$parameterName has incorrect type `$expectedType`, received `$receivedType`"
  )
}
