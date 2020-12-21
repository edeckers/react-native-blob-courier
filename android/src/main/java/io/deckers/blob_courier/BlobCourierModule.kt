/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.modules.network.OkHttpClientProvider
import io.deckers.blob_courier.common.BlobCourierError
import io.deckers.blob_courier.common.ERROR_UNKNOWN_HOST
import io.deckers.blob_courier.common.LIBRARY_NAME
import io.deckers.blob_courier.common.processUnexpectedError
import io.deckers.blob_courier.common.processUnexpectedException
import io.deckers.blob_courier.fetch.BlobDownloader
import io.deckers.blob_courier.fetch.DownloaderParameterFactory
import io.deckers.blob_courier.upload.BlobUploader
import io.deckers.blob_courier.upload.UploaderParameterFactory
import java.net.UnknownHostException
import kotlin.concurrent.thread

private fun createHttpClient() = OkHttpClientProvider.getOkHttpClient()

class BlobCourierModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String = LIBRARY_NAME

  @ReactMethod
  fun fetchBlob(input: ReadableMap, promise: Promise) {
    thread {
      try {
        val fetchParameters =
          DownloaderParameterFactory().fromInput(input, promise)

        fetchParameters?.run {
          BlobDownloader(reactContext, createHttpClient()).startBlobFetch(fetchParameters, promise)
        }
      } catch (e: BlobCourierError) {
        promise.reject(e.code, e.message)
      } catch (e: UnknownHostException) {
        promise.reject(ERROR_UNKNOWN_HOST, e)
      } catch (e: Exception) {
        processUnexpectedException(promise, e)
      } catch (e: Error) {
        processUnexpectedError(promise, e)
      }
    }
  }

  @ReactMethod
  fun uploadBlob(input: ReadableMap, promise: Promise) {
    thread {
      try {
        val uploadParameters = UploaderParameterFactory().fromInput(input, promise)

        uploadParameters?.run {
          BlobUploader(reactContext, createHttpClient()).upload(uploadParameters, promise)
        }
      } catch (e: UnknownHostException) {
        promise.reject(ERROR_UNKNOWN_HOST, e)
      } catch (e: BlobCourierError) {
        promise.reject(e.code, e.message)
      } catch (e: Exception) {
        processUnexpectedException(promise, e)
      } catch (e: Error) {
        processUnexpectedError(promise, e)
      }
    }
  }
}
