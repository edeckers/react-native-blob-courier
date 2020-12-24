/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.JavaOnlyArray
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.DEFAULT_PROMISE_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.Fixtures
import io.deckers.blob_courier.Fixtures.createValidTestFetchParameterMap
import io.deckers.blob_courier.Fixtures.runFetchBlob
import io.deckers.blob_courier.TestUtils.circumventHiddenApiExemptionsForMockk
import io.deckers.blob_courier.common.DOWNLOAD_TYPE_MANAGED
import io.deckers.blob_courier.common.MANAGED_DOWNLOAD_SUCCESS
import io.deckers.blob_courier.common.toReactMap
import io.mockk.every
import io.mockk.mockkStatic
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

private fun enableNetworking(enable: Boolean) {
  val word = if (enable) "enable" else "disable"

  InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("svc wifi $word")
  InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("svc data $word")
}

class BlobCourierInstrumentedModuleTests {
  @After
  fun restoreExpectedState() {
    enableNetworking(true)
    Thread.sleep(1_000)
  }

  @Before
  fun mockSomeNativeOnlyMethods() {
    circumventHiddenApiExemptionsForMockk()

    mockkStatic(Arguments::class)

    every { Arguments.createMap() } answers { JavaOnlyMap() }
    every { Arguments.createArray() } answers { JavaOnlyArray() }
  }

  @Test
  fun managed_download_succeeds() {
    val allRequiredParametersMap = createValidTestFetchParameterMap().toReactMap()

    allRequiredParametersMap.putBoolean("useDownloadManager", true)

    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val reactContext = ReactApplicationContext(ctx)

    runFetchBlob(
      reactContext,
      allRequiredParametersMap,
      Fixtures.EitherPromise(
        { message -> println(message) },
        { result ->
          assert(result?.getString("type") == DOWNLOAD_TYPE_MANAGED)
          assert(result?.getMap("data")?.getString("result") == MANAGED_DOWNLOAD_SUCCESS)
        }
      )
    )
  }

  @Test
  fun uploading_a_file_from_outside_app_data_directory_resolves_promise() {
    val someFileThatIsAlwaysAvailable = "file:///system/etc/fonts.xml"

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
        val uploadParametersMap =
          Fixtures.createValidUploadTestParameterMap(
            UUID.randomUUID().toString(),
            someFileThatIsAlwaysAvailable
          ).toReactMap()

        Fixtures.runUploadBlob(
          ctx,
          uploadParametersMap,
          Fixtures.EitherPromise(
            { m1 -> finishThread(false, "Failed upload: $m1") },
            { finishThread(true, "Success") }
          )
        )
        threadLock.wait()
      }
    }

    pool.shutdown()

    if (!pool.awaitTermination(DEFAULT_PROMISE_TIMEOUT_MILLISECONDS * 1L, TimeUnit.MILLISECONDS)) {
      pool.shutdownNow()
      Assert.assertTrue(
        "Test execution exceeded $DEFAULT_PROMISE_TIMEOUT_MILLISECONDS milliseconds", false
      )
      return
    }

    Assert.assertTrue(result.second, result.first)
  }

  @Test // This is the faster, and less thorough version of the Instrumented test with the same name
  fun non_existing_uploadable_file_rejects_promise() {
    val irrelevantTaskId = UUID.randomUUID().toString()
    val someNonExistentPath = "file:///this/path/does/not/exist.png"
    val allRequiredParametersMap =
      Fixtures.createValidUploadTestParameterMap(irrelevantTaskId, someNonExistentPath)

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
        Fixtures.runUploadBlob(
          ctx,
          allRequiredParametersMap.toReactMap(),
          Fixtures.EitherPromise(
            { m0 -> finishThread(true, "Success: $m0") },
            { m0 -> finishThread(false, "Resolved but expected reject: $m0") }
          )
        )
        threadLock.wait()
      }
    }

    pool.shutdown()

    if (!pool.awaitTermination(DEFAULT_PROMISE_TIMEOUT_MILLISECONDS * 1L, TimeUnit.MILLISECONDS)) {
      pool.shutdownNow()
      Assert.assertTrue(
        "Test execution exceeded $DEFAULT_PROMISE_TIMEOUT_MILLISECONDS milliseconds", false
      )
      return
    }

    Assert.assertTrue(result.second, result.first)
  }

  @Test(timeout = DEFAULT_PROMISE_TIMEOUT_MILLISECONDS)
  fun no_network_connection_rejects_promise() {
    val allRequiredParametersMap = createValidTestFetchParameterMap().toReactMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    var result = Pair(false, "Unknown")

    val pool = Executors.newSingleThreadScheduledExecutor()

    val threadLock = Object()
    val finishThread = { succeeded: Boolean, message: String ->
      synchronized(threadLock) {
        threadLock.notify()
        result = Pair(succeeded, message)
      }
    }

    enableNetworking(false)

    pool.schedule(
      {
        synchronized(threadLock) {
          runFetchBlob(
            ctx,
            allRequiredParametersMap,
            Fixtures.EitherPromise(
              { m0 -> finishThread(true, "Success: $m0") },
              { m0 -> finishThread(false, "Resolved but expected reject: $m0") }
            )
          )
          threadLock.wait()
        }
      },
      1_000, TimeUnit.MILLISECONDS
    )

    pool.shutdown()

    if (!pool.awaitTermination(DEFAULT_PROMISE_TIMEOUT_MILLISECONDS * 1L, TimeUnit.MILLISECONDS)) {
      pool.shutdownNow()
      Assert.assertTrue(
        "Test execution exceeded $DEFAULT_PROMISE_TIMEOUT_MILLISECONDS milliseconds", false
      )
      return
    }

    Assert.assertTrue(result.second, result.first)
  }
}
