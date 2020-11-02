/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.DOWNLOAD_TYPE_MANAGED
import io.deckers.blob_courier.Fixtures
import io.deckers.blob_courier.Fixtures.createValidTestFetchParameterMap
import io.deckers.blob_courier.Fixtures.runFetchBlob
import io.deckers.blob_courier.MANAGED_DOWNLOAD_SUCCESS
import io.deckers.blob_courier.TestUtils.circumventHiddenApiExemptionsForMockk
import io.deckers.blob_courier.toReactMap
import io.mockk.every
import io.mockk.mockkStatic
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
}
