/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.ReactApplicationContext
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
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

    val ctx = ReactApplicationContext(RuntimeEnvironment.application)

    runFetchBlob(ctx, allRequiredParametersMap, BooleanPromise { v -> assertTrue(v) })
  }

  @Test
  fun all_required_parameters_provided_resolves_upload_promise() {
    val allRequiredParametersMap = createValidTestFetchParameterMap().toReactMap()

    val ctx = ReactApplicationContext(RuntimeEnvironment.application)

    runFetchBlob(
      ctx,
      allRequiredParametersMap,
      Fixtures.EitherPromise(
        { assertTrue(false) },
        { m ->
          val taskId = allRequiredParametersMap.getString("taskId") ?: ""
          val absoluteFilePath = m?.getMap("data")?.getString("absoluteFilePath") ?: ""

          val uploadParametersMap =
            createValidUploadTestParameterMap(taskId, absoluteFilePath).toReactMap()
          runUploadBlob(ctx, uploadParametersMap, BooleanPromise { v -> assertTrue(v) })
        }
      )
    )
  }

  @Test
  fun missing_required_upload_parameters_rejects_fetch_promise() {
    val allValuesMapping = createValidUploadTestParameterMap("some-task-id", "/tmp")
    val parameterValuePairs = allValuesMapping.entries.map { it.key to it.value }
    val oneTooFew = allValuesMapping.count() - 1

    val setsOfParametersMissingSinglePair = createSublistsFromList(parameterValuePairs, oneTooFew)

    setsOfParametersMissingSinglePair.forEach {
      assert_missing_required_upload_parameter_rejects_promise(it, allValuesMapping)
    }
  }

  private fun assert_missing_required_fetch_parameter_rejects_promise(
    availableParameters: List<Pair<String, String>>,
    allValuesMapping: Map<String, String>
  ) {
    val availableParametersAsMap = availableParameters.toMap().toReactMap()

    availableParameters.forEach { availableParametersAsMap.putString(it.first, it.second) }

    val missingValues = allValuesMapping.keys.minus(availableParameters.map { it.first })
    println("Missing values: ${missingValues.joinToString()}")

    val ctx = ReactApplicationContext(RuntimeEnvironment.application)

    runFetchBlob(ctx, availableParametersAsMap, BooleanPromise { v -> assertFalse(v) })
  }

  private fun assert_missing_required_upload_parameter_rejects_promise(
    availableParameters: List<Pair<String, String>>,
    allValuesMapping: Map<String, String>
  ) {
    val allFetchParametersMap = createValidTestFetchParameterMap()
    val availableParametersAsMap = availableParameters.toMap().toReactMap()

    availableParameters.forEach { availableParametersAsMap.putString(it.first, it.second) }

    val missingValues = allValuesMapping.keys.minus(availableParameters.map { it.first })
    println("Missing values: ${missingValues.joinToString()}")

    val ctx = ReactApplicationContext(RuntimeEnvironment.application)

    runFetchBlob(
      ctx,
      allFetchParametersMap.toReactMap(),
      Fixtures.EitherPromise(
        { assertTrue(false) },
        { m ->
          val taskId = allFetchParametersMap["taskId"] ?: ""
          val absoluteFilePath = m?.getMap("data")?.getString("absoluteFilePath") ?: ""

          val uploadParametersMap =
            createValidUploadTestParameterMap(taskId, absoluteFilePath).toReactMap()
          runUploadBlob(ctx, uploadParametersMap, BooleanPromise { v -> assertTrue(v) })
        }
      )
    )
  }
}
