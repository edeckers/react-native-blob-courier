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
import io.deckers.blob_courier.common.Either
import io.deckers.blob_courier.common.left
import io.deckers.blob_courier.common.right
import io.deckers.blob_courier.react.toReactMap
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

const val DEFAULT_PROMISE_TIMEOUT_MILLISECONDS = 10_000L
const val DEFAULT_PROMISE_INSTRUMENTED_TIMEOUT_MILLISECONDS = 15_000L

object Fixtures {

  fun createValidTestFetchParameterMap(): Map<String, String> = mapOf(
    "taskId" to "some-task-id",
    "filename" to "some-filename.png",
    "url" to "https://github.com/edeckers/react-native-blob-courier"
  )

  fun createValidUploadTestParameterMap(
    taskId: String,
    localPath: String
  ) = TestUploadParameterMap(
    taskId,
    arrayOf(
      mapOf(
        "name" to "file",
        "payload" to mapOf(
          "absoluteFilePath" to localPath,
          "mimeType" to "text/html"
        ),
        "type" to "file"
      )
    ),
    "https://file.io"
  )

  suspend fun runFetchBlobSuspend(
    context: ReactApplicationContext,
    input: ReadableMap,
  ): Either<String, ReadableMap> {
    val m = BlobCourierModule(context)

    var result: Either<String, ReadableMap>? = null

    m.fetchBlob(
      input,
      EitherPromise(
        { e -> result = left(e ?: "Request failed without a message") },
        { v -> result = right(v ?: emptyMap<String, Any>().toReactMap()) }
      )
    )

    while (result == null) {
      coroutineContext.ensureActive()
      delay(1)
    }

    return result ?: left("Did not receive a response in time")
  }

  suspend fun runUploadBlobSuspend(
    context: ReactApplicationContext,
    input: ReadableMap
  ): Either<String, ReadableMap> {
    val m = BlobCourierModule(context)

    var result: Either<String, ReadableMap>? = null

    m.uploadBlob(
      input,
      EitherPromise(
        { e -> result = left(e ?: "Request failed without a message") },
        { v -> result = right(v ?: emptyMap<String, Any>().toReactMap()) }
      )
    )

    while (result == null) {
      coroutineContext.ensureActive()
      delay(1)
    }

    return result ?: left("Did not receive a response in time")
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
}
