/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.DEFAULT_PROMISE_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.DOWNLOAD_TYPE_MANAGED
import io.deckers.blob_courier.Fixtures
import io.deckers.blob_courier.Fixtures.createValidTestFetchParameterMap
import io.deckers.blob_courier.Fixtures.runFetchBlob
import io.deckers.blob_courier.MANAGED_DOWNLOAD_SUCCESS
import io.deckers.blob_courier.TestUtils.circumventHiddenApiExemptionsForMockk
import io.deckers.blob_courier.toReactMap
import io.mockk.every
import io.mockk.mockkStatic
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class BlobCourierInstrumentedModuleTests {
  @Before
  fun mockSomeNativeOnlyMethods() {
    circumventHiddenApiExemptionsForMockk()

    mockkStatic(Arguments::class)

    every { Arguments.createMap() } answers { JavaOnlyMap() }
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
}
