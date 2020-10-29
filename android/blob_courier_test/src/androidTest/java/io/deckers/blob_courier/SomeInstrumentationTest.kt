package io.deckers.blob_courier

import android.app.DownloadManager
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.ReactApplicationContext
import org.junit.Before
import org.junit.Test

// @RunWith(AndroidJUnit4::class)
// @androidx.test.filters.SmallTest
public class SomeInstrumentationTest {
  @Before
  fun createLogHistory() {
  }

  @Test
  fun logHistory_ParcelableWriteRead() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val reactContext = ReactApplicationContext(ctx)
    val defaultDownloadManager =
      reactContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager


  }

  @Test
  fun logHistory_ParcelableWriteRead2() {
    // val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    // val reactContext = ReactApplicationContext(ctx)
    // val defaultDownloadManager =
    //   reactContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    // // assert(true)
    // System.out.println("OK")

    assert(false) { "No" }
  }
}
