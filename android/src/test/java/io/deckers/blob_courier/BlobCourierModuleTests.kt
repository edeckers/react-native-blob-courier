/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

import androidx.test.core.app.ApplicationProvider
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.DEFAULT_PROMISE_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.Fixtures
import io.deckers.blob_courier.Fixtures.BooleanPromise
import io.deckers.blob_courier.Fixtures.createValidTestFetchParameterMap
import io.deckers.blob_courier.Fixtures.createValidUploadTestParameterMap
import io.deckers.blob_courier.Fixtures.runFetchBlob
import io.deckers.blob_courier.Fixtures.runUploadBlob
import io.deckers.blob_courier.TestUtils.createSublistsFromList
import io.deckers.blob_courier.toReactMap
import io.mockk.every
import io.mockk.mockkStatic
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BlobCourierModuleTests {
  @Before
  fun mockSomeNativeOnlyMethods() {
    mockkStatic(Arguments::class)

    every { Arguments.createMap() } answers { JavaOnlyMap() }
  }

  @Test
  fun missing_required_fetch_parameters_rejects_fetch_promise() {
    val allValuesMapping = createValidTestFetchParameterMap()
    val parameterValuePairs = allValuesMapping.entries.map { it.key to it.value }
    val oneTooFew = allValuesMapping.count() - 1

    val setsOfParametersMissingSinglePair = createSublistsFromList(parameterValuePairs, oneTooFew)

    setsOfParametersMissingSinglePair.forEach {
      assert_missing_required_fetch_parameter_rejects_promise(it, allValuesMapping)
    }
  }

  @Test
  fun all_required_fetch_parameters_provided_resolves_promise() {
    val allRequiredParametersMap = createValidTestFetchParameterMap().toReactMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    var result = Pair(false, "Unknown")

    val pool = Executors.newSingleThreadExecutor()

    val threadLock = Object()
    val finishThread = { succeeded: Boolean, message: String ->
      synchronized(threadLock) {
        threadLock.notify()
        result = Pair(succeeded, message)
      }
    }

    pool.execute {
      synchronized(threadLock) {
        runFetchBlob(
          ctx, allRequiredParametersMap,
          Fixtures.EitherPromise(
            { m0 -> finishThread(false, "Failed fetch: $m0") },
            { finishThread(true, "Success") }
          )
        )
        threadLock.wait()
      }
    }

    pool.shutdown()

    if (!pool.awaitTermination(DEFAULT_PROMISE_TIMEOUT_MILLISECONDS * 1L, TimeUnit.MILLISECONDS)) {
      pool.shutdownNow()
      assertTrue(
        "Test execution exceeded $DEFAULT_PROMISE_TIMEOUT_MILLISECONDS milliseconds", false
      )
      return
    }

    assertTrue(result.second, result.first)
  }

  @Test
  fun all_required_parameters_provided_resolves_upload_promise() {
    val allRequiredParametersMap = createValidTestFetchParameterMap().toReactMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    var result = Pair(false, "Unknown")

    val pool = Executors.newSingleThreadExecutor()

    val threadLock = Object()
    val finishThread = { succeeded: Boolean, message: String ->
      synchronized(threadLock) {
        threadLock.notify()
        result = Pair(succeeded, message)
      }
    }

    pool.execute {
      synchronized(threadLock) {
        runFetchBlob(
          ctx,
          allRequiredParametersMap,
          Fixtures.EitherPromise(
            { m0 -> finishThread(false, "Failed fetch step: $m0") },
            { r0 ->
              val taskId = allRequiredParametersMap.getString("taskId") ?: ""
              val absoluteFilePath = r0?.getMap("data")?.getString("absoluteFilePath") ?: ""

              val uploadParametersMap =
                createValidUploadTestParameterMap(taskId, absoluteFilePath).toReactMap()
              runUploadBlob(
                ctx, uploadParametersMap,
                Fixtures.EitherPromise(
                  { m1 -> finishThread(false, "Failed upload: $m1") },
                  { finishThread(true, "Success") }
                )
              )
            }
          )
        )
        threadLock.wait()
      }
    }

    pool.shutdown()

    if (!pool.awaitTermination(DEFAULT_PROMISE_TIMEOUT_MILLISECONDS * 1L, TimeUnit.MILLISECONDS)) {
      pool.shutdownNow()
      assertTrue(
        "Test execution exceeded $DEFAULT_PROMISE_TIMEOUT_MILLISECONDS milliseconds", false
      )
      return
    }

    assertTrue(result.second, result.first)
  }

  @Test
  fun missing_required_upload_parameters_rejects_fetch_promise() {
    val allValuesMapping = createValidUploadTestParameterMap("some-task-id", "/tmp")
    val parameterValuePairs = allValuesMapping.entries.map { it.key to it.value }
    val oneTooFew = allValuesMapping.count() - 1

    val setsOfParametersMissingSinglePair = createSublistsFromList(parameterValuePairs, oneTooFew)

    // setsOfParametersMissingSinglePair.forEach {
    //   assert_missing_required_upload_parameter_rejects_promise(it, allValuesMapping)
    // }
  }

  private fun assert_missing_required_fetch_parameter_rejects_promise(
    availableParameters: List<Pair<String, String>>,
    allValuesMapping: Map<String, String>
  ) {
    val availableParametersAsMap = availableParameters.toMap().toReactMap()

    availableParameters.forEach { availableParametersAsMap.putString(it.first, it.second) }

    val missingValues = allValuesMapping.keys.minus(availableParameters.map { it.first })
    println("Missing values: ${missingValues.joinToString()}")

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    runFetchBlob(ctx, availableParametersAsMap, BooleanPromise { v -> assertFalse(v) })
  }

  private fun assert_missing_required_upload_parameter_rejects_promise(
    availableParameters: List<Pair<String, String>>,
    allValuesMapping: Map<String, Any>
  ) {
    val allFetchParametersMap = createValidTestFetchParameterMap()
    val availableParametersAsMap = availableParameters.toMap().toReactMap()

    availableParameters.forEach { availableParametersAsMap.putString(it.first, it.second) }

    val missingValues = allValuesMapping.keys.minus(availableParameters.map { it.first })
    println("Missing values: ${missingValues.joinToString()}")

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    var result = Pair(false, "Unknown")

    val pool = Executors.newSingleThreadExecutor()

    val threadLock = Object()
    val finishThread = { succeeded: Boolean, message: String ->
      synchronized(threadLock) {
        threadLock.notify()
        result = Pair(succeeded, message)
      }
    }

    pool.execute {
      synchronized(threadLock) {
        runFetchBlob(
          ctx,
          allFetchParametersMap.toReactMap(),
          Fixtures.EitherPromise(
            { m0 -> finishThread(false, "Failed fetch step: $m0") },
            { r0 ->
              val taskId = allFetchParametersMap["taskId"] ?: ""
              val absoluteFilePath = r0?.getMap("data")?.getString("absoluteFilePath") ?: ""

              val uploadParametersMap =
                createValidUploadTestParameterMap(taskId, absoluteFilePath).toReactMap()
              runUploadBlob(
                ctx, uploadParametersMap,
                Fixtures.EitherPromise(
                  { m1 -> finishThread(false, "Failed upload: $m1") },
                  { finishThread(true, "Success") }
                )
              )
            }
          )
        )
      }
      threadLock.wait()
    }

    pool.shutdown()

    if (!pool.awaitTermination(DEFAULT_PROMISE_TIMEOUT_MILLISECONDS * 1L, TimeUnit.MILLISECONDS)) {
      pool.shutdownNow()
      assertTrue(
        "Test execution exceeded $DEFAULT_PROMISE_TIMEOUT_MILLISECONDS milliseconds", false
      )
      return
    }

    assertTrue(result.second, result.first)
  }
}
