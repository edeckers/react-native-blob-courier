import com.facebook.react.bridge.*
import io.deckers.blob_courier.BlobCourierModule
import io.mockk.every
import io.mockk.mockkStatic
import kotlin.concurrent.thread
import org.junit.Assert.assertFalse
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

  private fun assert_missing_required_parameter_rejects_promise(
    availableParameters: List<Pair<String, String>>,
    allValuesMapping: Map<String, String>
  ) {
    val ctx = ReactApplicationContext(RuntimeEnvironment.application)

    val m = BlobCourierModule(ctx)

    val tehMap = Arguments.createMap()

    availableParameters.forEach { tehMap.putString(it.first, it.second) }

    val missingValues = allValuesMapping.keys.minus(availableParameters.map { it.first })

    var maybePromiseResult: Boolean? = null

    val p = BooleanPromise { v -> maybePromiseResult = v }

    thread {
      m.fetchBlob(tehMap, p)
    }

    Thread.sleep(2_000)
    val r = maybePromiseResult ?: false

    System.out.println("Missing values: ${missingValues.joinToString()}")
    assertFalse(r)
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

  @Test
  fun missing_required_fetch_parameters_reject_promise() {
    val parameterValuePairs =
      listOf(
        ("taskId" to "123"),
        ("filename" to "teh_filename.png"),
        ("url" to "https://nu.nl")
      )
    val allValuesMapping = parameterValuePairs.toMap()

    val xs = create_sublists_from_list(parameterValuePairs, 2)

    xs.forEach { assert_missing_required_parameter_rejects_promise(it, allValuesMapping) }
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
