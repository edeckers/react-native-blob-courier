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
import java.io.File
import java.io.RandomAccessFile
import java.io.Serializable

object Fixtures {

  const val LARGE_FILE = "http://ipv4.download.thinkbroadband.com/100MB.zip"

  fun createValidTestFetchParameterMap(): Map<String, Serializable> = mapOf(
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

  @Suppress("SameParameterValue")
  fun createSparseFile(fileSize: Long): File {
    val file = File.createTempFile("blob_courier.", ".bin")

    val f = RandomAccessFile(file.absoluteFile, "rw")
    f.setLength(fileSize)

    return file
  }

  suspend fun runCancelFetchBlobSuspend(
    context: ReactApplicationContext,
    input: ReadableMap,
  ): Either<TestPromiseError, ReadableMap> {
    val m = BlobCourierModule(context)

    var result: Either<TestPromiseError, ReadableMap>? = null

    m.fetchBlob(
      input,
      EitherPromise(
        { e -> result = left(e) },
        { v -> result = right(v ?: emptyMap<String, Any>().toReactMap()) }
      )
    )

    val taskId = input.getString("taskId")
    val cancelInputParameters = mapOf("taskId" to taskId)

    while (result == null) {
      coroutineContext.ensureActive()
      runCancelBlobSuspend(context, cancelInputParameters.toReactMap())
      delay(1)
    }

    return result ?: left(TestPromiseError(message = "Did not receive a response in time"))
  }

  suspend fun runCancelUploadBlobSuspend(
    context: ReactApplicationContext,
    input: ReadableMap,
  ): Either<TestPromiseError, ReadableMap> {
    val m = BlobCourierModule(context)

    var result: Either<TestPromiseError, ReadableMap>? = null

    m.uploadBlob(
      input,
      EitherPromise(
        { e -> result = left(e) },
        { v -> result = right(v ?: emptyMap<String, Any>().toReactMap()) }
      )
    )

    val taskId = input.getString("taskId")
    val cancelInputParameters = mapOf("taskId" to taskId)

    while (result == null) {
      coroutineContext.ensureActive()
      runCancelBlobSuspend(context, cancelInputParameters.toReactMap())
      delay(1)
    }

    return result ?: left(TestPromiseError(message = "Did not receive a response in time"))
  }

  suspend fun runFetchBlobSuspend(
    context: ReactApplicationContext,
    input: ReadableMap,
  ): Either<TestPromiseError, ReadableMap> {
    val m = BlobCourierModule(context)

    var result: Either<TestPromiseError, ReadableMap>? = null

    m.fetchBlob(
      input,
      EitherPromise(
        { e -> result = left(e) },
        { v -> result = right(v ?: emptyMap<String, Any>().toReactMap()) }
      )
    )

    while (result == null) {
      coroutineContext.ensureActive()
      delay(1)
    }

    return result ?: left(TestPromiseError(message = "Did not receive a response in time"))
  }

  suspend fun runUploadBlobSuspend(
    context: ReactApplicationContext,
    input: ReadableMap
  ): Either<TestPromiseError, ReadableMap> {
    val m = BlobCourierModule(context)

    var result: Either<TestPromiseError, ReadableMap>? = null

    m.uploadBlob(
      input,
      EitherPromise(
        { e -> result = left(e) },
        { v -> result = right(v ?: emptyMap<String, Any>().toReactMap()) }
      )
    )

    while (result == null) {
      coroutineContext.ensureActive()
      delay(1)
    }

    return result ?: left(TestPromiseError(message = "Did not receive a response in time"))
  }

  suspend fun runCancelBlobSuspend(
    context: ReactApplicationContext,
    input: ReadableMap
  ): Either<TestPromiseError, ReadableMap> {
    val m = BlobCourierModule(context)

    var result: Either<TestPromiseError, ReadableMap>? = null

    m.cancelRequest(
      input,
      EitherPromise(
        { e -> result = left(e) },
        { v -> result = right(v ?: emptyMap<String, Any>().toReactMap()) }
      )
    )

    while (result == null) {
      coroutineContext.ensureActive()
      delay(1)
    }

    return result ?: left(TestPromiseError(message = "Did not receive a response in time"))
  }

  data class TestPromiseError(
    val code: String? = null,
    val message: String?,
    val throwable: Throwable? = null)

  class EitherPromise(
    val left: (error: TestPromiseError) -> Unit,
    val right: (v: ReadableMap?) -> Unit
  ) : Promise {
    override fun resolve(v: Any?) {
      val maybeValue = if (v is ReadableMap) v else null

      right(maybeValue)
    }

    override fun reject(code: String?, message: String?) = left(TestPromiseError(code, message))

    override fun reject(code: String?, throwable: Throwable?) =
      left(TestPromiseError(throwable?.javaClass?.typeName, throwable?.localizedMessage))

    override fun reject(code: String?, message: String?, throwable: Throwable?) =
      left(TestPromiseError(code, message, throwable))

    override fun reject(throwable: Throwable?) =
      left(TestPromiseError(throwable?.javaClass?.typeName, throwable?.localizedMessage, throwable))

    override fun reject(throwable: Throwable?, userInfo: WritableMap?) =
      left(TestPromiseError(throwable?.javaClass?.typeName, throwable?.localizedMessage, throwable))

    override fun reject(code: String?, userInfo: WritableMap) =
      left(TestPromiseError(code, code ?: ""))

    override fun reject(code: String?, throwable: Throwable?, userInfo: WritableMap?) =
      left(TestPromiseError(code, throwable?.localizedMessage, throwable))

    override fun reject(code: String?, message: String?, userInfo: WritableMap) =
      left(TestPromiseError(code, message))

    override fun reject(
      code: String?,
      message: String?,
      throwable: Throwable?,
      userInfo: WritableMap?
    ) =
      left(TestPromiseError(code, message, throwable))

    override fun reject(message: String?) =
      left(TestPromiseError(null, message ?: ""))
  }
}
