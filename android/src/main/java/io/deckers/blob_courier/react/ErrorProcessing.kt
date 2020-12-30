/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.react

import io.deckers.blob_courier.common.BlobCourierError
import io.deckers.blob_courier.common.BlobCourierErrorUnexpectedEmpty
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_EMPTY_VALUE
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_ERROR
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_EXCEPTION

fun processUnexpectedError(e: Error) = BlobCourierError(
  ERROR_UNEXPECTED_ERROR,
  "An unexpected error occurred: ${e.message},$e"
)

fun processUnexpectedException(e: Exception) = BlobCourierError(
  ERROR_UNEXPECTED_EXCEPTION,
  "An unexpected exception occurred: ${e.message},$e"
)

fun processUnexpectedEmptyValue(parameterName: String) = BlobCourierErrorUnexpectedEmpty(
  ERROR_UNEXPECTED_EMPTY_VALUE,
  parameterName
)
