/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import com.facebook.react.bridge.ReadableMap
import io.deckers.blob_courier.BuildConfig.PROMISE_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.common.Either
import io.deckers.blob_courier.common.fold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout
import org.junit.Assert

object TestUtils {
  suspend fun runRequest(
    block: suspend CoroutineScope.() -> Either<Fixtures.TestPromiseError, ReadableMap>,
    timeoutMilliseconds: Long = PROMISE_TIMEOUT_MILLISECONDS
  ) =
    withTimeout(timeoutMilliseconds, block)

  suspend fun runRequestToBoolean(
    block: suspend CoroutineScope.() -> Either<Fixtures.TestPromiseError, ReadableMap>,
    timeoutMilliseconds: Long = PROMISE_TIMEOUT_MILLISECONDS
  ) =
    runRequest(block, timeoutMilliseconds).fold({ e ->
      Pair(false, e.message ?: "")
    }, { m -> Pair(true, "$m") })

  suspend fun runInstrumentedRequestToBoolean(
    block: suspend CoroutineScope.() -> Either<Fixtures.TestPromiseError, ReadableMap>,
  ) =
    runRequest(block, PROMISE_TIMEOUT_MILLISECONDS)
      .fold({ e -> Pair(false, e.message ?: "") }, { m -> Pair(true, "$m") })

  fun assertRequestFalse(message: String, b: Boolean) =
    Assert.assertFalse("Resolves, but expected reject: $message", b)

  fun assertRequestTrue(message: String, b: Boolean) =
    Assert.assertTrue("Rejects, but expected resolve: $message", b)
}
