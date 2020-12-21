/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.progress

import com.facebook.react.bridge.ReactApplicationContext
import java.io.IOException
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Okio
import okio.Sink

class BlobCourierProgressRequest(
  private val context: ReactApplicationContext,
  private val taskId: String,
  private val requestBody: RequestBody,
  private val progressInterval: Int
) : RequestBody() {
  private val totalNumberOfBytes = requestBody.contentLength()

  override fun contentType(): MediaType? = requestBody.contentType()

  override fun contentLength(): Long = totalNumberOfBytes

  @Throws(IOException::class)
  override fun writeTo(sink: BufferedSink) =
    Okio.buffer(CountingSink(sink)).use(requestBody::writeTo)

  private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
    private val progressNotifier =
      CongestionAvoidingProgressNotifier(context, taskId, totalNumberOfBytes, progressInterval)

    private var totalNumberOfBytesWritten = 0L

    private fun processNumberOfBytesWritten(numberOfBytesRead: Long) {
      totalNumberOfBytesWritten += if (numberOfBytesRead > 0) numberOfBytesRead else 0

      progressNotifier.notify(totalNumberOfBytesWritten)
    }

    @Throws(IOException::class)
    override fun write(source: Buffer, byteCount: Long) {
      super.write(source, byteCount)

      processNumberOfBytesWritten(byteCount)
    }
  }
}
