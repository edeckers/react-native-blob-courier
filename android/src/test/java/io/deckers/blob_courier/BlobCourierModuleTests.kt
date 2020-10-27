import com.facebook.react.bridge.*
import io.deckers.blob_courier.BlobCourierModule
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
class BlobCourierModuleTests {

    @Before
    fun mockSomeNativeOnlyMethods() {
        mockkStatic(Arguments::class)
        every { Arguments.createMap() } returns JavaOnlyMap()
    }

    @Test
    fun missing_required_fetch_parameters_reject_promise() {
        val ctx = ReactApplicationContext(RuntimeEnvironment.application)

        val m = BlobCourierModule(ctx)

        val tehMap = Arguments.createMap()

        tehMap.putString("taskId", "123")
        tehMap.putString("filename", "teh_filename.png")
        tehMap.putString("url", "https://nu.nl")

        var maybePromiseResult: Boolean? = null

        val p = BooleanPromise { v -> maybePromiseResult = v }

        thread {
            m.fetchBlob(tehMap, p)
        }

        Thread.sleep(2_000)
        val r = maybePromiseResult ?: false

        assertTrue(r)
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

        override fun reject(code: String?, message: String?, throwable: Throwable?, userInfo: WritableMap?) =
                c(false)

        override fun reject(message: String?) = c(false)

        override fun resolve(value: Any?) = c(true)
    }
}
