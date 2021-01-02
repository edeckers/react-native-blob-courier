/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import io.deckers.blob_courier.react.toReactMap

data class TestUploadParameterMap(
  val taskId: String,
  val parts: Array<Map<String, *>>,
  val url: String
) {

  fun toMap() = mapOf("taskId" to taskId, "parts" to parts, "url" to url)
  fun toReactMap() = toMap().toReactMap()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as TestUploadParameterMap

    if (taskId != other.taskId) return false
    if (!parts.contentEquals(other.parts)) return false
    if (url != other.url) return false

    return true
  }

  override fun hashCode(): Int {
    var result = taskId.hashCode()
    result = 31 * result + parts.contentHashCode()
    result = 31 * result + url.hashCode()
    return result
  }
}
