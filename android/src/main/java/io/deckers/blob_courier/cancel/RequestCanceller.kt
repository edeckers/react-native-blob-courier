/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.cancel

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.common.ACTION_CANCEL_REQUEST
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_ERROR
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_EXCEPTION
import io.deckers.blob_courier.common.Failure
import io.deckers.blob_courier.common.Result
import io.deckers.blob_courier.common.Success
import io.deckers.blob_courier.common.createErrorFromThrowable
import java.lang.Exception

class RequestCanceller(private val reactContext: ReactApplicationContext) {
  fun cancel(taskId: String): Result<Unit> = try {
    val cancellationIntent =
      Intent(ACTION_CANCEL_REQUEST).putExtra("taskId", taskId)

    LocalBroadcastManager.getInstance(reactContext).sendBroadcast(cancellationIntent)

    Success(Unit)
  } catch (e: Exception) {
    Failure(createErrorFromThrowable(ERROR_UNEXPECTED_EXCEPTION, e))
  } catch (e: Error) {
    Failure(createErrorFromThrowable(ERROR_UNEXPECTED_ERROR, e))
  }
}
