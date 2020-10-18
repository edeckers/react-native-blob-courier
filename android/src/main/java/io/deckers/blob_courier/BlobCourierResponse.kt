/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import com.facebook.react.bridge.ReactApplicationContext
import java.io.IOException
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Okio
import okio.Source
import okio.Timeout

class BlobCourierResponse(
  private var context: ReactApplicationContext,
  private var taskId: String,
  private var originalResponseBody: ResponseBody
) : ResponseBody() {

  override fun contentType(): MediaType? = originalResponseBody.contentType()

  override fun contentLength(): Long = originalResponseBody.contentLength()

  override fun source(): BufferedSource =
    Okio.buffer(ProgressReportingSource())

  private inner class ProgressReportingSource : Source {
    val totalLength = contentLength()

    var totalNumberOfBytesRead: Long = 0

    private fun processNumberOfBytesRead(numberOfBytesRead: Long) {
      totalNumberOfBytesRead += if (numberOfBytesRead > 0) numberOfBytesRead else 0

      notifyBridge(context, taskId, totalNumberOfBytesRead, totalLength)
    }

    @Throws(IOException::class)
    override fun read(sink: Buffer, byteCount: Long): Long {
      val numberOfBytesRead = originalResponseBody.source().read(sink, byteCount)

      processNumberOfBytesRead(numberOfBytesRead)

      return numberOfBytesRead
    }

    override fun timeout(): Timeout? {
      return null
    }

    @Throws(IOException::class)
    override fun close() {
    }
  }
}
