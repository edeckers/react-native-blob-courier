/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.upload

import android.net.Uri

data class FilePayload(
  val absoluteFilePath: Uri,
  val filename: String,
  val mimeType: String
) : PartPayload
