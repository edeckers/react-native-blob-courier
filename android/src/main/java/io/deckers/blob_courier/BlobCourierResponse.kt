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
    val originalSource: BufferedSource
  ) : Source {
    val totalLength = contentLength()

    var totalNumberOfBytesRead: Long = 0

    private fun notifyBridge() =
      context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit(
          DEVICE_EVENT_PROGRESS,
          Arguments.createMap().apply {
            putString("taskId", taskId)
            putString("written", totalNumberOfBytesRead.toString())
            putString("total", totalLength.toString())
          }
        )

    private fun processNumberOfBytesRead(numberOfBytesRead: Long) {
      totalNumberOfBytesRead += if (numberOfBytesRead > 0) numberOfBytesRead else 0

      notifyBridge()
    }

    @Throws(IOException::class)
    override fun read(sink: Buffer, byteCount: Long): Long {
      val numberOfBytesRead = originalSource.read(sink, byteCount)

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
