/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.io.IOException
import java.nio.charset.Charset
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
    Okio.buffer(ProgressReportingSource(originalResponseBody.source()))

  private inner class ProgressReportingSource internal constructor(
    internal var mOriginalSource: BufferedSource
  ) : Source {
    internal var bytesRead: Long = 0

    @Throws(IOException::class)
    override fun read(sink: Buffer, byteCount: Long): Long {

      val read = mOriginalSource.read(sink, byteCount)
      bytesRead += if (read > 0) read else 0

      val cLen = contentLength()
      if (cLen == 0L) {
        return read
      }

      val args = Arguments.createMap().apply {
        putString("taskId", taskId)
        putString("written", bytesRead.toString())
        putString("total", cLen.toString())
        putString("chunk", sink.readString(Charset.defaultCharset()))
      }

      context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit(DEVICE_EVENT_PROGRESS, args)

      return read
    }

    override fun timeout(): Timeout? {
      return null
    }

    @Throws(IOException::class)
    override fun close() {
    }
  }
}
