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
import io.deckers.blob_courier.Fixtures
import io.deckers.blob_courier.Fixtures.createValidTestFetchParameterMap
import io.deckers.blob_courier.Fixtures.runFetchBlobSuspend
import io.deckers.blob_courier.TestUtils.assertRequestFalse
import io.deckers.blob_courier.TestUtils.assertRequestTrue
import io.deckers.blob_courier.TestUtils.circumventHiddenApiExemptionsForMockk
import io.deckers.blob_courier.TestUtils.runRequestToBoolean
import io.deckers.blob_courier.common.DOWNLOAD_TYPE_MANAGED
import io.deckers.blob_courier.common.MANAGED_DOWNLOAD_SUCCESS
import io.deckers.blob_courier.common.left
import io.deckers.blob_courier.common.right
import io.deckers.blob_courier.react.toReactMap
import io.mockk.every
import io.mockk.mockkStatic
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val ADB_COMMAND_DELAY_MILLISECONDS = 5_000L

private fun enableNetworking(enable: Boolean) {
  val word = if (enable) "enable" else "disable"

  InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("svc wifi $word")
  InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("svc data $word")
}

class BlobCourierInstrumentedModuleTests {
  @After
  fun restoreExpectedState() = runBlocking {
    enableNetworking(true)

    delay(ADB_COMMAND_DELAY_MILLISECONDS)
  }

  @Before
  fun mockSomeNativeOnlyMethods() {
    circumventHiddenApiExemptionsForMockk()

    mockkStatic(Arguments::class)

    every { Arguments.createMap() } answers { JavaOnlyMap() }
    every { Arguments.createArray() } answers { JavaOnlyArray() }
  }

//  @Ignore("This breaks on GitHub Actions due to timeout")
  @Test
  fun managed_download_succeeds() = runBlocking {
    val allRequiredParametersMap = createValidTestFetchParameterMap().toReactMap()
    val androidSettings = mapOf(
      "useDownloadManager" to true
    )
      .toReactMap()
    allRequiredParametersMap.putMap("android", androidSettings)

    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val reactContext = ReactApplicationContext(ctx)

    val (succeeded, message) = runRequestToBoolean({
      delay(ADB_COMMAND_DELAY_MILLISECONDS)

      runFetchBlobSuspend(
        reactContext,
        allRequiredParametersMap
      )
    })

    assertRequestTrue(message, succeeded)
  }

//  @Ignore("This breaks on GitHub Actions due to timeout")
  @Test
  fun managed_download_returns_correct_type() = runBlocking {
    val allRequiredParametersMap = createValidTestFetchParameterMap().toReactMap()
    val androidSettings = mapOf(
      "useDownloadManager" to true
    )
      .toReactMap()

    allRequiredParametersMap.putMap("android", androidSettings)

    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val reactContext = ReactApplicationContext(ctx)

    val (succeeded, message) = runRequestToBoolean({
      delay(ADB_COMMAND_DELAY_MILLISECONDS)

      runFetchBlobSuspend(reactContext, allRequiredParametersMap)
        .fmap { result ->
          val receivedType = result.getString("type") ?: ""
          val check = receivedType == DOWNLOAD_TYPE_MANAGED

          if (check) right(result) else left("Received incorrect type `$receivedType`")
        }
    })

    assertRequestTrue(message, succeeded)
  }

  // @Ignore("This breaks on GitHub Actions due to timeout")
  @Test
  fun managed_download_returns_expected_result() = runBlocking {
    val allRequiredParametersMap = createValidTestFetchParameterMap().toReactMap()
    val androidSettings = mapOf(
      "useDownloadManager" to true
    )
      .toReactMap()
    allRequiredParametersMap.putMap("android", androidSettings)

    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val reactContext = ReactApplicationContext(ctx)

    val (succeeded, message) = runRequestToBoolean({
      delay(ADB_COMMAND_DELAY_MILLISECONDS)

      runFetchBlobSuspend(reactContext, allRequiredParametersMap)
        .fmap { result ->
          val receivedResult = result.getMap("data")?.getString("result") ?: ""
          val check = receivedResult == MANAGED_DOWNLOAD_SUCCESS

          if (check) right(result) else left("Received incorrect result `$receivedResult`")
        }
    })

    assertRequestTrue(message, succeeded)
  }

  @Test
  fun uploading_a_file_from_outside_app_data_directory_resolves_promise() = runBlocking {
    val someFileThatIsAlwaysAvailable = "file:///system/etc/fonts.xml"

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val uploadParametersMap =
      Fixtures.createValidUploadTestParameterMap(
        UUID.randomUUID().toString(),
        someFileThatIsAlwaysAvailable
      )

    val (succeeded, message) = runRequestToBoolean({
      delay(ADB_COMMAND_DELAY_MILLISECONDS)

      Fixtures.runUploadBlobSuspend(ctx, uploadParametersMap.toReactMap())
    })

    assertRequestTrue(message, succeeded)
  }

  @Test
  fun non_existing_uploadable_file_rejects_promise() = runBlocking {
    val irrelevantTaskId = UUID.randomUUID().toString()
    val someNonExistentPath = "file:///this/path/does/not/exist.png"
    val allRequiredParametersMap =
      Fixtures.createValidUploadTestParameterMap(irrelevantTaskId, someNonExistentPath)

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val (succeeded, message) = runRequestToBoolean({
      delay(ADB_COMMAND_DELAY_MILLISECONDS)

      Fixtures.runUploadBlobSuspend(ctx, allRequiredParametersMap.toReactMap())
    })

    assertRequestFalse(message, succeeded)
  }

  //  @Ignore("This breaks on GitHub Actions due to timeout")
  @Test
  fun no_network_connection_rejects_promise() = runBlocking {
    val allRequiredParametersMap = createValidTestFetchParameterMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    enableNetworking(false)

    val (succeeded, message) = runRequestToBoolean({
      delay(ADB_COMMAND_DELAY_MILLISECONDS)

      runFetchBlobSuspend(ctx, allRequiredParametersMap.toReactMap())
    })

    assertRequestFalse(message, succeeded)
  }
}
