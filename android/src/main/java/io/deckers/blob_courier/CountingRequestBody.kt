package io.deckers.blob_courier

import java.io.IOException
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Okio
import okio.Sink

// Inspired by https://gist.github.com/kyawkyaw/54d9e2feb1c969c90673
class CountingRequestBody(
  private var requestBody: RequestBody,
  private val progressListener: ProgressListener
) : RequestBody() {
  private val totalNumberOfBytes = requestBody.contentLength()

  override fun contentType(): MediaType? = requestBody.contentType()

  override fun contentLength(): Long = totalNumberOfBytes

  @Throws(IOException::class)
  override fun writeTo(sink: BufferedSink) = Okio.buffer(CountingSink(sink)).use { bufferedSink ->
    requestBody.writeTo(bufferedSink)
  }

  private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
    private var totalNumberOfBytesWritten = 0L

    @Throws(IOException::class)
    override fun write(source: Buffer, byteCount: Long) {
      super.write(source, byteCount)

      totalNumberOfBytesWritten += byteCount
      progressListener.onRequestProgress(totalNumberOfBytesWritten, contentLength())
    }
  }

  interface ProgressListener {
    fun onRequestProgress(totalNumberOfBytesWritten: Long, totalNumberOfBytes: Long)
  }
}
