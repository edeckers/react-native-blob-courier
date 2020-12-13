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
import okio.Source
import okio.Timeout
import okio.buffer

class BlobCourierProgressResponse(
  private val context: ReactApplicationContext,
  private val taskId: String,
  private val progressInterval: Int,
  private val responseBody: ResponseBody
) : ResponseBody() {
  private val totalNumberOfBytes = contentLength()

  override fun contentType(): MediaType? = responseBody.contentType()

  override fun contentLength(): Long = responseBody.contentLength()

  override fun source(): BufferedSource =
    ProgressReportingSource().buffer()

  private inner class ProgressReportingSource : Source {
    private var totalNumberOfBytesRead: Long = 0

    private val progressNotifier =
      CongestionAvoidingProgressNotifier(context, taskId, totalNumberOfBytes, progressInterval)

    private fun processNumberOfBytesRead(numberOfBytesRead: Long) {
      totalNumberOfBytesRead += if (numberOfBytesRead > 0) numberOfBytesRead else 0

      progressNotifier.notify(totalNumberOfBytesRead)
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
