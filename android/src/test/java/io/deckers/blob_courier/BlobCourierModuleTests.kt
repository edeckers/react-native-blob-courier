/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.JavaOnlyArray
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.Fixtures.createValidTestFetchParameterMap
import io.deckers.blob_courier.Fixtures.createValidUploadTestParameterMap
import io.deckers.blob_courier.Fixtures.runFetchBlobSuspend
import io.deckers.blob_courier.Fixtures.runUploadBlobSuspend
import io.deckers.blob_courier.TestUtils.assertRequestFalse
import io.deckers.blob_courier.TestUtils.assertRequestTrue
import io.deckers.blob_courier.TestUtils.runRequest
import io.deckers.blob_courier.TestUtils.runRequestToBoolean
import io.deckers.blob_courier.category.EndToEnd
import io.deckers.blob_courier.category.Isolated
import io.deckers.blob_courier.category.Regression
import io.deckers.blob_courier.category.Slow
import io.deckers.blob_courier.common.DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.common.Either
import io.deckers.blob_courier.common.PARAMETER_SETTINGS_PROGRESS_INTERVAL
import io.deckers.blob_courier.common.ValidationError
import io.deckers.blob_courier.common.fold
import io.deckers.blob_courier.common.isNotNull
import io.deckers.blob_courier.common.isNotNullOrEmptyString
import io.deckers.blob_courier.common.validate
import io.deckers.blob_courier.react.CongestionAvoidingProgressNotifier
import io.deckers.blob_courier.react.CongestionAvoidingProgressNotifierFactory
import io.deckers.blob_courier.react.toReactMap
import io.deckers.blob_courier.upload.InputStreamRequestBody
import io.deckers.blob_courier.upload.UploaderParameterFactory
import io.deckers.blob_courier.upload.toMultipartBody
import io.mockk.EqMatcher
import io.mockk.OfTypeMatcher
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

const val SOME_FILE_THAT_IS_ALWAYS_AVAILABLE = "file:///system/etc/fonts.xml"

@Suppress("SameParameterValue")
private fun <T : Any, V> assertTypeOf(message: String, o: T, t: Class<V>) =
  assertSame(message, o::class.java, t)

private fun mapMultipartsToNames(parts: List<MultipartBody.Part>) =
  parts.fold(
    emptyArray(),
    { names: Array<String>, part: MultipartBody.Part ->
      val contentDisposition = part.headers?.get("Content-Disposition")

      val matches = contentDisposition?.let(Regex("name=\"(\\w+)\"")::find)

      val fieldName = matches?.destructured?.toList()?.first()

      fieldName?.let(names::plus) ?: names
    }
  )

private fun retrieveMissingKeys(
  expected: Map<*, *>,
  actual: Map<*, *>,
  prefix: String = ""
): List<String> =
  expected.keys
    .fold(
      listOf(),
      { p, c ->
        if (!actual.containsKey(c))
          p.plus("$prefix$c") else if (expected[c] is Map<*, *>)
          p.plus(
            retrieveMissingKeys(
              expected[c] as Map<*, *>,
              actual[c] as Map<*, *>,
              "$prefix$c."
            )
          ) else
          p
      }
    )

private fun createAllSingleMissingKeyCombinations(m: Map<*, *>): List<Map<Any?, Any?>> =
  m.keys.flatMap { k0 ->
    val dictWithoutKey0 = m.filterKeys { k -> k != k0 }

    val d0 =
      dictWithoutKey0.keys
        .filter { dictWithoutKey0[it] is Map<*, *> }
        .flatMap { k1 ->
          createAllSingleMissingKeyCombinations(dictWithoutKey0[k1] as Map<*, *>)
            .map { d -> dictWithoutKey0.plus(Pair(k1, d)) }
        }

    listOf(dictWithoutKey0).plus(d0)
  }

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BlobCourierModuleTests {
  @Before
  fun mockSomeNativeOnlyMethods() {
    mockkStatic(Arguments::class)

    every { Arguments.createMap() } answers { JavaOnlyMap() }
    every { Arguments.createArray() } answers { JavaOnlyArray() }
  }

  @Category(Isolated::class)
  @Test
  fun missing_required_fetch_parameters_rejects_fetch_promise() {
    val allValuesMapping = createValidTestFetchParameterMap()

    val missingKeyCombinations = createAllSingleMissingKeyCombinations(allValuesMapping)

    missingKeyCombinations.forEach(::assert_missing_required_fetch_parameter_rejects_promise)
  }

  @Category(EndToEnd::class)
  @Test
  fun all_required_fetch_parameters_provided_resolves_promise() = runBlocking {
    val allRequiredParametersMap = createValidTestFetchParameterMap().toReactMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())
    val (succeeded, message) =
      runRequestToBoolean({ runFetchBlobSuspend(ctx, allRequiredParametersMap) })

    assertRequestTrue(message, succeeded)
  }


  @Category(EndToEnd::class)
  @Test
  fun unreachable_fetch_server_rejects_promise() = runBlocking {
    val allRequiredParametersMap = createValidTestFetchParameterMap()
    val requestWithNonExistentUrl =
      allRequiredParametersMap.plus(Pair("url", "http://127.0.0.1:12345")).toReactMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val (succeeded, message) =
      runRequestToBoolean({ runFetchBlobSuspend(ctx, requestWithNonExistentUrl) })

    assertRequestFalse(message, succeeded)
  }

  @Category(EndToEnd::class, Slow::class)
  @Test
  fun non_ok_http_fetch_response_resolves_promise() = runBlocking {
    val allRequiredParametersMap = createValidTestFetchParameterMap()
    val requestWithNonExistentUrl =
      allRequiredParametersMap.plus(
        Pair("url", "https://github.com/edeckers/this-does-not-exist")
      ).toReactMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val (succeeded, message) =
      runRequestToBoolean({ runFetchBlobSuspend(ctx, requestWithNonExistentUrl) })

    assertRequestTrue(message, succeeded)
  }


  @Category(EndToEnd::class)
  @Test
  fun all_required_parameters_provided_resolves_upload_promise() = runBlocking {
    val allRequiredParametersMap = createValidTestFetchParameterMap().toReactMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val (succeeded, message) = runRequestToBoolean({
      val errorOrResult = runFetchBlobSuspend(ctx, allRequiredParametersMap)

      errorOrResult.fmap { result ->
        val taskId = allRequiredParametersMap.getString("taskId") ?: ""
        val absoluteFilePath = result.getMap("data")?.getString("absoluteFilePath") ?: ""

        Shadows.shadowOf(ctx.contentResolver)
          .registerInputStream(Uri.parse(absoluteFilePath), "".byteInputStream())

        val uploadParametersMap =
          createValidUploadTestParameterMap(taskId, absoluteFilePath).toReactMap()

        runBlocking {
          runUploadBlobSuspend(ctx, uploadParametersMap)
        }
      }
    })

    assertRequestTrue(message, succeeded)
  }

  @Category(EndToEnd::class)
  @Test
  fun using_a_string_payload_resolves_upload_promise() = runBlocking {
    val allRequiredParametersMap = createValidTestFetchParameterMap().toReactMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val (succeeded, message) = runRequestToBoolean({
      val errorOrResult = runFetchBlobSuspend(ctx, allRequiredParametersMap)

      errorOrResult.fmap { result ->
        val taskId = allRequiredParametersMap.getString("taskId") ?: ""
        val absoluteFilePath = result.getMap("data")?.getString("absoluteFilePath") ?: ""

        Shadows.shadowOf(ctx.contentResolver)
          .registerInputStream(Uri.parse(absoluteFilePath), "".byteInputStream())

        val uploadParametersMap =
          createValidUploadTestParameterMap(taskId, absoluteFilePath)

        val defaultParts = uploadParametersMap.parts
        val partsPlusStringPayload = defaultParts.plus(
          mapOf(
            "name" to "test",
            "payload" to "THIS_IS_A_STRING_PAYLOAD",
            "type" to "string",
          )
        )

        val uploadParametersWithStringPayloadMap =
          uploadParametersMap.toMap().plus("parts" to partsPlusStringPayload)

        runBlocking {
          runUploadBlobSuspend(ctx, uploadParametersWithStringPayloadMap.toReactMap())
        }
      }
    })

    assertRequestTrue(message, succeeded)
  }

  @Category(EndToEnd::class)
  @Test
  fun non_ok_http_response_resolves_upload_promise() = runBlocking {
    val allRequiredParametersMap = createValidTestFetchParameterMap().toReactMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val (succeeded, message) = runRequestToBoolean({
      val errorOrResult = runFetchBlobSuspend(ctx, allRequiredParametersMap)

      errorOrResult.fmap { result ->
        val taskId = allRequiredParametersMap.getString("taskId") ?: ""
        val absoluteFilePath = result.getMap("data")?.getString("absoluteFilePath") ?: ""

        Shadows.shadowOf(ctx.contentResolver)
          .registerInputStream(Uri.parse(absoluteFilePath), "".byteInputStream())

        val uploadParametersMap =
          createValidUploadTestParameterMap(taskId, absoluteFilePath)
            .toMap()
            .plus("url" to "https://github.com/edeckers/this-does-not-exist")

        runBlocking {
          runUploadBlobSuspend(ctx, uploadParametersMap.toReactMap())
        }
      }
    })

    assertRequestTrue(message, succeeded)
  }

  @Category(EndToEnd::class, Slow::class)
  @Test
  fun unreachable_server_rejects_upload_promise() = runBlocking {
    val allRequiredParametersMap = createValidTestFetchParameterMap().toReactMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val (succeeded, message) = runRequestToBoolean({
      val errorOrResult = runFetchBlobSuspend(ctx, allRequiredParametersMap)

      errorOrResult.fmap { result ->
        val taskId = allRequiredParametersMap.getString("taskId") ?: ""
        val absoluteFilePath = result.getMap("data")?.getString("absoluteFilePath") ?: ""

        val uploadParametersMap =
          createValidUploadTestParameterMap(taskId, absoluteFilePath)

        val requestWithUnreachableUrl =
          uploadParametersMap
            .toMap()
            .plus("url" to "http://127.0.0.1:12345")

        runBlocking {
          runUploadBlobSuspend(ctx, requestWithUnreachableUrl.toReactMap())
        }
      }
    })

    assertRequestFalse(message, succeeded)
  }

  @Category(Isolated::class)
  @Test
  fun total_number_of_bytes_estimate_is_returned_by_input_stream_request_body() {
    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val fileUri = Uri.parse(SOME_FILE_THAT_IS_ALWAYS_AVAILABLE)

    val someFileContent = "THESE_ARE_SOME_BYTES"

    Shadows.shadowOf(ctx.contentResolver)
      .registerInputStream(fileUri, someFileContent.byteInputStream())

    val requestBody =
      InputStreamRequestBody("text/plain".toMediaType(), ctx.contentResolver, fileUri)

    assertEquals(
      "Returned length differs from expected length",
      someFileContent.length.toLong(),
      requestBody.contentLength()
    )
  }

  @Category(EndToEnd::class)
  @Test
  fun missing_required_upload_parameters_rejects_fetch_promise() {
    val allValuesMapping =
      createValidUploadTestParameterMap("some-task-id", "/tmp").toMap()

    val missingKeyCombinations = createAllSingleMissingKeyCombinations(allValuesMapping)

    missingKeyCombinations.forEach(::assert_missing_required_upload_parameter_rejects_promise)
  }

  @Category(EndToEnd::class, Regression::class)
  @Test // This is the faster, and less thorough version of the Instrumented test with the same name
  fun uploading_a_file_from_outside_app_data_directory_resolves_promise() = runBlocking {
    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    Shadows.shadowOf(ctx.contentResolver)
      .registerInputStream(Uri.parse(SOME_FILE_THAT_IS_ALWAYS_AVAILABLE), "".byteInputStream())

    val uploadParametersMap =
      createValidUploadTestParameterMap(
        UUID.randomUUID().toString(),
        SOME_FILE_THAT_IS_ALWAYS_AVAILABLE
      ).toReactMap()

    val (succeeded, message) = runRequestToBoolean({
      runUploadBlobSuspend(ctx, uploadParametersMap)
    })

    assertRequestTrue(message, succeeded)
  }

  @Category(EndToEnd::class, Regression::class)
  @Test
  fun correct_target_parameters_resolve_promise() {
    listOf("cache", "data").forEach { assert_correct_target_parameter_resolves_promise(it) }
  }

  @Category(EndToEnd::class, Regression::class)
  @Test
  fun incorrect_target_parameter_rejects_promise() = runBlocking {
    val allRequiredParametersMap = createValidTestFetchParameterMap()

    val android = mapOf("target" to "SOME_UNKNOWN_TARGET")

    val requestWithInvalidTargetDirectory =
      allRequiredParametersMap.plus(Pair("android", android)).toReactMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val (succeeded, message) =
      runRequestToBoolean({
        runFetchBlobSuspend(ctx, requestWithInvalidTargetDirectory)
      })

    assertRequestFalse(message, succeeded)
  }

  @Category(Isolated::class, Regression::class)
  @Test
  fun multipart_parameters_must_be_sent_to_the_server_in_the_order_they_were_provided() {
    val uploadParameterReadableMap =
      createValidUploadTestParameterMap("taskId", "/some/local/path")
        .toMap()
        .plus(
          "parts" to arrayOf(
            mapOf("name" to "bbbbb", "type" to "string", "payload" to "something0"),
            mapOf("name" to "ccccc", "type" to "string", "payload" to "something1"),
            mapOf("name" to "aaaaa", "type" to "string", "payload" to "something2")
          )
        )
        .toReactMap()

    val errorOrUploaderParameters =
      UploaderParameterFactory().fromInput(uploadParameterReadableMap)

    when (errorOrUploaderParameters) {
      is Either.Left -> assertTrue("Invalid uploader parameters", false)
      is Either.Right -> {
        val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())
        val uploaderMultipartBody =
          errorOrUploaderParameters.v.toMultipartBody(ctx.contentResolver)

        val names = mapMultipartsToNames(uploaderMultipartBody.parts)

        assertArrayEquals(
          "Sent array of upload part names differs from provided part names",
          arrayOf("bbbbb", "ccccc", "aaaaa"),
          names
        )
      }
    }
  }

  @Category(EndToEnd::class)
  @Test // This is the faster, and less thorough version of the Instrumented test with the same name
  fun non_existing_uploadable_file_rejects_promise() = runBlocking {
    val irrelevantTaskId = UUID.randomUUID().toString()
    val someNonExistentPath = "file:///this/path/does/not/exist.png"
    val allRequiredParametersMap =
      createValidUploadTestParameterMap(irrelevantTaskId, someNonExistentPath)

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val (succeeded, message) = runRequestToBoolean({
      runUploadBlobSuspend(ctx, allRequiredParametersMap.toReactMap())
    })

    assertRequestFalse(message, succeeded)
  }

  @Category(Isolated::class)
  @Test
  fun validating_non_null_values_works() {
    val someObject = Object()
    val someParameterName = "SOME_PARAMETER_NAME_0"

    val leftNull = validate(null, isNotNull(someParameterName))
    val rightObject = validate(someObject, isNotNull("SOME_PARAMETER_NAME_1"))

    assertTrue("Validation should fail", leftNull is Either.Left)
    assertTrue("Validation should succeed", rightObject is Either.Right)

    leftNull as Either.Left
    rightObject as Either.Right

    assertTrue("Failure error is of wrong type", leftNull.v is ValidationError.IsNull)
    assertEquals(someParameterName, (leftNull.v as ValidationError.IsNull).parameterName)

    assertSame("Object doesn't match the provided object", someObject, rightObject.v)
  }

  @Category(Isolated::class)
  @Test
  fun validating_not_null_or_empty_values_works() {
    val someObject = Object()
    val someParameterName = "SOME_PARAMETER_NAME_0"

    val leftEmpty = validate("", isNotNullOrEmptyString(someParameterName))
    val leftNull = validate(null, isNotNullOrEmptyString(someParameterName))
    val rightObject = validate(someObject, isNotNull("SOME_PARAMETER_NAME_1"))

    assertTrue("Validation should fail", leftNull is Either.Left)
    assertTrue("Validation should succeed", rightObject is Either.Right)

    leftEmpty as Either.Left
    leftNull as Either.Left
    rightObject as Either.Right

    assertTypeOf("Failure error is of wrong type", leftEmpty.v, ValidationError.IsEmpty::class.java)
    assertEquals(someParameterName, (leftEmpty.v as ValidationError.IsEmpty).parameterName)

    assertTypeOf("Failure error is of wrong type", leftNull.v, ValidationError.IsNull::class.java)
    assertEquals(someParameterName, (leftNull.v as ValidationError.IsNull).parameterName)

    assertSame("Object doesn't match the provided object", someObject, rightObject.v)
  }

  @Category(EndToEnd::class, Regression::class)
  @Test
  fun congestion_avoiding_progress_updater_is_instantiated_with_correct_interval_for_download() =
    runBlocking {
      val irrelevantTaskId = UUID.randomUUID().toString()
      val someTimeOutValueThatIsNotDefault = DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS * 2

      val requiredParametersAndProgressInterval = createValidTestFetchParameterMap()
        .plus(
          PARAMETER_SETTINGS_PROGRESS_INTERVAL to someTimeOutValueThatIsNotDefault
        ).toReactMap()

      mockkConstructor(CongestionAvoidingProgressNotifierFactory::class)

      val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

      every {
        constructedWith<CongestionAvoidingProgressNotifierFactory>(
          OfTypeMatcher<ReactApplicationContext>(ReactApplicationContext::class),
          EqMatcher(someTimeOutValueThatIsNotDefault),
        ).create(any())
      } returns CongestionAvoidingProgressNotifier(
        ctx,
        irrelevantTaskId,
        someTimeOutValueThatIsNotDefault
      )

      val (succeeded, message) =
        runRequestToBoolean({ runFetchBlobSuspend(ctx, requiredParametersAndProgressInterval) })

      verify {
        constructedWith<CongestionAvoidingProgressNotifierFactory>(
          OfTypeMatcher<ReactApplicationContext>(ReactApplicationContext::class),
          EqMatcher(someTimeOutValueThatIsNotDefault),
        ).create(any())
      }

      assertRequestTrue(message, succeeded)
    }

  @Category(EndToEnd::class, Regression::class)
  @Test
  fun congestion_avoiding_progress_updater_is_instantiated_with_correct_interval_for_upload() =
    runBlocking {
      val irrelevantTaskId = UUID.randomUUID().toString()
      val someTimeOutValueThatIsNotDefault = DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS * 2

      val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

      Shadows.shadowOf(ctx.contentResolver)
        .registerInputStream(Uri.parse(SOME_FILE_THAT_IS_ALWAYS_AVAILABLE), "".byteInputStream())

      val uploadParametersMap =
        createValidUploadTestParameterMap(
          UUID.randomUUID().toString(),
          SOME_FILE_THAT_IS_ALWAYS_AVAILABLE
        ).toMap()
          .plus(
            PARAMETER_SETTINGS_PROGRESS_INTERVAL to someTimeOutValueThatIsNotDefault
          ).toReactMap()

      mockkConstructor(CongestionAvoidingProgressNotifierFactory::class)

      every {
        constructedWith<CongestionAvoidingProgressNotifierFactory>(
          OfTypeMatcher<ReactApplicationContext>(ReactApplicationContext::class),
          EqMatcher(someTimeOutValueThatIsNotDefault),
        ).create(any())
      } returns CongestionAvoidingProgressNotifier(
        ctx,
        irrelevantTaskId,
        someTimeOutValueThatIsNotDefault
      )

      val (succeeded, message) = runRequestToBoolean({
        runUploadBlobSuspend(ctx, uploadParametersMap)
      })

      verify {
        constructedWith<CongestionAvoidingProgressNotifierFactory>(
          OfTypeMatcher<ReactApplicationContext>(ReactApplicationContext::class),
          EqMatcher(someTimeOutValueThatIsNotDefault),
        ).create(any())
      }

      assertRequestTrue(message, succeeded)
    }

  private fun assert_correct_target_parameter_resolves_promise(correctTarget: String) =
    runBlocking {
      val allRequiredParametersMap = createValidTestFetchParameterMap()

      val android = mapOf("target" to correctTarget)

      val requestWithInvalidTargetDirectory =
        allRequiredParametersMap.plus(Pair("android", android))

      val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

      val (succeeded, message) = runRequestToBoolean({
        runFetchBlobSuspend(ctx, requestWithInvalidTargetDirectory.toReactMap())
      })

      assertRequestTrue(message, succeeded)
    }

  private fun createErrorReportForMissingParameter(availableParameters: Map<*, *>, result: String) =
    "(missingKeys=${
      retrieveMissingKeys(
        createValidUploadTestParameterMap("", "").toMap(),
        availableParameters
      ).joinToString(";")
    }; result=$result)"

  private fun assert_missing_required_fetch_parameter_rejects_promise(
    availableParameters: Map<Any?, Any?>,
  ) = runBlocking {
    val availableParametersAsMap = availableParameters.toMap().toReactMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val (succeeded, message) = runRequest({
      runFetchBlobSuspend(ctx, availableParametersAsMap)
    }).fold(
      { Pair(false, createErrorReportForMissingParameter(availableParameters, it.message ?: "")) },
      { Pair(true, createErrorReportForMissingParameter(availableParameters, "$it")) }
    )

    assertRequestFalse(message, succeeded)
  }

  private fun assert_missing_required_upload_parameter_rejects_promise(
    availableParameters: Map<Any?, Any?>
  ) = runBlocking {
    val allFetchParametersMap = createValidTestFetchParameterMap()

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val (succeeded, message) = runRequest({
      val errorOrResult = runFetchBlobSuspend(ctx, allFetchParametersMap.toReactMap())

      errorOrResult.fmap { result ->
        val absoluteFilePath = result.getMap("data")?.getString("absoluteFilePath") ?: ""

        Shadows.shadowOf(ctx.contentResolver)
          .registerInputStream(Uri.parse(absoluteFilePath), "".byteInputStream())

        runBlocking {
          runUploadBlobSuspend(ctx, availableParameters.toReactMap())
        }
      }
    }).fold(
      { Pair(false, createErrorReportForMissingParameter(availableParameters, it.message ?: "")) },
      { Pair(true, createErrorReportForMissingParameter(availableParameters, "$it")) }
    )

    assertRequestFalse(message, succeeded)
  }
}
