/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.cancel

import java.util.concurrent.ConcurrentHashMap

object CancellationRegistry {
  private val handlers = ConcurrentHashMap<String, () -> Unit>()

  fun register(taskId: String, onCancel: () -> Unit) {
    handlers[taskId] = onCancel
  }

  fun cancel(taskId: String) {
    handlers.remove(taskId)?.invoke()
  }

  fun unregister(taskId: String) {
    handlers.remove(taskId)
  }
}
