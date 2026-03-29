/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.cancel

import io.deckers.blob_courier.common.Logger
import okhttp3.Call

private const val TAG = "CancelController"

private val logger = Logger(TAG)
private fun lv(m: String, e: Throwable? = null) = logger.v(m, e)

fun registerCancellationHandler(taskId: String, call: Call) {
  lv("Registering cancellation handler for $taskId")

  CancellationRegistry.register(taskId) { call.cancel() }

  lv("Registered cancellation handler for $taskId")
}
