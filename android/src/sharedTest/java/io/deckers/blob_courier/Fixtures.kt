/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import kotlin.concurrent.thread

private const val DEFAULT_PROMISE_TIMEOUT_MILLISECONDS = 10_000L

object Fixtures {

  fun createValidTestFetchParameterMap(): Map<String, String> = mapOf(
    "taskId" to "some-task-id",
    "filename" to "some-filename.png",
    "url" to "https://github.com/edeckers/react-native-blob-courier"
  )

  fun createValidUploadTestParameterMap(
    taskId: String,
    localPath: String
  ): Map<String, String> = mapOf(
    "taskId" to taskId,
    "filePath" to localPath,
    "url" to "https://file.io"
  )

  fun runFetchBlob(
    context: ReactApplicationContext,
    input: ReadableMap,
    promise: Promise,
    timeoutMilliseconds: Long = DEFAULT_PROMISE_TIMEOUT_MILLISECONDS
  ) {
    val m = BlobCourierModule(context)

    thread {
      m.fetchBlob(input, promise)
    }

    Thread.sleep(timeoutMilliseconds)
  }

  fun runUploadBlob(
    context: ReactApplicationContext,
    input: ReadableMap,
    promise: Promise,
    timeoutMilliseconds: Long = DEFAULT_PROMISE_TIMEOUT_MILLISECONDS
  ) {
    val m = BlobCourierModule(context)

    thread {
      m.uploadBlob(input, promise)
    }

    Thread.sleep(timeoutMilliseconds)
  }

  class EitherPromise(
    val left: (message: String?) -> Unit,
    val right: (v: ReadableMap?) -> Unit
  ) : Promise {
    override fun resolve(v: Any?) {
      val maybeValue = if (v is ReadableMap) v else null

      right(maybeValue)
    }

    override fun reject(code: String?, message: String?) = left(message)

    override fun reject(code: String?, throwable: Throwable?) = left(throwable?.localizedMessage)

    override fun reject(code: String?, message: String?, throwable: Throwable?) = left(message)

    override fun reject(throwable: Throwable?) = left(throwable?.localizedMessage)

    override fun reject(throwable: Throwable?, userInfo: WritableMap?) =
      left(throwable?.localizedMessage)

    override fun reject(code: String?, userInfo: WritableMap) = left(code ?: "")

    override fun reject(code: String?, throwable: Throwable?, userInfo: WritableMap?) =
      left(throwable?.localizedMessage)

    override fun reject(code: String?, message: String?, userInfo: WritableMap) = left(message)

    override fun reject(
      code: String?,
      message: String?,
      throwable: Throwable?,
      userInfo: WritableMap?
    ) =
      left(message)

    override fun reject(message: String?) = left(message ?: "")
  }

  class BooleanPromise(val c: (Boolean) -> Unit) : Promise {
    override fun reject(code: String?, message: String?) = c(false)

    override fun reject(code: String?, throwable: Throwable?) = c(false)

    override fun reject(code: String?, message: String?, throwable: Throwable?) = c(false)

    override fun reject(throwable: Throwable?) = c(false)

    override fun reject(throwable: Throwable?, userInfo: WritableMap?) = c(false)

    override fun reject(code: String?, userInfo: WritableMap) = c(false)

    override fun reject(code: String?, throwable: Throwable?, userInfo: WritableMap?) = c(false)

    override fun reject(code: String?, message: String?, userInfo: WritableMap) = c(false)

    override fun reject(
      code: String?,
      message: String?,
      throwable: Throwable?,
      userInfo: WritableMap?
    ) =
      c(false)

    override fun reject(message: String?) = c(false)

    override fun resolve(value: Any?) = c(true)
  }
}
