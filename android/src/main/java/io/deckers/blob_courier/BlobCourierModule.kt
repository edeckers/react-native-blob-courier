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
import io.deckers.blob_courier.common.DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_EXCEPTION
import io.deckers.blob_courier.common.ERROR_UNKNOWN_HOST
import io.deckers.blob_courier.common.LIBRARY_NAME
import io.deckers.blob_courier.fetch.BlobDownloader
import io.deckers.blob_courier.fetch.DownloaderParameterFactory
import io.deckers.blob_courier.react.CongestionAvoidingProgressNotifierFactory
import io.deckers.blob_courier.react.processUnexpectedError
import io.deckers.blob_courier.react.processUnexpectedException
import io.deckers.blob_courier.react.toReactMap
import io.deckers.blob_courier.upload.BlobUploader
import io.deckers.blob_courier.upload.UploaderParameterFactory
import java.net.UnknownHostException
import kotlin.concurrent.thread

private fun createHttpClient() = OkHttpClientProvider.getOkHttpClient()
private fun createProgressFactory(reactContext: ReactApplicationContext) =
  CongestionAvoidingProgressNotifierFactory(
    reactContext,
    DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS
  )

class BlobCourierModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String = LIBRARY_NAME

  @ReactMethod
  fun fetchBlob(input: ReadableMap, promise: Promise) {
    thread {
      try {
        val (_, fetchParameters) =
          DownloaderParameterFactory().fromInput(input)

        val (error, response) = fetchParameters?.let {
          BlobDownloader(reactContext, createHttpClient(), createProgressFactory(reactContext))
            .download(fetchParameters)
        } ?: Pair(Error("An unexpected error occurred"), null)

        if (error != null) {
          promise.reject(error)
          return@thread
        }

        promise.resolve(response?.toReactMap())
      } catch (e: BlobCourierError) {
        promise.reject(e.code, e.message)
      } catch (e: UnknownHostException) {
        promise.reject(ERROR_UNKNOWN_HOST, e)
      } catch (e: Exception) {
        promise.reject(ERROR_UNEXPECTED_EXCEPTION, processUnexpectedException(e))
      } catch (e: Error) {
        promise.reject(ERROR_UNEXPECTED_EXCEPTION, processUnexpectedError(e))
      }
    }
  }

  @ReactMethod
  fun uploadBlob(input: ReadableMap, promise: Promise) {
    thread {
      try {
        val (_, uploadParameters) = UploaderParameterFactory().fromInput(input, promise)

        val (error, response) = uploadParameters?.run {
          BlobUploader(reactContext, createHttpClient(), createProgressFactory(reactContext))
            .upload(uploadParameters)
        } ?: Pair(Throwable("An unexpected error occurred"), null)

        if (error != null) {
          promise.reject(error)
          return@thread
        }

        promise.resolve(response?.toReactMap())
      } catch (e: BlobCourierError) {
        promise.reject(e.code, e.message)
      } catch (e: UnknownHostException) {
        promise.reject(ERROR_UNKNOWN_HOST, e)
      } catch (e: Exception) {
        promise.reject(ERROR_UNEXPECTED_EXCEPTION, processUnexpectedException(e))
      } catch (e: Error) {
        promise.reject(ERROR_UNEXPECTED_EXCEPTION, processUnexpectedError(e))
      }
    }
  }
}
