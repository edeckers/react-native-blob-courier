/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.ReactApplicationContext
import io.deckers.blob_courier.Fixtures.BooleanPromise
import io.deckers.blob_courier.Fixtures.create_valid_test_parameter_map
import io.deckers.blob_courier.Fixtures.run_fetch_blob
import io.deckers.blob_courier.TestUtils.create_sublists_from_list
import io.deckers.blob_courier.TestUtils.toReactMap
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
  fun missing_required_fetch_parameters_rejects_promise() {
    val allValuesMapping = create_valid_test_parameter_map()
    val parameterValuePairs = allValuesMapping.entries.map { it.key to it.value }

    val setsOfParametersMissingSinglePair = create_sublists_from_list(parameterValuePairs, 2)

    setsOfParametersMissingSinglePair.forEach {
      assert_missing_required_parameter_rejects_promise(it, allValuesMapping)
    }
  }

  @Test
  fun all_required_parameters_provided_resolves_promise() {
    val allRequiredParametersMap = create_valid_test_parameter_map().toReactMap()

    val ctx = ReactApplicationContext(RuntimeEnvironment.application)

    run_fetch_blob(ctx, allRequiredParametersMap, BooleanPromise { v -> assertTrue(v) })
  }

  private fun assert_missing_required_parameter_rejects_promise(
    availableParameters: List<Pair<String, String>>,
    allValuesMapping: Map<String, String>
  ) {
    val availableParametersAsMap = availableParameters.toMap().toReactMap()

    availableParameters.forEach { availableParametersAsMap.putString(it.first, it.second) }

    val missingValues = allValuesMapping.keys.minus(availableParameters.map { it.first })
    println("Missing values: ${missingValues.joinToString()}")

    val ctx = ReactApplicationContext(RuntimeEnvironment.application)

    run_fetch_blob(ctx, availableParametersAsMap, BooleanPromise { v -> assertFalse(v) })
  }
}
