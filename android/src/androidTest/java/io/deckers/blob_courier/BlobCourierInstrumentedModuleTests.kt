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
import io.deckers.blob_courier.BuildConfig.ADB_COMMAND_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.BuildConfig.PROMISE_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.Fixtures
import io.deckers.blob_courier.Fixtures.createSparseFile
import io.deckers.blob_courier.Fixtures.createValidTestFetchParameterMap
import io.deckers.blob_courier.Fixtures.createValidUploadTestParameterMap
import io.deckers.blob_courier.Fixtures.runCancelFetchBlobSuspend
import io.deckers.blob_courier.Fixtures.runCancelUploadBlobSuspend
import io.deckers.blob_courier.Fixtures.runFetchBlobSuspend
import io.deckers.blob_courier.TestUtils
import io.deckers.blob_courier.TestUtils.assertRequestFalse
import io.deckers.blob_courier.TestUtils.assertRequestTrue
import io.deckers.blob_courier.TestUtils.runInstrumentedRequestToBoolean
import io.deckers.blob_courier.common.DOWNLOAD_TYPE_MANAGED
import io.deckers.blob_courier.common.ERROR_CANCELED_EXCEPTION
import io.deckers.blob_courier.common.Logger
import io.deckers.blob_courier.common.MANAGED_DOWNLOAD_SUCCESS
import io.deckers.blob_courier.common.fold
import io.deckers.blob_courier.common.ifLeft
import io.deckers.blob_courier.common.left
import io.deckers.blob_courier.common.right
import io.deckers.blob_courier.react.toReactMap
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.UUID


private val TAG = BlobCourierInstrumentedModuleTests::class.java.name

private val logger = Logger(TAG)
private fun li(m: String) = logger.i(m)
private fun lv(m: String, e: Throwable? = null) = logger.v(m, e)

private suspend fun waitForDisabledNetwork() =
  withContext(Dispatchers.IO) {
    while (true) {
      lv("Test if known host is reachable")
      val p = Runtime.getRuntime().exec("ping github.com")

      p.errorStream.bufferedReader().use {
        val line = it.readLine()
        li("Read line $line")
        if (line?.contains("unknown") == true) {
          li("Line contains 'unknown'; network is disabled")
          return@withContext
        }

        // if (line?.contains("unreachable") == true) {
        //   li("Line contains 'unreachable'; network is disabled")
        //   return@withContext
        // }
      }

      if (!p.isAlive && p.exitValue() == 2) {
        li("Process exited with non-zero value; network is disabled")
        return@withContext
      }

      lv("Network still active, backing off for a few milliseconds before retry")
      delay(10)
    }
  }

private suspend fun waitForEnabledNetwork() =
  withContext(Dispatchers.IO) {
    while (true) {
      lv("Test if known host is reachable")
      val p: Process = Runtime.getRuntime().exec("ping github.com")

      p.inputStream.bufferedReader().use {
        while (p.isAlive) {
          val line = it.readLine()
          li("Read line $line")
          if (line?.contains("PING github.com") == true) {
            li("Line contains valid PING-response'; network is enabled")
            return@withContext
          }

          lv("Network still inactive, backing off for a few milliseconds before retry")
          delay(10)
        }
      }

      if (!p.isAlive && p.exitValue() == 0) {
        li("Process exited with zero value; network is enabled")
        return@withContext
      }
    }
  }

private fun toggleNetworking(enable: Boolean) {
  val word = if (enable) "enable" else "disable"

  li("Toggling network (toggle=$word)")

  InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("svc wifi $word")
  InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("svc data $word")
  li("Toggled network (toggle=$word)")

  runBlocking {
    li("Waiting for network status to settle (timeout=$ADB_COMMAND_TIMEOUT_MILLISECONDS)")

    try {
      withTimeout(ADB_COMMAND_TIMEOUT_MILLISECONDS) {
        if (enable) waitForEnabledNetwork() else waitForDisabledNetwork()
      }
    } catch (e: TimeoutCancellationException) {
      li("Network status did not change in due time (timeout=$ADB_COMMAND_TIMEOUT_MILLISECONDS)")

      throw e
    } finally {
      li("Finished waiting for network status to settle")
    }
  }
}

class BlobCourierInstrumentedModuleTests {
  @Before
  fun logTestContextInformation() {
    li("Setting test timeout (timeout=${PROMISE_TIMEOUT_MILLISECONDS}ms)")
    li("Setting adb command timeout (timeout=${ADB_COMMAND_TIMEOUT_MILLISECONDS}ms)")
  }

  @Before
  fun mockSomeNativeOnlyMethods() {
    li("Restore method mocks")

    mockkStatic(Arguments::class)

    every { Arguments.createMap() } answers { JavaOnlyMap() }
    every { Arguments.createArray() } answers { JavaOnlyArray() }
  }

  @Before
  fun restoreNetworkingState() {
    li("Restore networking state")
    toggleNetworking(true)
  }

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

    val (succeeded, message) = runInstrumentedRequestToBoolean {
      toggleNetworking(true)

      runFetchBlobSuspend(reactContext, allRequiredParametersMap)
    }

    assertRequestTrue(message, succeeded)
  }

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

    val (succeeded, message) = runInstrumentedRequestToBoolean {
      toggleNetworking(true)

      runFetchBlobSuspend(reactContext, allRequiredParametersMap)
        .fmap { result ->
          val receivedType = result.getString("type") ?: ""
          val check = receivedType == DOWNLOAD_TYPE_MANAGED

          if (check)
            right(result)
          else left(Fixtures.TestPromiseError(message = "Received incorrect type `$receivedType`"))
        }
    }

    assertRequestTrue(message, succeeded)
  }

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

    val (succeeded, message) = runInstrumentedRequestToBoolean {
      toggleNetworking(true)

      runFetchBlobSuspend(reactContext, allRequiredParametersMap)
        .fmap { result ->
          val receivedResult = result.getMap("data")?.getString("result") ?: ""
          val check = receivedResult == MANAGED_DOWNLOAD_SUCCESS

          if (check)
            right(result)
          else left(Fixtures.TestPromiseError(message = "Received incorrect result `$receivedResult`"))
        }
    }

    assertRequestTrue(message, succeeded)
  }

  @Test
  fun cancel_successful_fetch_request_resolves_promise() = runBlocking {
    val allRequiredParametersMap =
      createValidTestFetchParameterMap().plus("url" to Fixtures.LARGE_FILE).toReactMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val errorOrResult =
      TestUtils.runRequest(
        { runCancelFetchBlobSuspend(ctx, allRequiredParametersMap) },
        ADB_COMMAND_TIMEOUT_MILLISECONDS)

    val isError = errorOrResult.map { false }.ifLeft(true)
    val code = errorOrResult.fold({ e -> e.code }, { _ -> "INVALID" })

    Assert.assertTrue(isError)
    Assert.assertEquals(ERROR_CANCELED_EXCEPTION, code)
  }

  @Test
  fun cancel_successful_upload_request_resolves_promise() = runBlocking {
    val file = createSparseFile(100 * 1024 * 1024)

    val taskId = UUID.randomUUID().toString()

    val allRequiredParametersMap =
      createValidUploadTestParameterMap(taskId, file.absolutePath)

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val errorOrResult = TestUtils.runRequest(
      { runCancelUploadBlobSuspend(ctx, allRequiredParametersMap.toReactMap()) },
      60_000)

    val isError = errorOrResult.map { false }.ifLeft(true)
    val code = errorOrResult.fold({ e -> e.code }, { _ -> "INVALID" })

    Assert.assertTrue(isError)
    Assert.assertEquals(ERROR_CANCELED_EXCEPTION, code)
  }

  @Test
  fun uploading_a_file_from_outside_app_data_directory_resolves_promise() = runBlocking {
    val someFileThatIsAlwaysAvailable = "file:///system/etc/fonts.xml"

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val uploadParametersMap =
      createValidUploadTestParameterMap(
        UUID.randomUUID().toString(),
        someFileThatIsAlwaysAvailable
      )

    val (succeeded, message) = runInstrumentedRequestToBoolean {
      toggleNetworking(true)

      Fixtures.runUploadBlobSuspend(ctx, uploadParametersMap.toReactMap())
    }

    assertRequestTrue(message, succeeded)
  }

  @Test
  fun non_existing_uploadable_file_rejects_promise() = runBlocking {
    val irrelevantTaskId = UUID.randomUUID().toString()
    val someNonExistentPath = "file:///this/path/does/not/exist.png"
    val allRequiredParametersMap =
      createValidUploadTestParameterMap(irrelevantTaskId, someNonExistentPath)

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val (succeeded, message) = runInstrumentedRequestToBoolean {
      toggleNetworking(true)

      Fixtures.runUploadBlobSuspend(ctx, allRequiredParametersMap.toReactMap())
    }

    assertRequestFalse(message, succeeded)
  }

  @Test
  fun no_network_connection_rejects_promise() = runBlocking {
    val allRequiredParametersMap = createValidTestFetchParameterMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val (succeeded, message) = runInstrumentedRequestToBoolean {
      toggleNetworking(false)

      runFetchBlobSuspend(ctx, allRequiredParametersMap.toReactMap())
    }

    assertRequestFalse(message, succeeded)
  }
}
