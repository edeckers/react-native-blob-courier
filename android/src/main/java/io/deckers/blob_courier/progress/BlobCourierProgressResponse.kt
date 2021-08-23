/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.progress

import java.io.IOException
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.buffer
import okio.Source
import okio.Timeout

class BlobCourierProgressResponse(
  private val progressNotifier: ProgressNotifier,
  private val responseBody: ResponseBody
) : ResponseBody() {
  private val totalNumberOfBytes = responseBody.contentLength()

  override fun contentType() = responseBody.contentType()

  override fun contentLength() = totalNumberOfBytes

  override fun source(): BufferedSource = ProgressReportingSource().buffer()

  private inner class ProgressReportingSource : Source {
    private var totalNumberOfBytesRead: Long = 0

    private fun processNumberOfBytesRead(numberOfBytesRead: Long) {
      totalNumberOfBytesRead += if (numberOfBytesRead > 0) numberOfBytesRead else 0

      progressNotifier.notify(totalNumberOfBytesRead, totalNumberOfBytes)
    }

    @Throws(IOException::class)
    override fun read(sink: Buffer, byteCount: Long): Long {
      val numberOfBytesRead = responseBody.source().read(sink, byteCount)

      processNumberOfBytesRead(numberOfBytesRead)

      return numberOfBytesRead
    }

    override fun timeout(): Timeout = Timeout.NONE

    @Throws(IOException::class)
    override fun close() = Unit
  }
}
