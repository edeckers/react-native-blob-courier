/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.react

import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.progress.ProgressNotifierFactory

class CongestionAvoidingProgressNotifierFactory(
  private val context: ReactApplicationContext,
  private val progressInterval: Int
) : ProgressNotifierFactory {
  override fun create(taskId: String) =
    CongestionAvoidingProgressNotifier(context, taskId, progressInterval)
}
