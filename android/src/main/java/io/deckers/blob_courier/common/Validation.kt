/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

open class ValidationError(code: String, parameterName: String) : BlobCourierError(
  code, "Unexpected `null` value for `$parameterName`"
)

class ValidationIsNull(parameterName: String) : ValidationError(
  ERROR_PARAMETER_IS_NULL, "Unexpected `null` value for `$parameterName`"
)

class ValidationIsEmpty(parameterName: String) : ValidationError(
  ERROR_PARAMETER_IS_NULL, "Unexpected empty value for `$parameterName`"
)

class ValidationKeyDoesNotExist(parameterName: String) : ValidationError(
  ERROR_PARAMETER_IS_NULL, "Key `$parameterName` does not exist"
)
