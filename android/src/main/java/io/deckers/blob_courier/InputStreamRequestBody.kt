/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio

// Credit:
// [1] https://commonsware.com/blog/2020/07/05/multipart-upload-okttp-uri.html
// [2] https://github.com/square/okhttp/issues/3585#issuecomment-327319196
class InputStreamRequestBody(
  private val contentType: MediaType,
  private val contentResolver: ContentResolver,
  private val uri: Uri
) : RequestBody() {
  override fun contentType() = contentType

  override fun contentLength(): Long = -1

  @Throws(IOException::class)
  override fun writeTo(sink: BufferedSink) {
    val input = contentResolver.openInputStream(uri)

    input?.use { sink.writeAll(Okio.source(it)) }
      ?: throw IOException("Could not open $uri")
  }
}