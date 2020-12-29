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
import io.deckers.blob_courier.Fixtures.BooleanPromise
import io.deckers.blob_courier.Fixtures.createValidTestFetchParameterMap
import io.deckers.blob_courier.Fixtures.createValidUploadTestParameterMap
import io.deckers.blob_courier.Fixtures.runFetchBlob
import io.deckers.blob_courier.Fixtures.runUploadBlob
import io.deckers.blob_courier.common.Either
import io.deckers.blob_courier.common.ValidationError
import io.deckers.blob_courier.common.isNotNull
import io.deckers.blob_courier.common.isNotNullOrEmpty
import io.deckers.blob_courier.common.test
import io.deckers.blob_courier.react.toReactMap
import io.deckers.blob_courier.upload.InputStreamRequestBody
import io.deckers.blob_courier.upload.UploaderParameterFactory
import io.deckers.blob_courier.upload.toMultipartBody
import io.mockk.every
import io.mockk.mockkStatic
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import okhttp3.MediaType
import okhttp3.MultipartBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

const val SOME_FILE_THAT_IS_ALWAYS_AVAILABLE = "file:///system/etc/fonts.xml"

private fun mapMultipartsToNames(parts: List<MultipartBody.Part>) =
  parts.fold(
    emptyArray(),
    { names: Array<String>, part: MultipartBody.Part ->
      val contentDisposition = part.headers()?.get("Content-Disposition")

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

private fun createAllSingleMissingKeyCombinations(m: Map<*, *>): List<Map<Any?, Any?>> {
  if (m.keys.isEmpty()) return listOf()

  return m.keys.flatMap { k0 ->
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

  @Test
  fun missing_required_fetch_parameters_rejects_fetch_promise() {
    val allValuesMapping = createValidTestFetchParameterMap()

    val missingKeyCombinations = createAllSingleMissingKeyCombinations(allValuesMapping)

    missingKeyCombinations.forEach {
      assert_missing_required_fetch_parameter_rejects_promise(it, allValuesMapping)
    }
  }

  @Test(timeout = DEFAULT_PROMISE_TIMEOUT_MILLISECONDS)
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
          ctx,
          allRequiredParametersMap,
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

  @Test(timeout = DEFAULT_PROMISE_TIMEOUT_MILLISECONDS)
  fun unreachable_fetch_server_rejects_promise() {
    val allRequiredParametersMap = createValidTestFetchParameterMap()
    val requestWithNonExistentUrl =
      allRequiredParametersMap.plus(Pair("url", "http://127.0.0.1:12345")).toReactMap()

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
          requestWithNonExistentUrl,
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
      assertTrue(
        "Test execution exceeded $DEFAULT_PROMISE_TIMEOUT_MILLISECONDS milliseconds", false
      )
      return
    }

    assertTrue(result.second, result.first)
  }

  @Test(timeout = DEFAULT_PROMISE_TIMEOUT_MILLISECONDS)
  fun non_ok_http_fetch_response_resolves_promise() {
    val allRequiredParametersMap = createValidTestFetchParameterMap()
    val requestWithNonExistentUrl =
      allRequiredParametersMap.plus(
        Pair("url", "https://github.com/edeckers/this-does-not-exist")
      ).toReactMap()

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
          requestWithNonExistentUrl,
          Fixtures.EitherPromise(
            { m0 -> finishThread(false, "Rejected but expected resolve: $m0") },
            { m0 -> finishThread(true, "Success: $m0") }
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

  @Test(timeout = DEFAULT_PROMISE_TIMEOUT_MILLISECONDS)
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

              Shadows.shadowOf(ctx.contentResolver)
                .registerInputStream(Uri.parse(absoluteFilePath), "".byteInputStream())

              val uploadParametersMap =
                createValidUploadTestParameterMap(taskId, absoluteFilePath).toReactMap()

              runUploadBlob(
                ctx,
                uploadParametersMap,
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

  @Test(timeout = DEFAULT_PROMISE_TIMEOUT_MILLISECONDS)
  fun using_a_string_payload_resolves_upload_promise() {
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

              runUploadBlob(
                ctx,
                uploadParametersWithStringPayloadMap.toReactMap(),
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

  @Test(timeout = DEFAULT_PROMISE_TIMEOUT_MILLISECONDS)
  fun non_ok_http_response_resolves_upload_promise() {
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

              Shadows.shadowOf(ctx.contentResolver)
                .registerInputStream(Uri.parse(absoluteFilePath), "".byteInputStream())

              val uploadParametersMap =
                createValidUploadTestParameterMap(taskId, absoluteFilePath)
                  .toMap()
                  .plus("url" to "https://github.com/edeckers/this-does-not-exist")
                  .toReactMap()

              runUploadBlob(
                ctx,
                uploadParametersMap,
                Fixtures.EitherPromise(
                  { m1 -> finishThread(false, "Failed upload: $m1") },
                  { m1 -> finishThread(true, "Success: $m1") }
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

  @Test(timeout = DEFAULT_PROMISE_TIMEOUT_MILLISECONDS)
  fun unreachable_server_rejects_upload_promise() {
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
                createValidUploadTestParameterMap(taskId, absoluteFilePath)
              val requestWithUnreachableUrl =
                uploadParametersMap
                  .toMap()
                  .plus("url" to "http://127.0.0.1:12345")
                  .toReactMap()

              runUploadBlob(
                ctx,
                requestWithUnreachableUrl,
                Fixtures.EitherPromise(
                  { m1 -> finishThread(true, "Success: $m1") },
                  { m1 -> finishThread(false, "Resolved but expected reject: $m1") }
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
  fun total_number_of_bytes_estimate_is_returned_by_input_stream_request_body() {
    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    val fileUri = Uri.parse(SOME_FILE_THAT_IS_ALWAYS_AVAILABLE)

    val someFileContent = "THESE_ARE_SOME_BYTES"

    Shadows.shadowOf(ctx.contentResolver)
      .registerInputStream(fileUri, someFileContent.byteInputStream())

    val requestBody =
      InputStreamRequestBody(MediaType.get("text/plain"), ctx.contentResolver, fileUri)

    assertEquals(
      "Returned length differs from expected length",
      someFileContent.length.toLong(),
      requestBody.contentLength()
    )
  }

  @Test
  fun missing_required_upload_parameters_rejects_fetch_promise() {
    val allValuesMapping =
      createValidUploadTestParameterMap("some-task-id", "/tmp").toMap()

    val missingKeyCombinations = createAllSingleMissingKeyCombinations(allValuesMapping)

    missingKeyCombinations.forEach {
      assert_missing_required_upload_parameter_rejects_promise(it, allValuesMapping)
    }
  }

  @Test // This is the faster, and less thorough version of the Instrumented test with the same name
  fun uploading_a_file_from_outside_app_data_directory_resolves_promise() {

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
        Shadows.shadowOf(ctx.contentResolver)
          .registerInputStream(Uri.parse(SOME_FILE_THAT_IS_ALWAYS_AVAILABLE), "".byteInputStream())

        val uploadParametersMap =
          createValidUploadTestParameterMap(
            UUID.randomUUID().toString(),
            SOME_FILE_THAT_IS_ALWAYS_AVAILABLE
          ).toReactMap()

        runUploadBlob(
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
      assertTrue(
        "Test execution exceeded $DEFAULT_PROMISE_TIMEOUT_MILLISECONDS milliseconds", false
      )
      return
    }

    assertTrue(result.second, result.first)
  }

  @Test
  fun correct_target_parameters_resolve_promise() {
    listOf("cache", "data").forEach { assert_correct_target_parameter_resolves_promise(it) }
  }

  @Test
  fun incorrect_target_parameter_rejects_promise() {
    val allRequiredParametersMap = createValidTestFetchParameterMap()

    val android = mapOf("target" to "SOME_UNKNOWN_TARGET")

    val requestWithInvalidTargetDirectory =
      allRequiredParametersMap.plus(Pair("android", android)).toReactMap()

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
          requestWithInvalidTargetDirectory,
          Fixtures.EitherPromise(
            { m0 ->
              finishThread(true, "Success: $m0")
            },
            { m0 ->
              finishThread(false, "Resolved but expected reject: $m0")
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

        val names = mapMultipartsToNames(uploaderMultipartBody.parts())

        assertArrayEquals(
          "Sent array of upload part names differs from provided part names",
          arrayOf("bbbbb", "ccccc", "aaaaa"),
          names
        )
      }
    }
  }

  @Test // This is the faster, and less thorough version of the Instrumented test with the same name
  fun non_existing_uploadable_file_rejects_promise() {
    val irrelevantTaskId = UUID.randomUUID().toString()
    val someNonExistentPath = "file:///this/path/does/not/exist.png"
    val allRequiredParametersMap =
      createValidUploadTestParameterMap(irrelevantTaskId, someNonExistentPath)

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
        runUploadBlob(
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
      assertTrue(
        "Test execution exceeded $DEFAULT_PROMISE_TIMEOUT_MILLISECONDS milliseconds", false
      )
      return
    }

    assertTrue(result.second, result.first)
  }

  @Test
  fun validating_non_null_values_works() {
    val someObject = Object()
    val someParameterName = "SOME_PARAMETER_NAME_0"

    val leftNull = test(null, isNotNull(someParameterName))
    val rightObject = test(someObject, isNotNull("SOME_PARAMETER_NAME_1"))

    assertTrue("Validation should fail", leftNull is Either.Left)
    assertTrue("Validation should succeed", rightObject is Either.Right)

    leftNull as Either.Left
    rightObject as Either.Right

    assertTrue("Failure error is of wrong type", leftNull.v is ValidationError.IsNull)
    assertEquals(someParameterName, (leftNull.v as ValidationError.IsNull).parameterName)

    assertSame("Object doesn't match the provided object", someObject, rightObject.v)
  }

  @Test
  fun validating_not_null_or_empty_values_works() {
    val someObject = Object()
    val someParameterName = "SOME_PARAMETER_NAME_0"

    val leftEmpty = test("", isNotNullOrEmpty(someParameterName))
    val leftNull = test(null, isNotNullOrEmpty(someParameterName))
    val rightObject = test(someObject, isNotNull("SOME_PARAMETER_NAME_1"))

    assertTrue("Validation should fail", leftNull is Either.Left)
    assertTrue("Validation should succeed", rightObject is Either.Right)

    leftEmpty as Either.Left
    leftNull as Either.Left
    rightObject as Either.Right

    assertTrue("Failure error is of wrong type", leftEmpty.v is ValidationError.IsEmpty)
    assertEquals(someParameterName, (leftEmpty.v as ValidationError.IsEmpty).parameterName)

    assertTrue("Failure error is of wrong type", leftNull.v is ValidationError.IsNull)
    assertEquals(someParameterName, (leftNull.v as ValidationError.IsNull).parameterName)

    assertSame("Object doesn't match the provided object", someObject, rightObject.v)
  }

  private fun assert_correct_target_parameter_resolves_promise(correctTarget: String) {
    val allRequiredParametersMap = createValidTestFetchParameterMap()

    val android = mapOf("target" to correctTarget)

    val requestWithInvalidTargetDirectory =
      allRequiredParametersMap.plus(Pair("android", android)).toReactMap()

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
          requestWithInvalidTargetDirectory,
          Fixtures.EitherPromise(
            { m0 -> finishThread(false, "Rejected but expected resolve: $m0") },
            { m0 -> finishThread(true, "Success: $m0") }
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

  private fun assert_missing_required_fetch_parameter_rejects_promise(
    availableParameters: Map<Any?, Any?>,
    allValuesMapping: Map<String, String>
  ) {
    val availableParametersAsMap = availableParameters.toMap().toReactMap()

    val missingValues = retrieveMissingKeys(allValuesMapping, availableParameters)
    println("Missing values: ${missingValues.joinToString()}")

    val ctx = ReactApplicationContext(ApplicationProvider.getApplicationContext())

    runFetchBlob(ctx, availableParametersAsMap, BooleanPromise { v -> assertFalse(v) })
  }

  private fun assert_missing_required_upload_parameter_rejects_promise(
    availableParameters: Map<Any?, Any?>,
    allValuesMapping: Map<String, Any>
  ) {
    val allFetchParametersMap = createValidTestFetchParameterMap()

    val missingValues = retrieveMissingKeys(allValuesMapping, availableParameters)
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

              Shadows.shadowOf(ctx.contentResolver)
                .registerInputStream(Uri.parse(absoluteFilePath), "".byteInputStream())

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
}
