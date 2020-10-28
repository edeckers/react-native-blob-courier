package io.deckers.blob_courier

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import kotlin.concurrent.thread
import org.robolectric.RuntimeEnvironment

private const val DEFAULT_PROMISE_TIMEOUT_MILLISECONDS = 2_000L

fun create_valid_test_parameter_map(): Map<String, String> = mapOf(
  "taskId" to "some-task-id",
  "filename" to "some-filename.png",
  "url" to "https://github.com/edeckers/react-native-blob-courier"
)

fun run_fetch_blob(
  input: ReadableMap,
  promise: BooleanPromise,
  timeoutMilliseconds: Long = DEFAULT_PROMISE_TIMEOUT_MILLISECONDS
) {
  val ctx = ReactApplicationContext(RuntimeEnvironment.application)

  val m = BlobCourierModule(ctx)

  thread {
    m.fetchBlob(input, promise)
  }

  Thread.sleep(timeoutMilliseconds)
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
