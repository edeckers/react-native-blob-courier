import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import io.deckers.blob_courier.BlobCourierModule
import io.mockk.every
import io.mockk.mockkStatic
import kotlin.concurrent.thread
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

private const val DEFAULT_PROMISE_TIMEOUT_MILLISECONDS = 2_000L

@RunWith(RobolectricTestRunner::class)
class BlobCourierModuleTests {
  @Before
  fun mockSomeNativeOnlyMethods() {
    mockkStatic(Arguments::class)

    every { Arguments.createMap() } answers { JavaOnlyMap() }
  }

  // TODO Make this a little prettier
  private fun <T> create_sublists_from_list(theList: List<T>, length: Int): List<List<T>> {
    val restCount = length - 1
    val subLists = mutableListOf<List<T>>()

    for (i in 0 until theList.size) {
      val head = theList[i]
      val next = i + 1
      val tailList = theList.takeLast(theList.size - next)
      for (j in 0..tailList.size - restCount) {
        subLists.add(listOf(head) + tailList.subList(j, j + restCount))
      }
    }

    return subLists.toList()
  }

  private fun Map<String, String>.toReactMap(): WritableMap {
    val thisMap = this

    return Arguments.createMap().apply {
      thisMap.forEach { (k, v) -> putString(k, v) }
    }
  }

  private fun create_valid_test_parameter_map(): Map<String, String> = mapOf(
    "taskId" to "some-task-id",
    "filename" to "some-filename.png",
    "url" to "https://github.com/edeckers/react-native-blob-courier"
  )

  private fun run_fetch_blob(
    input: ReadableMap,
    promise: BooleanPromise,
    timeoutMilliseconds: Long = DEFAULT_PROMISE_TIMEOUT_MILLISECONDS
  ) {
    val ctx = ReactApplicationContext(RuntimeEnvironment.application)

    val m = BlobCourierModule(ctx)

    thread {
      m.fetchBlob(input, promise)
    }

    Thread.sleep(timeoutMilliseconds)
  }

  private fun assert_missing_required_parameter_rejects_promise(
    availableParameters: List<Pair<String, String>>,
    allValuesMapping: Map<String, String>
  ) {
    val availableParametersAsMap = availableParameters.toMap().toReactMap()

    availableParameters.forEach { availableParametersAsMap.putString(it.first, it.second) }

    val missingValues = allValuesMapping.keys.minus(availableParameters.map { it.first })
    System.out.println("Missing values: ${missingValues.joinToString()}")

    run_fetch_blob(availableParametersAsMap, BooleanPromise { v -> assertFalse(v) })
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

    run_fetch_blob(allRequiredParametersMap, BooleanPromise { v -> assertTrue(v) })
  }

  class BooleanPromise(val c: (Boolean) -> Unit) : Promise {
    override fun reject(code: String?, message: String?) = c(false)

    override fun reject(code: String?, throwable: Throwable?) = c(false)

    override fun reject(code: String?, message: String?, throwable: Throwable?) = c(false)

    override fun reject(throwable: Throwable?) = c(false)

    override fun reject(throwable: Throwable?, userInfo: WritableMap?) = c(false)

    override fun reject(code: String?, userInfo: WritableMap) = c(false)

    override fun reject(code: String?, throwable: Throwable?, userInfo: WritableMap?) = c(false)

    override fun reject(code: String?, message: String?, userInfo: WritableMap) = c(false)

    override fun reject(
      code: String?,
      message: String?,
      throwable: Throwable?,
      userInfo: WritableMap?
    ) =
      c(false)

    override fun reject(message: String?) = c(false)

    override fun resolve(value: Any?) = c(true)
  }
}
