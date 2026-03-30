/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.cancel

import io.deckers.blob_courier.common.ERROR_UNEXPECTED_ERROR
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_EXCEPTION
import io.deckers.blob_courier.common.Failure
import io.deckers.blob_courier.common.Result
import io.deckers.blob_courier.common.Success
import io.deckers.blob_courier.common.createErrorFromThrowable

class RequestCanceller {
  fun cancel(taskId: String): Result<Unit> = try {
    CancellationRegistry.cancel(taskId)

    Success(Unit)
  } catch (e: Exception) {
    Failure(createErrorFromThrowable(ERROR_UNEXPECTED_EXCEPTION, e))
  } catch (e: Error) {
    Failure(createErrorFromThrowable(ERROR_UNEXPECTED_ERROR, e))
  }
}
